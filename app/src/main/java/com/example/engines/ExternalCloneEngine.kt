package com.example.engines

import com.example.config.LimitsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

/**
 * ExternalCloneEngine adapter.
 * Documented stub for external voice cloning backends (e.g., self-hosted F5-TTS,
 * CosyVoice 2 server, or hosted cloning provider).
 * Disabled by default.
 */
class ExternalCloneEngine(
    private var endpointUrl: String? = null,
    private var isEnabled: Boolean = false
) : VoiceEngine {

    override val id: String = "external-clone"
    override val displayName: String = "External Cloning Engine (F5-TTS / CosyVoice 2)"
    override val description: String = "Documented external zero-shot voice cloning adapter. Requires a dedicated self-hosted or cloud inference endpoint."

    private val httpClient = OkHttpClient()

    fun configure(endpoint: String?, enabled: Boolean) {
        endpointUrl = endpoint?.trim()
        isEnabled = enabled
    }

    fun isConfiguredAndEnabled(): Boolean = isEnabled && !endpointUrl.isNullOrBlank()

    override fun getCapabilities(): EngineCapabilities = EngineCapabilities(
        languages = LimitsConfig.SUPPORTED_LANGUAGES.map { it.first },
        supportedVoices = listOf("Cloned Profile Reference"),
        supportsCloning = true, // Genuine cloning capability when external server is connected
        supportedDelivery = listOf(
            DeliveryFeature.PACE,
            DeliveryFeature.PAUSES,
            DeliveryFeature.EMPHASIS,
            DeliveryFeature.TONE
        ),
        maxCharsPerChunk = 500
    )

    override suspend fun createProfile(
        userId: String,
        displayName: String,
        sampleAudio: ByteArray?
    ): String? = withContext(Dispatchers.IO) {
        if (!isConfiguredAndEnabled()) {
            throw IllegalStateException(
                "ExternalCloneEngine is disabled by default. Configure a valid F5-TTS/CosyVoice endpoint in settings to enroll custom cloned voices."
            )
        }

        // Stub implementation: call external enrollment endpoint
        val url = "$endpointUrl/v1/profiles"
        val jsonPayload = JSONObject().apply {
            put("userId", userId)
            put("displayName", displayName)
            if (sampleAudio != null) {
                put("sampleAudioBase64", android.util.Base64.encodeToString(sampleAudio, android.util.Base64.NO_WRAP))
            }
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody(mediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("External clone enrollment failed: HTTP ${response.code}")
        }
        val body = response.body?.string().orEmpty()
        JSONObject(body).optString("engineProfileRef", "ext_ref_${System.currentTimeMillis()}")
    }

    override suspend fun synthesize(
        text: String,
        profile: VoiceProfile,
        language: String,
        delivery: DeliverySettings
    ): SynthesisOutput = withContext(Dispatchers.IO) {
        if (!isConfiguredAndEnabled()) {
            throw IllegalStateException(
                "ExternalCloneEngine is disabled. To use true voice cloning, configure and connect an F5-TTS or CosyVoice 2 server."
            )
        }

        val url = "$endpointUrl/v1/synthesize"
        val payload = JSONObject().apply {
            put("text", text)
            put("profileRef", profile.engineProfileRef)
            put("language", language)
            put("delivery", JSONObject().apply {
                put("pace", delivery.pace)
                put("tone", delivery.tone)
                put("pauses", delivery.pauses)
                put("emphasis", delivery.emphasis)
            })
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(mediaType))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("External cloning synthesis failed: HTTP ${response.code}")
        }
        val audioBytes = response.body?.bytes() ?: ByteArray(0)
        SynthesisOutput(
            audioBytes = audioBytes,
            mimeType = "audio/wav",
            engineId = id,
            engineName = displayName,
            chunksCount = 1,
            durationSeconds = audioBytes.size / (24000f * 2f),
            textLength = text.length
        )
    }

    override suspend fun deleteProfile(ref: String) = withContext(Dispatchers.IO) {
        if (isConfiguredAndEnabled()) {
            runCatching {
                val url = "$endpointUrl/v1/profiles/$ref"
                val request = Request.Builder().url(url).delete().build()
                httpClient.newCall(request).execute().close()
            }
        }
    }

    override suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfiguredAndEnabled()) return@withContext false
        try {
            val url = "$endpointUrl/health"
            val request = Request.Builder().url(url).get().build()
            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
