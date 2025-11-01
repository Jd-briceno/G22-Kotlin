package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache de sesión encriptada para login offline.
 * Las credenciales están encriptadas usando Security Crypto.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val uid: String,
    val encryptedEmail: String,
    val encryptedPassword: String, // Hasheado, no plaintext
    val rememberMe: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L // 0 = never expires
) {
    fun isExpired(): Boolean {
        if (expiresAt == 0L) return false
        return System.currentTimeMillis() > expiresAt
    }
}

