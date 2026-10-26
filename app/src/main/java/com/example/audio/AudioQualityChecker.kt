package com.example.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioQualityReport(
    val isValid: Boolean,
    val durationSeconds: Float,
    val rmsLevelDb: Float,
    val peakAmplitude: Short,
    val isSilent: Boolean,
    val isClipping: Boolean,
    val isNoisy: Boolean,
    val userGuidance: String
)

object AudioQualityChecker {

    /**
     * Inspects 16-bit PCM (or WAV) byte array to assess silence, clipping, and background noise.
     */
    fun analyze(audioBytes: ByteArray, sampleRate: Int = 16000): AudioQualityReport {
        if (audioBytes.isEmpty()) {
            return AudioQualityReport(
                isValid = false,
                durationSeconds = 0f,
                rmsLevelDb = -100f,
                peakAmplitude = 0,
                isSilent = true,
                isClipping = false,
                isNoisy = false,
                userGuidance = "No audio data recorded. Please tap Record and speak clearly."
            )
        }

        // If WAV, skip 44-byte header
        val pcmOffset = if (AudioUtils.isWavHeader(audioBytes)) 44 else 0
        val pcmLength = (audioBytes.size - pcmOffset).coerceAtLeast(0)
        val numSamples = pcmLength / 2

        if (numSamples < sampleRate) {
            return AudioQualityReport(
                isValid = false,
                durationSeconds = numSamples.toFloat() / sampleRate,
                rmsLevelDb = -100f,
                peakAmplitude = 0,
                isSilent = true,
                isClipping = false,
                isNoisy = false,
                userGuidance = "Audio sample is too short. Please speak the full consent sentence (at least 2 seconds)."
            )
        }

        val buffer = ByteBuffer.wrap(audioBytes, pcmOffset, pcmLength).order(ByteOrder.LITTLE_ENDIAN)

        var sumSquare = 0.0
        var maxPeak: Short = 0
        var clipCount = 0

        for (i in 0 until numSamples) {
            val sample = buffer.short
            val absSample = if (sample < 0) (-sample).toShort() else sample
            if (absSample > maxPeak) maxPeak = absSample

            // Clipping threshold for 16-bit signed (-32768 to 32767)
            if (absSample >= 32000) {
                clipCount++
            }

            sumSquare += (sample.toDouble() * sample.toDouble())
        }

        val rms = sqrt(sumSquare / numSamples)
        val normalizedRms = (rms / 32768.0).coerceAtLeast(0.00001)
        val rmsDb = (20.0 * log10(normalizedRms)).toFloat()

        val duration = numSamples.toFloat() / sampleRate
        val isSilent = rmsDb < -38.0f // Very quiet speech or dead silence
        val isClipping = clipCount > (numSamples * 0.005) // More than 0.5% samples clipped
        val isNoisy = (rmsDb > -20.0f && maxPeak < 12000) // High floor without speech spikes

        val guidance = when {
            isClipping -> "Clipping detected. Your voice is distorting — please speak a bit further from the microphone."
            isSilent -> "Audio is too quiet. Please speak up or move closer to the microphone."
            isNoisy -> "High ambient noise detected. Please move to a quieter room."
            else -> "Audio quality is excellent! Signal is clean and clear."
        }

        val isValid = !isSilent && !isClipping && duration >= 2.0f

        return AudioQualityReport(
            isValid = isValid,
            durationSeconds = duration,
            rmsLevelDb = rmsDb,
            peakAmplitude = maxPeak,
            isSilent = isSilent,
            isClipping = isClipping,
            isNoisy = isNoisy,
            userGuidance = guidance
        )
    }
}
