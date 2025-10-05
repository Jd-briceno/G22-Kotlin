// models/EmotionModel.kt
package com.g22.orbitsoundkotlin.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a domain model for a single emotion in the OrbitSound application.
 * This model is used by the UI and AppState layers, isolating them from
 * any specific data source (like a database or external API).
 *
 * @property id A unique identifier for the emotion (e.g., "anger", "joy").
 * @property name A user-friendly name for the emotion.
 * @property description A brief phrase or sentence describing the feeling.
 * @property color The primary color associated with this emotion in the UI.
 * @property icon The ImageVector used to represent the emotion in the UI.
 */
data class EmotionModel(
    val id: String,
    val name: String,
    val description: String,
    val color: Color,
    val iconRes: Int,
    val source: String
)

/**
 * Represents the state of a specific selection control, like one of the Knobs.
 * It maps the angle/position of the control to the selected EmotionModel.
 */
data class EmotionControlState(
    val selectedEmotion: EmotionModel,
    val angle: Float // The current angle of the knob's indicator
)

data class SliderEmotionControlState(val selectedEmotion: EmotionModel, val value: Float)