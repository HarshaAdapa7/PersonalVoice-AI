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
import com.example.audio.AudioUtils
import com.example.audio.PlaybackState
import com.example.config.LimitsConfig
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class VoiceStudioUiState(
    val text: String = "Hello! Welcome to PersonalVoice AI. We bring authentic, situation-appropriate speech to life with expressive delivery controls.",
    val selectedEngineId: String = "gemini-tts",
    val selectedVoiceName: String = "Puck",
    val selectedLanguage: String = "en-US",
    val deliverySettings: DeliverySettings = DeliverySettings(pace = 1.0f, tone = "Warm & Friendly"),
    val isGenerating: Boolean = false,
    val generationProgressMessage: String? = null,
    val lastSynthesisOutput: SynthesisOutput? = null,
    val savedFilePath: String? = null,
    val errorMessage: String? = null,
    val isApiKeyDialogOpen: Boolean = false,
    val isEngineInfoDialogOpen: Boolean = false,
    val currentApiKey: String = ""
)

class VoiceStudioViewModel(application: Application) : AndroidViewModel(application) {

    val engineRegistry = EngineRegistry()
    val audioPlayerController = AudioPlayerController(application.applicationContext)

    val playbackState: StateFlow<PlaybackState> = audioPlayerController.playbackState

    private val _uiState = MutableStateFlow(
        VoiceStudioUiState(
            currentApiKey = engineRegistry.geminiEngine.getEffectiveApiKey()
        )
    )
    val uiState: StateFlow<VoiceStudioUiState> = _uiState.asStateFlow()

    init {
        // Pre-configure initial voice based on default engine
        val defaultEngine = engineRegistry.getDefaultEngine()
        val defaultVoice = defaultEngine.getCapabilities().supportedVoices.firstOrNull() ?: "Puck"
        _uiState.value = _uiState.value.copy(
            selectedEngineId = defaultEngine.id,
            selectedVoiceName = defaultVoice
        )
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
        _uiState.value = _uiState.value.copy(selectedVoiceName = voice)
    }

    fun onEngineSelected(engineId: String) {
        val engine = engineRegistry.getEngine(engineId) ?: return
        val capabilities = engine.getCapabilities()
        val defaultVoice = capabilities.supportedVoices.firstOrNull() ?: "Default"
        val clampedDelivery = _uiState.value.deliverySettings.clampToCapabilities(capabilities)

        _uiState.value = _uiState.value.copy(
            selectedEngineId = engineId,
            selectedVoiceName = defaultVoice,
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

                val profile = VoiceProfile(
                    id = "profile_studio_${System.currentTimeMillis()}",
                    userId = "user_default",
                    displayName = _uiState.value.selectedVoiceName,
                    engineId = engine.id,
                    engineModelVersion = "v1",
                    voiceName = _uiState.value.selectedVoiceName
                )

                val output = engine.synthesize(
                    text = text,
                    profile = profile,
                    language = _uiState.value.selectedLanguage,
                    delivery = _uiState.value.deliverySettings
                )

                // Load output into AudioPlayerController
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
                    errorMessage = e.message ?: "Voice synthesis failed. Please check your network or API key."
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

            // Also launch share sheet
            val uri = androidx.core.content.FileProvider.getUriForFile(
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
            // Fallback: toast simple save
            Toast.makeText(context, "Saved in app directory: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerController.release()
    }
}
