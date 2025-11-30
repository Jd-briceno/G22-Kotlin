package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.UserDailyActivitySummaryEntity

/**
 * DAO para resúmenes diarios de actividad.
 * 
 * Soporta:
 * - Upsert de summaries por día/usuario
 * - Consultas por rango de fechas o período
 * - Obtención de summaries no sincronizados para Worker
 */
@Dao
interface UserDailyActivitySummaryDao {
    
    /**
     * Inserta o actualiza un summary diario.
     * Usa REPLACE para evitar duplicados por userId+date.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: UserDailyActivitySummaryEntity)
    
    /**
     * Obtiene el summary para un día específico y usuario.
     */
    @Query("SELECT * FROM user_daily_activity_summary WHERE userId = :userId AND date = :date")
    suspend fun getSummaryForDate(userId: String, date: String): UserDailyActivitySummaryEntity?
    
    /**
     * Obtiene summaries para un rango de fechas.
     * Útil para períodos (24h, 7d, 30d).
     */
    @Query("""
        SELECT * FROM user_daily_activity_summary 
        WHERE userId = :userId 
        AND date >= :startDate 
        AND date <= :endDate
        ORDER BY date DESC
    """)
    suspend fun getSummariesInDateRange(
        userId: String,
        startDate: String,
        endDate: String
    ): List<UserDailyActivitySummaryEntity>
    
    /**
     * Obtiene todos los summaries no sincronizados para un usuario.
     * Usado por ActivityStatsSyncWorker.
     */
    @Query("""
        SELECT * FROM user_daily_activity_summary 
        WHERE userId = :userId 
        AND isSynced = 0
        ORDER BY date ASC
    """)
    suspend fun getUnsyncedSummaries(userId: String): List<UserDailyActivitySummaryEntity>
    
    /**
     * Marca summaries como sincronizados.
     */
    @Transaction
    @Query("UPDATE user_daily_activity_summary SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)
    
    /**
     * Elimina summaries antiguos (más de N días).
     * Útil para limpieza periódica.
     */
    @Query("""
        DELETE FROM user_daily_activity_summary 
        WHERE userId = :userId 
        AND date < :cutoffDate
    """)
    suspend fun deleteOldSummaries(userId: String, cutoffDate: String)
    
    /**
     * Elimina todos los summaries de un usuario.
     * Útil para logout o limpieza de datos.
     */
    @Query("DELETE FROM user_daily_activity_summary WHERE userId = :userId")
    suspend fun deleteAllSummariesForUser(userId: String)
}



