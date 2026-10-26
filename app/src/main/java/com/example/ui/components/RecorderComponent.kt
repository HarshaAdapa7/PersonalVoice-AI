package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioRecorderController
import com.example.audio.RecordingState
import java.util.Locale

@Composable
fun RecorderComponent(
    recorderController: AudioRecorderController,
    recordingState: RecordingState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recorder_component"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual amplitude sound bars
        LiveAmplitudeVisualizer(
            isRecording = recordingState.isRecording,
            amplitude = recordingState.currentAmplitude
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Timer & Status
        Text(
            text = if (recordingState.isRecording) {
                String.format(Locale.US, "Recording: %.1f s", recordingState.elapsedSeconds)
            } else if (recordingState.recordedAudioBytes != null) {
                String.format(Locale.US, "Sample Captured: %.1f s", recordingState.elapsedSeconds)
            } else {
                "Ready to record consent sample"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (recordingState.isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Big Mic Action Button (min 56dp touch target)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (recordingState.isRecording) {
                Button(
                    onClick = { recorderController.stopRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("stop_recording_button")
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Recording", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { recorderController.startRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("start_recording_button")
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Start Recording")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (recordingState.recordedAudioBytes != null) "Re-record Sample" else "Record Voice Sample",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!recordingState.isRecording && recordingState.recordedAudioBytes != null) {
                OutlinedButton(
                    onClick = { recorderController.reset() },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.testTag("reset_recording_button")
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Real-time Audio Quality Guidance Banner
        recordingState.qualityReport?.let { report ->
            val bannerBg = if (report.isValid) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            val bannerFg = if (report.isValid) Color(0xFF059669) else Color(0xFFDC2626)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_quality_banner"),
                shape = RoundedCornerShape(12.dp),
                color = bannerBg
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (report.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Quality Status",
                        tint = bannerFg,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (report.isValid) "Audio Quality Validated" else "Quality Check Notice",
                            fontWeight = FontWeight.Bold,
                            color = bannerFg,
                            fontSize = 13.sp
                        )
                        Text(
                            text = report.userGuidance,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "RMS: %.1f dBFS • Peak: %d", report.rmsLevelDb, report.peakAmplitude),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveAmplitudeVisualizer(
    isRecording: Boolean,
    amplitude: Float
) {
    val barCount = 18
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val scaleFactor = (Math.sin((i.toDouble() / barCount) * Math.PI) * 0.8 + 0.2).toFloat()
            val height = if (isRecording) {
                (amplitude * 44f * scaleFactor).coerceIn(6f, 44f).dp
            } else {
                6.dp
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isRecording) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
