package com.example.engines

import com.example.audio.AudioUtils
import com.example.config.LimitsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Built-in synthetic audio engine for offline testing, UI verification,
 * and demonstration without requiring immediate external API keys.
 */
class OfflinePreviewEngine : VoiceEngine {

    override val id: String = "offline-preview"
    override val displayName: String = "Device Synth Preview"
    override val description: String = "High-precision on-device acoustic simulation for offline testing, chunking verification, and UI preview."

    override fun getCapabilities(): EngineCapabilities = EngineCapabilities(
        languages = LimitsConfig.SUPPORTED_LANGUAGES.map { it.first },
        supportedVoices = listOf("Preview Voice Alpha", "Preview Voice Beta", "Preview Voice Gamma"),
        supportsCloning = false,
        supportedDelivery = listOf(
            DeliveryFeature.PACE,
            DeliveryFeature.TONE
        ),
        maxCharsPerChunk = 250
    )

    override suspend fun createProfile(
        userId: String,
        displayName: String,
        sampleAudio: ByteArray?
    ): String {
        return "preview_profile_${System.currentTimeMillis()}"
    }

    override suspend fun synthesize(
        text: String,
        profile: VoiceProfile,
        language: String,
        delivery: DeliverySettings
    ): SynthesisOutput = withContext(Dispatchers.Default) {
        val capabilities = getCapabilities()
        val clamped = delivery.clampToCapabilities(capabilities)
        val chunks = AudioUtils.splitIntoSentenceChunks(text, capabilities.maxCharsPerChunk)

        // Simulate synthesis latency per chunk
        delay((chunks.size * 200L).coerceAtMost(800L))

        val baseFreq = when (profile.voiceName) {
            "Preview Voice Alpha" -> 440.0 // A4
            "Preview Voice Beta" -> 330.0  // E4
            else -> 523.25 // C5
        }

        // Adjust pitch slightly based on tone
        val toneMod = when (clamped.tone) {
            "Energetic" -> 1.15
            "Calm & Meditative" -> 0.85
            "Authoritative" -> 0.90
            else -> 1.0
        }

        val chunkAudios = mutableListOf<ByteArray>()
        for ((idx, chunk) in chunks.withIndex()) {
            val chunkFreq = (baseFreq * toneMod * (1.0 + (idx % 3) * 0.08))
            // Duration scales with text length and inversely with pace
            val chunkDuration = ((chunk.length * 0.05f) / clamped.pace).coerceIn(1.0f, 4.0f)
            val chunkAudio = AudioUtils.generateSyntheticToneAudio(chunkDuration, chunkFreq)
            chunkAudios.add(chunkAudio)
        }

        val joined = AudioUtils.joinAudioChunks(chunkAudios, sampleRate = 24000)
        val duration = (joined.size - 44).coerceAtLeast(0).toFloat() / (24000f * 2f)

        SynthesisOutput(
            audioBytes = joined,
            mimeType = "audio/wav",
            engineId = id,
            engineName = displayName,
            chunksCount = chunks.size,
            durationSeconds = duration,
            textLength = text.length
        )
    }

    override suspend fun deleteProfile(ref: String) {
        // No-op
    }

    override suspend fun healthCheck(): Boolean = true
}
