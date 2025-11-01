package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Telemetría de intentos de login almacenada localmente.
 * Se sincroniza con Firestore cuando hay conectividad.
 */
@Entity(
    tableName = "login_telemetry",
    indices = [Index(value = ["synced"], unique = false)]
)
data class LoginTelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val loginType: LoginType,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val synced: Boolean = false,
    val syncAttempts: Int = 0
)

enum class LoginType {
    EMAIL_PASSWORD,
    GOOGLE,
    APPLE,
    SPOTIFY,
    ANONYMOUS
}

