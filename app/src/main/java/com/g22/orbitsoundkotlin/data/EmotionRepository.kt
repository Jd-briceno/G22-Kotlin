package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.EmotionLog
import kotlinx.coroutines.flow.Flow

interface EmotionRepository {
    suspend fun logEmotions(userId: String, emotions: EmotionLog): Result<Unit>
    fun getEmotionLogs(userId: String): Flow<List<EmotionLog>>
}