package com.example

import com.example.audio.AudioUtils
import com.example.engines.DeliveryFeature
import com.example.engines.DeliverySettings
import com.example.engines.EngineCapabilities
import com.example.engines.EngineRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceEngineTest {

    @Test
    fun testSentenceAwareChunking() {
        val text = "First sentence here. Second sentence follows. Third sentence wraps up."
        val chunks = AudioUtils.splitIntoSentenceChunks(text, maxCharsPerChunk = 30)

        assertTrue(chunks.size >= 2)
        for (chunk in chunks) {
            assertTrue(chunk.length <= 40)
        }
    }

    @Test
    fun testWavHeaderCreationAndConcatenation() {
        val toneAudio1 = AudioUtils.generateSyntheticToneAudio(1.0f, 440.0)
        assertTrue(AudioUtils.isWavHeader(toneAudio1))

        val toneAudio2 = AudioUtils.generateSyntheticToneAudio(1.0f, 550.0)
        val joined = AudioUtils.joinAudioChunks(listOf(toneAudio1, toneAudio2))

        assertTrue(AudioUtils.isWavHeader(joined))
        assertTrue(joined.size > toneAudio1.size)
    }

    @Test
    fun testDeliverySettingsClamping() {
        val geminiCaps = EngineCapabilities(
            languages = listOf("en-US"),
            supportedVoices = listOf("Puck"),
            supportsCloning = false,
            supportedDelivery = listOf(DeliveryFeature.PACE, DeliveryFeature.TONE),
            maxCharsPerChunk = 400
        )

        val settings = DeliverySettings(pace = 3.5f, tone = "Energetic", pauses = 2.0f)
        val clamped = settings.clampToCapabilities(geminiCaps)

        assertEquals(2.0f, clamped.pace, 0.01f) // Clamped to 2.0 max
        assertEquals("Energetic", clamped.tone)
        assertEquals(1.0f, clamped.pauses, 0.01f) // Pauses not supported by Gemini -> resets to 1.0f
    }

    @Test
    fun testEngineRegistryHonesty() {
        val registry = EngineRegistry()

        val gemini = registry.geminiEngine
        assertFalse(gemini.getCapabilities().supportsCloning)
        assertEquals("gemini-tts", gemini.id)

        val external = registry.externalEngine
        assertTrue(external.getCapabilities().supportsCloning)
        assertEquals("external-clone", external.id)

        assertNotNull(registry.offlineEngine)
    }
}
