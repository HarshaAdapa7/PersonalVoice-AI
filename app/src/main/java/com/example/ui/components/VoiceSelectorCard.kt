package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.LimitsConfig
import com.example.engines.EngineRegistry
import com.example.engines.VoiceEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSelectorCard(
    engineRegistry: EngineRegistry,
    selectedEngineId: String,
    selectedVoiceName: String,
    selectedLanguage: String,
    onEngineSelected: (String) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val engines = engineRegistry.getAllEngines()
    val activeEngine = engineRegistry.getEngine(selectedEngineId) ?: engineRegistry.geminiEngine
    val capabilities = activeEngine.getCapabilities()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_selector_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Engine Selection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Engine Selector",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Speech Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Engine Selection Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                engines.forEach { engine ->
                    val isSelected = engine.id == selectedEngineId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEngineSelected(engine.id) },
                        label = {
                            Text(
                                text = when (engine.id) {
                                    "gemini-tts" -> "Gemini TTS"
                                    "external-clone" -> "External Cloning"
                                    else -> "Device Preview"
                                },
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("engine_chip_${engine.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Engine Honesty Badge
            EngineBadge(engine = activeEngine)

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Profiles / Prebuilt Voices
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Voice Selection",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (capabilities.supportsCloning) "Voice Target" else "Voice Persona",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                capabilities.supportedVoices.forEach { voice ->
                    val isSelected = voice == selectedVoiceName
                    FilterChip(
                        selected = isSelected,
                        onClick = { onVoiceSelected(voice) },
                        label = { Text(voice, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("voice_chip_$voice")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selector
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Language",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Spoken Language",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LimitsConfig.SUPPORTED_LANGUAGES.forEach { (code, label) ->
                    val isSelected = code == selectedLanguage
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(code) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("lang_chip_$code")
                    )
                }
            }
        }
    }
}
