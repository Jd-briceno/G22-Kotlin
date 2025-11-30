package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.EmotionLog
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.toEmotionEntry
import com.google.firebase.Timestamp

/**
 * Factory interface for creating EmotionLog instances.
 * Implemented by repositories that can create EmotionLog from UI models.
 */
interface EmotionLogFactory {
    fun createEmotionLog(userId: String, emotions: List<EmotionModel>): EmotionLog
}

/**
 * Default implementation of EmotionLogFactory.
 */
object DefaultEmotionLogFactory : EmotionLogFactory {
    override fun createEmotionLog(userId: String, emotions: List<EmotionModel>): EmotionLog {
        return EmotionLog(
            userId = userId,
            timestamp = Timestamp.now(),
            emotions = emotions.map { it.toEmotionEntry() }
        )
    }
}

