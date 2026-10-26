package com.example.engines

/**
 * Registry holding available voice engines and managing resolution.
 */
class EngineRegistry {

    private val engines = mutableMapOf<String, VoiceEngine>()

    val geminiEngine = GeminiTTSEngine()
    val externalEngine = ExternalCloneEngine()
    val offlineEngine = OfflinePreviewEngine()

    init {
        register(geminiEngine)
        register(externalEngine)
        register(offlineEngine)
    }

    fun register(engine: VoiceEngine) {
        engines[engine.id] = engine
    }

    fun getEngine(id: String): VoiceEngine? = engines[id]

    fun getAllEngines(): List<VoiceEngine> = engines.values.toList()

    fun getDefaultEngine(): VoiceEngine {
        // If Gemini has a valid key configured, default to Gemini, otherwise offer Gemini or offline
        return geminiEngine
    }
}
