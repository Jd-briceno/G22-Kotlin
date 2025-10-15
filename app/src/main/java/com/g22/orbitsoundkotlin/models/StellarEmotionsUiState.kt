package com.g22.orbitsoundkotlin.models

data class StellarEmotionsUiState(
    val selectedEmotions: List<EmotionModel> = emptyList(),
    val isLoading: Boolean = false,
    val lastSubmittedAt: Long? = null
)
