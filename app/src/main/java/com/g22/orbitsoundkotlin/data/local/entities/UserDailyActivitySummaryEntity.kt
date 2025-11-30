package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Resumen diario de actividad del usuario.
 * 
 * Almacena métricas agregadas por día para:
 * - Alimentar las cards de summary en ActivityStatsScreen
 * - Sincronización a Firestore para futura BQ
 * - Base de datos para análisis de patrones de uso
 * 
 * SEGURIDAD: Filtrado por userId para evitar exposición de datos de otros usuarios.
 * EVENTUAL CONNECTIVITY: Campo isSynced marca si ya se subió a Firestore.
 */
@Entity(
    tableName = "user_daily_activity_summary",
    indices = [
        Index(value = ["userId", "date"], unique = true),
        Index(value = ["userId", "isSynced"], unique = false)
    ]
)
data class UserDailyActivitySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Formato: "YYYY-MM-DD" (solo fecha, sin hora)
    val sessionsCount: Int = 0,
    val totalTimeMinutes: Int = 0,
    val mostCommonAction: String = "", // Ej: "search", "like", "play"
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // Para eventual connectivity
)



