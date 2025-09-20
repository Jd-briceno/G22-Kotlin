package com.g22.orbitsoundkotlin.models

data class UserProfile(
    val id: String,
    val username: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
    val isPremium: Boolean = false,
    val qrData: String,
    val achievements: List<Achievement> = emptyList(),
    val friends: List<Friend> = emptyList()
)
