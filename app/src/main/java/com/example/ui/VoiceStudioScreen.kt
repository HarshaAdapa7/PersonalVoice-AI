package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioUtils
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.ConsentFlowDialog
import com.example.ui.components.DeliveryControlsCard
import com.example.ui.components.VoiceSelectorCard
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuditPrivacyScreen
import com.example.ui.screens.ProfilesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    viewModel: VoiceStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeProfiles by viewModel.activeProfiles.collectAsState()
    val auditEvents by viewModel.auditEvents.collectAsState()
    val deletionJobs by viewModel.deletionJobs.collectAsState()
    val consentRecords by viewModel.consentRecords.collectAsState()

    val context = LocalContext.current
    val capabilities = viewModel.getEngineCapabilities()

    val textLength = uiState.text.length
    val chunksCount = AudioUtils.splitIntoSentenceChunks(uiState.text, capabilities.maxCharsPerChunk).size

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PersonalVoice AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = when (uiState.currentTab) {
                                0 -> "Voice Studio • ${uiState.selectedProfileName ?: "Prebuilt Persona"}"
                                1 -> "Voice Profiles • Enrollment & Lifecycle"
                                2 -> "Audit Log & Privacy Queue"
                                else -> "Account Session • ${currentUser?.displayName ?: "Guest"}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setApiKeyDialogVisible(true) },
                        modifier = Modifier.testTag("open_api_key_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key Configuration",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setEngineInfoDialogVisible(true) },
                        modifier = Modifier.testTag("open_info_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Architecture Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Studio") },
                    label = { Text("Studio", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_studio")
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Profiles") },
                    label = { Text("Profiles", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_profiles")
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Privacy") },
                    label = { Text("Audit", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_audit")
                )
                NavigationBarItem(
                    selected = uiState.currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Account") },
                    label = { Text("Account", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_account")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Success banner
                uiState.successMessage?.let { msg ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissSuccess() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Error banner
                uiState.errorMessage?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Tab Content
                when (uiState.currentTab) {
                    0 -> StudioTabContent(
                        viewModel = viewModel,
                        uiState = uiState,
                        playbackState = playbackState,
                        textLength = textLength,
                        chunksCount = chunksCount,
                        capabilities = capabilities
                    )
                    1 -> ProfilesScreen(
                        profiles = activeProfiles,
                        selectedProfileId = uiState.selectedProfileId,
                        onSelectProfileForStudio = { viewModel.onProfileSelectedForStudio(it) },
                        onOpenEnrollDialog = { viewModel.setEnrollDialogVisible(true) },
                        onDeleteProfile = { viewModel.deleteProfile(it) }
                    )
                    2 -> AuditPrivacyScreen(
                        auditEvents = auditEvents,
                        deletionJobs = deletionJobs,
                        consentRecords = consentRecords
                    )
                    3 -> AccountScreen(
                        currentUser = currentUser,
                        onLogin = { email, pass -> viewModel.login(email, pass) },
                        onRegister = { email, pass, name -> viewModel.register(email, pass, name) },
                        onLogout = { viewModel.logout() },
                        onSwitchTestUser = { viewModel.switchTestUser() }
                    )
                }
            }
        }
    }

    // Enrollment Flow Dialog with Verification Statement and Audio Quality Gate
    if (uiState.isEnrollDialogOpen) {
        ConsentFlowDialog(
            userDisplayName = currentUser?.displayName ?: "User",
            recorderController = viewModel.recorderController,
            recordingState = recordingState,
            isSubmitting = uiState.isEnrolling,
            onEnroll = { name, statement, consentChecked ->
                viewModel.enrollProfile(name, statement, consentChecked)
            },
            onDismiss = { viewModel.setEnrollDialogVisible(false) }
        )
    }

    // Dialogs
    if (uiState.isApiKeyDialogOpen) {
        ApiKeyDialog(
            currentKey = uiState.currentApiKey,
            onSave = { viewModel.onSaveApiKey(it) },
            onDismiss = { viewModel.setApiKeyDialogVisible(false) }
        )
    }

    if (uiState.isEngineInfoDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.setEngineInfoDialogVisible(false) },
            title = { Text("PersonalVoice Architecture") },
            text = {
                Column {
                    Text(
                        text = "Phase 2 Security & Consent Architecture",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Ownership Verification: Profile IDs are never authorization. Access is strictly gated by authenticated session.\n\n" +
                                "• Randomized Consent: Account holders read an explicit, randomized verification sentence aloud before profile creation.\n\n" +
                                "• Quality Gate: RMS silence detection, clipping detection, and SNR verification.\n\n" +
                                "• Immediate Revocation & Deletion Queue: Deleting a profile revokes synthesis access instantly and purges local audio artifacts asynchronously.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setEngineInfoDialogVisible(false) }) {
                    Text("Understood")
                }
            }
        )
    }
}

@Composable
private fun StudioTabContent(
    viewModel: VoiceStudioViewModel,
    uiState: VoiceStudioUiState,
    playbackState: com.example.audio.PlaybackState,
    textLength: Int,
    chunksCount: Int,
    capabilities: com.example.engines.EngineCapabilities
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Active Profile Indicator (if selected from Profiles)
        uiState.selectedProfileName?.let { profName ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Active Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active Authorized Profile: $profName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Enrolled with verified consent • Engine: ${uiState.selectedEngineId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { viewModel.setTab(1) }) {
                        Text("Change", fontSize = 11.sp)
                    }
                }
            }
        }

        // Voice Script Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("script_input_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Text Input",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Speech Script",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "$textLength chars • ~$chunksCount chunk(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.text,
                    onValueChange = { viewModel.onTextChanged(it) },
                    placeholder = { Text("Enter script or text to synthesize...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .testTag("speech_text_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            viewModel.onTextChanged(
                                "Hello! I am speaking using my authorized personal voice identity, protected with verifiable consent records."
                            )
                        },
                        label = { Text("Consent Demo", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            viewModel.onTextChanged(
                                "PersonalVoice AI ensures strict data isolation. Profiles and audio samples are owned solely by verified account holders."
                            )
                        },
                        label = { Text("Privacy Demo", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Voice & Engine Selector Card
        VoiceSelectorCard(
            engineRegistry = viewModel.engineRegistry,
            selectedEngineId = uiState.selectedEngineId,
            selectedVoiceName = uiState.selectedVoiceName,
            selectedLanguage = uiState.selectedLanguage,
            onEngineSelected = { viewModel.onEngineSelected(it) },
            onVoiceSelected = { viewModel.onVoiceSelected(it) },
            onLanguageSelected = { viewModel.onLanguageSelected(it) }
        )

        // Delivery & Style Controls Card
        DeliveryControlsCard(
            deliverySettings = uiState.deliverySettings,
            capabilities = capabilities,
            onPaceChanged = { viewModel.onPaceChanged(it) },
            onToneChanged = { viewModel.onToneChanged(it) }
        )

        // Synthesize Button
        Button(
            onClick = { viewModel.generateSpeech() },
            enabled = !uiState.isGenerating && uiState.text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("generate_speech_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = uiState.generationProgressMessage ?: "Synthesizing audio...",
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Generate",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Synthesize Speech (${chunksCount} chunk${if (chunksCount > 1) "s" else ""})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Audio Player Card
        uiState.lastSynthesisOutput?.let { output ->
            AudioPlayerCard(
                output = output,
                playbackState = playbackState,
                onPlayPauseToggle = { viewModel.togglePlayPause() },
                onSeek = { viewModel.seekAudio(it) },
                onExportDownload = { viewModel.exportAudio(context) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
