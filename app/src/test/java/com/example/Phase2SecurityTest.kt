package com.example

import com.example.audio.AudioQualityChecker
import com.example.audio.AudioUtils
import com.example.data.repository.ConsentSentenceGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase2SecurityTest {

    @Test
    fun testAudioQualityCheckerDetectsSilence() {
        val emptyPcm = ByteArray(32000) // All zero bytes = silence
        val silentWav = AudioUtils.pcmToWav(emptyPcm, sampleRate = 16000)
        val report = AudioQualityChecker.analyze(silentWav, sampleRate = 16000)

        assertTrue(report.isSilent)
        assertFalse(report.isValid)
        assertTrue(report.userGuidance.contains("quiet", ignoreCase = true))
    }

    @Test
    fun testAudioQualityCheckerValidatesCleanSignal() {
        val cleanAudio = AudioUtils.generateSyntheticToneAudio(durationSeconds = 2.5f, frequency = 440.0)
        val report = AudioQualityChecker.analyze(cleanAudio, sampleRate = 24000)

        assertFalse(report.isSilent)
        assertFalse(report.isClipping)
        assertTrue(report.isValid)
    }

    @Test
    fun testConsentSentenceGeneration() {
        val sentence = ConsentSentenceGenerator.generateSentence("Alice Developer")
        assertTrue(sentence.contains("Alice Developer"))
        assertTrue(sentence.contains("Verification code"))
        assertTrue(sentence.length > 50)
    }
}
