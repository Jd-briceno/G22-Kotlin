package com.g22.orbitsoundkotlin.models

enum class FriendStatus {
    ONLINE, AWAY, OFFLINE
}

data class Friend(
    val id: String,
    val name: String,
    val imageUrl: String,
    val status: FriendStatus,
    val isOnline: Boolean = false
)
