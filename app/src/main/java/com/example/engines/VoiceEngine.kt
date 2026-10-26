package com.example.engines

enum class DeliveryFeature(val label: String) {
    PACE("pace"),
    PAUSES("pauses"),
    EMPHASIS("emphasis"),
    TONE("tone")
}

data class EngineCapabilities(
    val languages: List<String>,
    val supportedVoices: List<String> = emptyList(),
    val supportsCloning: Boolean,
    val supportedDelivery: List<DeliveryFeature>,
    val maxCharsPerChunk: Int
)

data class DeliverySettings(
    val pace: Float = 1.0f,
    val pauses: Float = 1.0f,
    val emphasis: String = "Normal",
    val tone: String = "Neutral"
) {
    /**
     * Clamps settings against the engine's stated capabilities.
     */
    fun clampToCapabilities(capabilities: EngineCapabilities): DeliverySettings {
        val hasPace = capabilities.supportedDelivery.contains(DeliveryFeature.PACE)
        val hasTone = capabilities.supportedDelivery.contains(DeliveryFeature.TONE)
        val hasPauses = capabilities.supportedDelivery.contains(DeliveryFeature.PAUSES)
        val hasEmphasis = capabilities.supportedDelivery.contains(DeliveryFeature.EMPHASIS)

        return this.copy(
            pace = if (hasPace) pace.coerceIn(0.5f, 2.0f) else 1.0f,
            tone = if (hasTone) tone else "Neutral",
            pauses = if (hasPauses) pauses.coerceIn(0.5f, 2.0f) else 1.0f,
            emphasis = if (hasEmphasis) emphasis else "Normal"
        )
    }
}

data class VoiceProfile(
    val id: String,
    val userId: String,
    val displayName: String,
    val engineId: String,
    val engineModelVersion: String,
    val engineProfileRef: String? = null,
    val voiceName: String = "Puck",
    val status: String = "active",
    val consentRecordId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class SynthesisOutput(
    val audioBytes: ByteArray,
    val mimeType: String,
    val engineId: String,
    val engineName: String,
    val chunksCount: Int,
    val durationSeconds: Float = 0f,
    val textLength: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SynthesisOutput
        return audioBytes.contentEquals(other.audioBytes) &&
                mimeType == other.mimeType &&
                engineId == other.engineId
    }

    override fun hashCode(): Int {
        var result = audioBytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + engineId.hashCode()
        return result
    }
}

/**
 * Universal VoiceEngine interface as defined in PersonalVoice AI specification.
 */
interface VoiceEngine {
    val id: String
    val displayName: String
    val description: String

    fun getCapabilities(): EngineCapabilities

    suspend fun createProfile(
        userId: String,
        displayName: String,
        sampleAudio: ByteArray? = null
    ): String?

    suspend fun synthesize(
        text: String,
        profile: VoiceProfile,
        language: String,
        delivery: DeliverySettings
    ): SynthesisOutput

    suspend fun deleteProfile(ref: String)

    suspend fun healthCheck(): Boolean
}
