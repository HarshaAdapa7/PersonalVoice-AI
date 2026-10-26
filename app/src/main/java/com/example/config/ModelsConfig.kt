package com.example.config

/**
 * Model names are configuration, not hardcoded strings throughout the codebase.
 * Strictly using supported Gemini model versions.
 */
object ModelsConfig {
    // Primary TTS model identifier
    const val GEMINI_TTS_MODEL = "gemini-2.5-flash-preview-tts"
    
    // Fallback or multi-modal audio model
    const val GEMINI_FLASH_AUDIO_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"

    // Primary Text model for prompt structuring, script drafting and evaluation
    const val GEMINI_TEXT_MODEL = "gemini-3.5-flash"

    // Embedding model for Phase 3 RAG
    const val GEMINI_EMBEDDING_MODEL = "gemini-embedding-2-preview"

    // Prebuilt voices available in Gemini TTS
    val GEMINI_PREBUILT_VOICES = listOf(
        "Puck",    // Playful, clear, mid-range
        "Charon",  // Deep, authoritative, calm
        "Kore",    // Warm, natural, balanced female
        "Fenrir",  // Resonant, energetic, male
        "Aoede",   // Melodic, polished, female
        "Leda",    // Gentle, expressive, female
        "Zephyr",  // Crisp, professional, neutral
        "Orus"     // Dynamic, commanding, male
    )
}
