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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioUtils
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.AudioPlayerCard
import com.example.ui.components.DeliveryControlsCard
import com.example.ui.components.VoiceSelectorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceStudioScreen(
    viewModel: VoiceStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
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
                            text = "Phase 1: Voice Generation MVP",
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Error Banner
                uiState.errorMessage?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.dismissError() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Voice Script Input Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("script_input_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
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
                                .height(130.dp)
                                .testTag("speech_text_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick prompt presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    viewModel.onTextChanged(
                                        "Hello team, this is PersonalVoice AI. I am excited to share our progress on the speech orchestration pipeline today."
                                    )
                                },
                                label = { Text("Intro", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    viewModel.onTextChanged(
                                        "PersonalVoice AI delivers context-grounded speech synthesis. It joins long scripts into seamless audio while evaluating naturalness."
                                    )
                                },
                                label = { Text("Product", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    viewModel.onTextChanged(
                                        "Welcome to the demonstration! Take a deep breath, relax, and let us embark on this audio journey together."
                                    )
                                },
                                label = { Text("Calm", fontSize = 11.sp) }
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

                // Generate Button with Progress
                Button(
                    onClick = { viewModel.generateSpeech() },
                    enabled = !uiState.isGenerating && uiState.text.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("generate_speech_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
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

                // Audio Output & Player Card
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
                        text = "Core Principle: Voice Cloning Honesty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Gemini TTS Engine uses prebuilt Google voices (Puck, Charon, Kore, Fenrir) with natural-language style & pace conditioning. It does not clone custom voices.\n\n" +
                                "2. External Cloning Engine is a documented adapter stub for dedicated F5-TTS or CosyVoice 2 self-hosted inference servers (disabled by default).\n\n" +
                                "3. Sentence-Aware Chunking splits long scripts into sentences under maxCharsPerChunk and seamlessly concatenates them into a unified WAV file.",
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
