package com.example.config

/**
 * System limits and thresholds for generation, chunking, and evaluation.
 */
object LimitsConfig {
    // Max characters per text chunk sent to synthesis models
    const val DEFAULT_MAX_CHARS_PER_CHUNK = 400
    
    // Evaluation pass threshold (WER)
    const val MAX_WER_THRESHOLD = 0.10f

    // Bounded retries limit for self-correction
    const val MAX_GENERATION_RETRIES = 2

    // Delivery parameter clamps
    const val MIN_PACE = 0.5f
    const val MAX_PACE = 2.0f
    const val DEFAULT_PACE = 1.0f

    // Supported delivery tones
    val SUPPORTED_TONES = listOf(
        "Neutral",
        "Professional",
        "Warm & Friendly",
        "Energetic",
        "Calm & Meditative",
        "Authoritative",
        "Storyteller"
    )

    // Supported languages
    val SUPPORTED_LANGUAGES = listOf(
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "es-ES" to "Spanish",
        "fr-FR" to "French",
        "de-DE" to "German",
        "it-IT" to "Italian",
        "ja-JP" to "Japanese",
        "hi-IN" to "Hindi"
    )
}
