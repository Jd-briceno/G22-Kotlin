// models/EmotionModel.kt
package com.g22.orbitsoundkotlin.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class EmotionModel(
    val name: String,
    val id: String = "",
    val description: String = "",
    val color: Color = Color.Transparent,
    val iconRes: Int = 0,
    val source: String = ""
)

data class EmotionControlState(
    val selectedEmotion: EmotionModel,
    val angle: Float
)

data class SliderEmotionControlState(val selectedEmotion: EmotionModel, val value: Float)