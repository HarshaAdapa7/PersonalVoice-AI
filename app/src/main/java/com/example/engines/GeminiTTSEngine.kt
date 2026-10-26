package com.example.engines

import android.util.Base64
import com.example.BuildConfig
import com.example.audio.AudioUtils
import com.example.config.LimitsConfig
import com.example.config.ModelsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiTTSEngine(
    private var customApiKey: String? = null
) : VoiceEngine {

    override val id: String = "gemini-tts"
    override val displayName: String = "Gemini TTS Engine"
    override val description: String = "Prebuilt expressive voices with prompt-based tone & pace control (Gemini 2.5 TTS). Does not clone audio."

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun setRuntimeApiKey(key: String?) {
        customApiKey = key?.trim()
    }

    fun getEffectiveApiKey(): String {
        val key = customApiKey?.takeIf { it.isNotBlank() }
            ?: runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()
            ?: ""
        return key.trim()
    }

    override fun getCapabilities(): EngineCapabilities = EngineCapabilities(
        languages = LimitsConfig.SUPPORTED_LANGUAGES.map { it.first },
        supportedVoices = ModelsConfig.GEMINI_PREBUILT_VOICES,
        supportsCloning = false, // Honest: Gemini TTS uses prebuilt voices, not cloning
        supportedDelivery = listOf(DeliveryFeature.PACE, DeliveryFeature.TONE),
        maxCharsPerChunk = LimitsConfig.DEFAULT_MAX_CHARS_PER_CHUNK
    )

    override suspend fun createProfile(
        userId: String,
        displayName: String,
        sampleAudio: ByteArray?
    ): String {
        // For Gemini TTS, profile reference maps to prebuilt voice name
        return "gemini_voice_${displayName.lowercase().replace(" ", "_")}"
    }

    override suspend fun synthesize(
        text: String,
        profile: VoiceProfile,
        language: String,
        delivery: DeliverySettings
    ): SynthesisOutput = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "Gemini API key is missing or placeholder. Please provide a valid Gemini API key in Settings or AI Studio Secrets."
            )
        }

        val capabilities = getCapabilities()
        val clampedDelivery = delivery.clampToCapabilities(capabilities)
        val chunks = AudioUtils.splitIntoSentenceChunks(text, capabilities.maxCharsPerChunk)

        if (chunks.isEmpty()) {
            throw IllegalArgumentException("Text cannot be empty.")
        }

        val voiceName = if (profile.voiceName in capabilities.supportedVoices) {
            profile.voiceName
        } else {
            capabilities.supportedVoices.firstOrNull() ?: "Puck"
        }

        val chunkAudioResults = mutableListOf<ByteArray>()
        var resolvedMimeType = "audio/wav"

        for (chunkText in chunks) {
            val audioBytes = synthesizeSingleChunk(
                apiKey = apiKey,
                chunkText = chunkText,
                voiceName = voiceName,
                language = language,
                delivery = clampedDelivery
            )
            chunkAudioResults.add(audioBytes)
        }

        val finalAudio = AudioUtils.joinAudioChunks(chunkAudioResults, sampleRate = 24000)
        val estimatedDuration = (finalAudio.size - 44).coerceAtLeast(0).toFloat() / (24000f * 2f)

        SynthesisOutput(
            audioBytes = finalAudio,
            mimeType = resolvedMimeType,
            engineId = id,
            engineName = displayName,
            chunksCount = chunks.size,
            durationSeconds = estimatedDuration,
            textLength = text.length
        )
    }

    private fun synthesizeSingleChunk(
        apiKey: String,
        chunkText: String,
        voiceName: String,
        language: String,
        delivery: DeliverySettings
    ): ByteArray {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${ModelsConfig.GEMINI_TTS_MODEL}:generateContent?key=$apiKey"

        val stylePrompt = buildString {
            append("Tone: ${delivery.tone}. ")
            if (delivery.pace != 1.0f) {
                append("Pace: ${delivery.pace}x. ")
            }
            append("Language: $language. ")
            append("Speak the following text clearly and naturally, without any introductory or concluding comments:\n\n")
            append(chunkText)
        }

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", stylePrompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().apply {
                    put("AUDIO")
                })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", voiceName)
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            val errorMsg = try {
                val errObj = JSONObject(bodyString).optJSONObject("error")
                errObj?.optString("message") ?: "HTTP ${response.code}: $bodyString"
            } catch (e: Exception) {
                "HTTP ${response.code}: ${response.message}"
            }
            throw IOException("Gemini TTS API error: $errorMsg")
        }

        val jsonResponse = JSONObject(bodyString)
        val candidates = jsonResponse.optJSONArray("candidates")
            ?: throw IOException("No audio candidates returned by Gemini TTS")

        if (candidates.length() == 0) {
            throw IOException("Empty candidate list returned by Gemini TTS")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
            ?: throw IOException("Missing content object in Gemini response")

        val parts = content.optJSONArray("parts")
            ?: throw IOException("Missing parts array in Gemini response")

        var base64Data: String? = null
        var mimeType = "audio/wav"

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            val inlineData = part.optJSONObject("inlineData")
            if (inlineData != null) {
                base64Data = inlineData.optString("data")
                mimeType = inlineData.optString("mimeType", "audio/wav")
                break
            }
        }

        if (base64Data.isNullOrBlank()) {
            throw IOException("No audio inlineData found in response parts")
        }

        val rawBytes = Base64.decode(base64Data, Base64.DEFAULT)
        return AudioUtils.ensureWavFormat(rawBytes, sampleRate = 24000)
    }

    override suspend fun deleteProfile(ref: String) {
        // No-op for prebuilt Gemini cloud voices
    }

    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") return@withContext false
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
