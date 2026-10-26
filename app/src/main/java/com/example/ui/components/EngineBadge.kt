package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engines.VoiceEngine

@Composable
fun EngineBadge(
    engine: VoiceEngine,
    modifier: Modifier = Modifier
) {
    val capabilities = engine.getCapabilities()
    val isCloning = capabilities.supportsCloning

    val badgeColor = when {
        engine.id == "gemini-tts" -> Color(0xFF6366F1)
        isCloning -> Color(0xFF10B981)
        else -> Color(0xFFF59E0B)
    }

    val statusText = when {
        engine.id == "gemini-tts" -> "Gemini Prebuilt • Style Control (No Cloning)"
        isCloning -> "External Voice Cloning (F5-TTS/CosyVoice)"
        else -> "Device Acoustic Synth (Preview Mode)"
    }

    Box(
        modifier = modifier
            .testTag("engine_badge")
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isCloning) Icons.Default.Shield else Icons.Default.GraphicEq,
                contentDescription = "Engine Type",
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
