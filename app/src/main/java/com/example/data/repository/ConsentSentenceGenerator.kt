package com.example.data.repository

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object ConsentSentenceGenerator {

    private val phrases = listOf(
        "I hereby verify that this is my own voice and grant explicit authorization to PersonalVoice AI to construct a personalized voice profile for my account.",
        "I authorize PersonalVoice AI to generate speech based solely on my explicit consent, and confirm that I am the sole owner of this voice.",
        "I declare that I am the authorized account holder and permit PersonalVoice AI to record and process my vocal identity under strict privacy controls.",
        "My voice is my identity. I grant PersonalVoice AI permission to model my speaking style strictly for verified personal use."
    )

    fun generateSentence(displayName: String): String {
        val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
        val randomCode = Random.nextInt(1000, 9999)
        val template = phrases.random()
        return "$template Verification code $randomCode. Consented by $displayName on $dateStr."
    }
}
