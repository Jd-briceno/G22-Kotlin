package com.g22.orbitsoundkotlin.models

data class Achievement(
    val id: String,
    val name: String,
    val imageUrl: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
