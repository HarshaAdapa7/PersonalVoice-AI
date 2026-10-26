package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerController
import com.example.audio.AudioRecorderController
import com.example.audio.AudioUtils
import com.example.audio.PlaybackState
import com.example.audio.RecordingState
import com.example.config.LimitsConfig
import com.example.data.PersonalVoiceDatabase
import com.example.data.model.AuditEventEntity
import com.example.data.model.ConsentRecordEntity
import com.example.data.model.DeletionJobEntity
import com.example.data.model.UserEntity
import com.example.data.model.VoiceProfileEntity
import com.example.data.repository.SessionManager
import com.example.data.repository.VoiceProfileRepository
import com.example.engines.DeliverySettings
import com.example.engines.EngineCapabilities
import com.example.engines.EngineRegistry
import com.example.engines.SynthesisOutput
import com.example.engines.VoiceEngine
import com.example.engines.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class VoiceStudioUiState(
    val currentTab: Int = 0, // 0 = Studio, 1 = Profiles, 2 = Audit/Privacy, 3 = Account
    val text: String = "Hello! Welcome to PersonalVoice AI. We bring authentic, situation-appropriate speech to life with expressive delivery controls.",
    val selectedEngineId: String = "gemini-tts",
    val selectedVoiceName: String = "Puck",
    val selectedProfileId: String? = null,
    val selectedProfileName: String? = null,
    val selectedLanguage: String = "en-US",
    val deliverySettings: DeliverySettings = DeliverySettings(pace = 1.0f, tone = "Warm & Friendly"),
    val isGenerating: Boolean = false,
    val generationProgressMessage: String? = null,
    val lastSynthesisOutput: SynthesisOutput? = null,
    val savedFilePath: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isApiKeyDialogOpen: Boolean = false,
    val isEngineInfoDialogOpen: Boolean = false,
    val isEnrollDialogOpen: Boolean = false,
    val isEnrolling: Boolean = false,
    val currentApiKey: String = ""
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class VoiceStudioViewModel(application: Application) : AndroidViewModel(application) {

    val database = PersonalVoiceDatabase.getInstance(application)
    val sessionManager = SessionManager(database)
    val profileRepository = VoiceProfileRepository(application, database, sessionManager)
    val engineRegistry = EngineRegistry()
    val audioPlayerController = AudioPlayerController(application.applicationContext)
    val recorderController = AudioRecorderController(application.applicationContext)

    val playbackState: StateFlow<PlaybackState> = audioPlayerController.playbackState
    val recordingState: StateFlow<RecordingState> = recorderController.recordingState
    val currentUser: StateFlow<UserEntity?> = sessionManager.currentUser

    // Observables filtered by active user session
    val activeProfiles: StateFlow<List<VoiceProfileEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) database.voiceProfileDao().getActiveProfilesForUser(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditEvents: StateFlow<List<AuditEventEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) database.auditDao().getAuditEventsForUser(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletionJobs: StateFlow<List<DeletionJobEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) database.deletionJobDao().getJobsForUser(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consentRecords: StateFlow<List<ConsentRecordEntity>> = currentUser
        .flatMapLatest { user ->
            if (user != null) database.consentDao().getConsentsForUser(user.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(
        VoiceStudioUiState(
            currentApiKey = engineRegistry.geminiEngine.getEffectiveApiKey()
        )
    )
    val uiState: StateFlow<VoiceStudioUiState> = _uiState.asStateFlow()

    init {
        val defaultEngine = engineRegistry.getDefaultEngine()
        val defaultVoice = defaultEngine.getCapabilities().supportedVoices.firstOrNull() ?: "Puck"
        _uiState.value = _uiState.value.copy(
            selectedEngineId = defaultEngine.id,
            selectedVoiceName = defaultVoice
        )
    }

    fun setTab(index: Int) {
        _uiState.value = _uiState.value.copy(currentTab = index)
    }

    fun getSelectedEngine(): VoiceEngine {
        return engineRegistry.getEngine(_uiState.value.selectedEngineId)
            ?: engineRegistry.geminiEngine
    }

    fun getEngineCapabilities(): EngineCapabilities {
        return getSelectedEngine().getCapabilities()
    }

    fun onTextChanged(newText: String) {
        _uiState.value = _uiState.value.copy(text = newText)
    }

    fun onLanguageSelected(lang: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = lang)
    }

    fun onVoiceSelected(voice: String) {
        _uiState.value = _uiState.value.copy(
            selectedVoiceName = voice,
            selectedProfileId = null,
            selectedProfileName = null
        )
    }

    fun onProfileSelectedForStudio(profile: VoiceProfileEntity) {
        _uiState.value = _uiState.value.copy(
            selectedProfileId = profile.id,
            selectedProfileName = profile.displayName,
            selectedVoiceName = profile.voiceName,
            selectedEngineId = profile.engineId,
            currentTab = 0 // Navigate back to studio
        )
    }

    fun onEngineSelected(engineId: String) {
        val engine = engineRegistry.getEngine(engineId) ?: return
        val capabilities = engine.getCapabilities()
        val defaultVoice = capabilities.supportedVoices.firstOrNull() ?: "Default"
        val clampedDelivery = _uiState.value.deliverySettings.clampToCapabilities(capabilities)

        _uiState.value = _uiState.value.copy(
            selectedEngineId = engineId,
            selectedVoiceName = defaultVoice,
            selectedProfileId = null,
            selectedProfileName = null,
            deliverySettings = clampedDelivery
        )
    }

    fun onPaceChanged(newPace: Float) {
        val clampedPace = newPace.coerceIn(LimitsConfig.MIN_PACE, LimitsConfig.MAX_PACE)
        _uiState.value = _uiState.value.copy(
            deliverySettings = _uiState.value.deliverySettings.copy(pace = clampedPace)
        )
    }

    fun onToneChanged(newTone: String) {
        _uiState.value = _uiState.value.copy(
            deliverySettings = _uiState.value.deliverySettings.copy(tone = newTone)
        )
    }

    fun setApiKeyDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isApiKeyDialogOpen = visible)
    }

    fun setEngineInfoDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isEngineInfoDialogOpen = visible)
    }

    fun setEnrollDialogVisible(visible: Boolean) {
        if (visible) {
            recorderController.reset()
        }
        _uiState.value = _uiState.value.copy(isEnrollDialogOpen = visible)
    }

    fun onSaveApiKey(apiKey: String) {
        engineRegistry.geminiEngine.setRuntimeApiKey(apiKey)
        _uiState.value = _uiState.value.copy(
            currentApiKey = apiKey,
            isApiKeyDialogOpen = false
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // Enrollment flow with verification statement & explicit consent
    fun enrollProfile(displayName: String, statement: String, consentChecked: Boolean) {
        val audioBytes = recordingState.value.recordedAudioBytes
        if (audioBytes == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please record a voice sample before enrolling.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEnrolling = true)
            val result = profileRepository.createProfile(
                displayName = displayName,
                engineId = _uiState.value.selectedEngineId,
                voiceName = _uiState.value.selectedVoiceName,
                sampleAudio = audioBytes,
                consentStatement = statement,
                explicitConsentChecked = consentChecked
            )

            result.fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(
                        isEnrolling = false,
                        isEnrollDialogOpen = false,
                        selectedProfileId = profile.id,
                        selectedProfileName = profile.displayName,
                        successMessage = "Voice profile '${profile.displayName}' authorized and enrolled!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isEnrolling = false,
                        errorMessage = error.message ?: "Failed to enroll voice profile."
                    )
                }
            )
        }
    }

    // Delete flow: revokes access immediately and enqueues deletion job
    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            val result = profileRepository.deleteProfile(profileId)
            result.fold(
                onSuccess = {
                    if (_uiState.value.selectedProfileId == profileId) {
                        _uiState.value = _uiState.value.copy(
                            selectedProfileId = null,
                            selectedProfileName = null
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Profile access revoked and permanent deletion job completed."
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = error.message ?: "Failed to delete profile."
                    )
                }
            )
        }
    }

    // Auth methods
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val res = sessionManager.login(email, pass)
            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Logged in as ${it.displayName}",
                        errorMessage = null
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(errorMessage = it.message)
                }
            )
        }
    }

    fun register(email: String, pass: String, name: String) {
        viewModelScope.launch {
            val res = sessionManager.register(email, pass, name)
            res.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Account created for ${it.displayName}",
                        errorMessage = null
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(errorMessage = it.message)
                }
            )
        }
    }

    fun logout() {
        sessionManager.logout()
        _uiState.value = _uiState.value.copy(
            selectedProfileId = null,
            selectedProfileName = null,
            successMessage = "Logged out of session."
        )
    }

    fun switchTestUser() {
        viewModelScope.launch {
            val testBEmail = "colleague@personalvoice.ai"
            var userB = database.userDao().getUserByEmail(testBEmail)
            if (userB == null) {
                userB = sessionManager.register(testBEmail, "pass123", "Colleague B").getOrNull()
            }
            if (userB != null) {
                sessionManager.switchUser(userB)
                _uiState.value = _uiState.value.copy(
                    selectedProfileId = null,
                    selectedProfileName = null,
                    successMessage = "Switched to account: ${userB.displayName} (Demonstrating cross-user data isolation)"
                )
            }
        }
    }

    fun generateSpeech() {
        val text = _uiState.value.text.trim()
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter some text to synthesize.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                generationProgressMessage = "Preparing sentence-aware chunking..."
            )

            try {
                val engine = getSelectedEngine()
                val capabilities = engine.getCapabilities()
                val chunks = AudioUtils.splitIntoSentenceChunks(text, capabilities.maxCharsPerChunk)

                _uiState.value = _uiState.value.copy(
                    generationProgressMessage = "Synthesizing ${chunks.size} chunk(s) via ${engine.displayName}..."
                )

                val userId = sessionManager.requireUserId()
                val profile = VoiceProfile(
                    id = _uiState.value.selectedProfileId ?: "profile_studio_${System.currentTimeMillis()}",
                    userId = userId,
                    displayName = _uiState.value.selectedProfileName ?: _uiState.value.selectedVoiceName,
                    engineId = engine.id,
                    engineModelVersion = "v1",
                    voiceName = _uiState.value.selectedVoiceName
                )

                // Ownership check on active profile if one is selected
                _uiState.value.selectedProfileId?.let { profId ->
                    val ownedProfile = profileRepository.getProfile(profId)
                    if (ownedProfile == null) {
                        throw SecurityException("Access denied: profile $profId does not belong to the current user.")
                    }
                }

                val output = engine.synthesize(
                    text = text,
                    profile = profile,
                    language = _uiState.value.selectedLanguage,
                    delivery = _uiState.value.deliverySettings
                )

                // Record audit event for synthesis
                database.auditDao().insertEvent(
                    AuditEventEntity(
                        userId = userId,
                        action = "SPEECH_GENERATED",
                        resourceType = "VOICE_SYNTHESIS",
                        resourceId = profile.id,
                        details = "Synthesized ${chunks.size} chunk(s) (${text.length} chars) via ${engine.displayName}"
                    )
                )

                audioPlayerController.loadAudio(output.audioBytes)

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generationProgressMessage = null,
                    lastSynthesisOutput = output
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generationProgressMessage = null,
                    errorMessage = e.message ?: "Voice synthesis failed."
                )
            }
        }
    }

    fun togglePlayPause() {
        audioPlayerController.togglePlayPause()
    }

    fun seekAudio(progress: Float) {
        val duration = playbackState.value.durationMs
        val targetMs = (progress * duration).toInt()
        audioPlayerController.seekTo(targetMs)
    }

    fun exportAudio(context: Context) {
        val output = _uiState.value.lastSynthesisOutput ?: return
        try {
            val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val exportFile = File(musicDir, "PersonalVoice_${System.currentTimeMillis()}.wav")
            FileOutputStream(exportFile).use { it.write(output.audioBytes) }

            _uiState.value = _uiState.value.copy(savedFilePath = exportFile.absolutePath)
            Toast.makeText(context, "Audio saved: ${exportFile.name}", Toast.LENGTH_LONG).show()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share or Save Voice Audio").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Saved in app directory: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerController.release()
        recorderController.reset()
    }
}
