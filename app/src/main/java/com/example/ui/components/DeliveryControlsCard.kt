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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.LimitsConfig
import com.example.engines.DeliveryFeature
import com.example.engines.DeliverySettings
import com.example.engines.EngineCapabilities
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeliveryControlsCard(
    deliverySettings: DeliverySettings,
    capabilities: EngineCapabilities,
    onPaceChanged: (Float) -> Unit,
    onToneChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportsPace = capabilities.supportedDelivery.contains(DeliveryFeature.PACE)
    val supportsTone = capabilities.supportedDelivery.contains(DeliveryFeature.TONE)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("delivery_controls_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Delivery Controls",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delivery & Style Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pace Slider (if supported)
            if (supportsPace) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speaking Pace",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%.2fx", deliverySettings.pace),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (deliverySettings.pace != 1.0f) {
                            TextButton(
                                onClick = { onPaceChanged(1.0f) },
                                modifier = Modifier.testTag("reset_pace_button")
                            ) {
                                Text("Reset", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Slider(
                    value = deliverySettings.pace,
                    onValueChange = { onPaceChanged(it) },
                    valueRange = LimitsConfig.MIN_PACE..LimitsConfig.MAX_PACE,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pace_slider")
                )

                // Quick pace shortcuts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(0.8f, 1.0f, 1.25f, 1.5f).forEach { preset ->
                        TextButton(
                            onClick = { onPaceChanged(preset) },
                            modifier = Modifier.testTag("pace_preset_${preset}x")
                        ) {
                            Text("${preset}x", fontSize = 11.sp)
                        }
                    }
                }
            } else {
                Text(
                    text = "Pace control is not supported by the selected engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tone Selector (if supported)
            if (supportsTone) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "Tone",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Vocal Tone & Emotion",
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
                    LimitsConfig.SUPPORTED_TONES.forEach { tone ->
                        val isSelected = tone == deliverySettings.tone
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToneChanged(tone) },
                            label = { Text(tone, fontSize = 12.sp) },
                            modifier = Modifier.testTag("tone_chip_$tone")
                        )
                    }
                }
            } else {
                Text(
                    text = "Tone control is not supported by the selected engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
