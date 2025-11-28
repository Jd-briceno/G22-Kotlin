package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Log de actividad de sesión procesado y agregado.
 * Almacena métricas calculadas de una sesión de usuario para evitar recálculo.
 * 
 * Una sesión se define como un período de actividad del usuario que comienza
 * con un login exitoso y termina después de 30 minutos de inactividad.
 * 
 * Cache con TTL de 24 horas para balancear frescura vs rendimiento.
 */
@Entity(
    tableName = "session_activity_logs",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["sessionStartTimestamp"]),
        Index(value = ["userId", "sessionStartTimestamp"])
    ]
)
data class SessionActivityLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val sessionStartTimestamp: Long, // Timestamp del login que inició la sesión
    val sessionEndTimestamp: Long,   // Timestamp del último evento + 30 min de inactividad
    val durationMinutes: Int,        // Duración calculada en minutos
    val loginType: String,           // Tipo de login (EMAIL_PASSWORD, GOOGLE, SPOTIFY, etc.)
    val totalActions: Int,           // Total de acciones realizadas en la sesión
    val actionTypesJson: String,     // JSON: Map<String, Int> (tipo de acción -> conteo)
    val totalSearches: Int,           // Total de búsquedas realizadas
    val searchQueriesJson: String,   // JSON: List<String> (últimas 10 búsquedas)
    val processedAt: Long = System.currentTimeMillis(), // Timestamp de procesamiento
    val cachedAt: Long = System.currentTimeMillis(),     // Timestamp de cache
    val expiresAt: Long                // TTL: 24 horas por defecto
) {
    companion object {
        private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 horas

        /**
         * Calcula el timestamp de expiración del cache.
         */
        fun calculateExpiresAt(): Long {
            return System.currentTimeMillis() + CACHE_TTL_MS
        }

        /**
         * Constante para ventana de sesión: 30 minutos de inactividad cierra una sesión.
         */
        const val SESSION_INACTIVITY_WINDOW_MS = 30 * 60 * 1000L // 30 minutos
    }

    /**
     * Verifica si el cache está expirado.
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
}


