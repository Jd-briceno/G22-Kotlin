package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.EmotionLog

interface EmotionRepository {
    suspend fun logEmotions(emotionLog: EmotionLog): Result<Unit>
}
