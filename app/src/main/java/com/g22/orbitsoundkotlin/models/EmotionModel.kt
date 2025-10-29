// models/EmotionModel.kt
package com.g22.orbitsoundkotlin.models

import androidx.compose.ui.graphics.Color

data class EmotionModel(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val iconRes: Int,
    val source: String
)

data class EmotionControlState(
    val selectedEmotion: EmotionModel,
    val angle: Float
)

data class SliderEmotionControlState(val selectedEmotion: EmotionModel, val value: Float)