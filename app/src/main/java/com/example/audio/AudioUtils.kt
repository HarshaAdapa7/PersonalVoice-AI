package com.example.audio

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioUtils {

    /**
     * Splits long text into sentence-aware chunks respecting maxCharsPerChunk.
     */
    fun splitIntoSentenceChunks(text: String, maxCharsPerChunk: Int): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxCharsPerChunk) return listOf(trimmed)

        val chunks = mutableListOf<String>()
        // Match sentence terminators (. ! ? ; or newline)
        val regex = Regex("(?<=[.!?;\n])\\s+")
        val sentences = trimmed.split(regex).map { it.trim() }.filter { it.isNotEmpty() }

        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > maxCharsPerChunk) {
                // If a single sentence exceeds the limit, break by comma or words
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }
                val subChunks = splitLongSentence(sentence, maxCharsPerChunk)
                chunks.addAll(subChunks)
            } else {
                if (currentChunk.length + sentence.length + 1 <= maxCharsPerChunk) {
                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                    currentChunk.append(sentence)
                } else {
                    if (currentChunk.isNotEmpty()) {
                        chunks.add(currentChunk.toString().trim())
                    }
                    currentChunk = StringBuilder(sentence)
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return if (chunks.isEmpty()) listOf(trimmed) else chunks
    }

    private fun splitLongSentence(sentence: String, maxChars: Int): List<String> {
        val words = sentence.split(Regex("\\s+"))
        val result = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            if (current.length + word.length + 1 <= maxChars) {
                if (current.isNotEmpty()) current.append(" ")
                current.append(word)
            } else {
                if (current.isNotEmpty()) {
                    result.add(current.toString().trim())
                }
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result
    }

    /**
     * Inspects audio bytes. If they start with "RIFF" (WAV), returns as is.
     * If raw PCM, prepends a standard 44-byte 24kHz 16-bit mono WAV header.
     */
    fun ensureWavFormat(audioBytes: ByteArray, sampleRate: Int = 24000): ByteArray {
        if (isWavHeader(audioBytes)) {
            return audioBytes
        }
        return pcmToWav(audioBytes, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
    }

    fun isWavHeader(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        val riff = String(bytes, 0, 4)
        val wave = String(bytes, 8, 4)
        return riff == "RIFF" && wave == "WAVE"
    }

    /**
     * Converts raw PCM bytes into a standard RIFF/WAVE container.
     */
    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Short = 1,
        bitsPerSample: Short = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))

        // fmt chunk
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat 1 = PCM
        buffer.putShort(channels)
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * bitsPerSample / 8).toShort()) // BlockAlign
        buffer.putShort(bitsPerSample)

        // data chunk
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalAudioLen)

        val out = ByteArrayOutputStream(44 + totalAudioLen)
        out.write(header)
        out.write(pcmData)
        return out.toByteArray()
    }

    /**
     * Joins multiple audio chunks (PCM or WAV) into a single cohesive WAV file.
     */
    fun joinAudioChunks(
        chunkAudios: List<ByteArray>,
        sampleRate: Int = 24000
    ): ByteArray {
        if (chunkAudios.isEmpty()) return ByteArray(0)
        if (chunkAudios.size == 1) return ensureWavFormat(chunkAudios.first(), sampleRate)

        val rawPcmStream = ByteArrayOutputStream()

        for (chunk in chunkAudios) {
            val pcmBytes = if (isWavHeader(chunk)) {
                // Strip the 44-byte WAV header if present
                if (chunk.size > 44) {
                    chunk.copyOfRange(44, chunk.size)
                } else {
                    ByteArray(0)
                }
            } else {
                chunk
            }
            rawPcmStream.write(pcmBytes)
        }

        val allPcm = rawPcmStream.toByteArray()
        return pcmToWav(allPcm, sampleRate = sampleRate)
    }

    /**
     * Writes audio bytes to a cache file for Android MediaPlayer or user export.
     */
    fun saveAudioToCache(context: Context, audioBytes: ByteArray, filename: String = "generated_voice.wav"): File {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { it.write(audioBytes) }
        return file
    }

    /**
     * Generates a pleasant test synth waveform for offline / fallback testing.
     */
    fun generateSyntheticToneAudio(durationSeconds: Float = 2.0f, frequency: Double = 440.0): ByteArray {
        val sampleRate = 24000
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val pcm = ByteArray(totalSamples * 2)
        val buffer = ByteBuffer.wrap(pcm).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val time = i.toDouble() / sampleRate
            // Soft envelope
            val envelope = when {
                time < 0.05 -> time / 0.05
                time > durationSeconds - 0.05 -> (durationSeconds - time) / 0.05
                else -> 1.0
            }
            val angle = 2.0 * Math.PI * frequency * time
            // Add slight harmonic richness
            val sampleVal = (Math.sin(angle) * 0.7 + Math.sin(angle * 2.0) * 0.2 + Math.sin(angle * 3.0) * 0.1) * envelope
            val shortVal = (sampleVal * 16000.0).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortVal)
        }

        return pcmToWav(pcm, sampleRate = sampleRate)
    }
}
