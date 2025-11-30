package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache de recomendaciones de Ares (AI-powered playlists) con TTL.
 * Usa SWR pattern: sirve cache inmediatamente mientras actualiza en background.
 * 
 * TTL Strategy:
 * - Gemini queries: 24 horas (cache conservador)
 * - Spotify tracks: 6 horas (fresh data)
 * - Modo híbrido offline: cache <24h válido
 */
@Entity(tableName = "ares_cache")
data class AresCacheEntity(
    @PrimaryKey
    val userInput: String, // Normalized input (lowercase, trimmed) como key única
    val geminiResponse: String, // Queries generadas por Gemini (JSON array)
    val trackIds: String, // IDs simples de tracks (JSON array) - para referencia
    val trackData: String, // Datos completos de tracks (JSON) - para reconstruir List<Track>
    val userId: String, // Usuario que generó la playlist
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long // TTL: 24 horas para modo híbrido
) {
    companion object {
        // Cache conservador: 24 horas
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 horas
        
        // TTL corto para tracks (6 horas) - usado en validación de freshness
        const val FRESH_CACHE_TTL_MS = 6 * 60 * 60 * 1000L // 6 horas

        fun calculateExpiresAt(): Long {
            return System.currentTimeMillis() + CACHE_TTL_MS
        }
    }

    /**
     * Verifica si el cache ha expirado completamente (>24h).
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Verifica si el cache es "fresco" (<6h) para tracks de Spotify.
     */
    fun isFresh(): Boolean {
        val age = System.currentTimeMillis() - cachedAt
        return age < FRESH_CACHE_TTL_MS
    }
    
    /**
     * Edad del cache en milisegundos.
     */
    fun getAge(): Long {
        return System.currentTimeMillis() - cachedAt
    }
}
