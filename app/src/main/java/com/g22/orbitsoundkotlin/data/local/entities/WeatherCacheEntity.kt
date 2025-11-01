package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache de datos de clima con TTL (Time To Live).
 * Usa SWR pattern: sirve cache inmediatamente mientras actualiza en background.
 */
@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val uid: String,
    val temperatureC: Float,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long // TTL: 5 minutos por defecto
) {
    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutos

        fun calculateExpiresAt(): Long {
            return System.currentTimeMillis() + CACHE_TTL_MS
        }
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
}

