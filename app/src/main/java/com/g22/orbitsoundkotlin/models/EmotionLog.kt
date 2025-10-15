package com.g22.orbitsoundkotlin.models

import com.google.firebase.Timestamp

data class EmotionLog(
    val userId: String,
    val timestamp: Timestamp,
    val emotions: List<EmotionEntry>
) {
    data class EmotionEntry(
        val id: String,
        val name: String,
        val source: String
    )
}

// Extension function to convert UI EmotionModel to domain EmotionEntry
fun EmotionModel.toEmotionEntry(): EmotionLog.EmotionEntry {
    return EmotionLog.EmotionEntry(
        id = id,
        name = name,
        source = source
    )
}