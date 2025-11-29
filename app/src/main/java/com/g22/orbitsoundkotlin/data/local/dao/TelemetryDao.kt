package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.LoginTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM login_telemetry WHERE synced = 0")
    suspend fun getUnsyncedTelemetry(): List<LoginTelemetryEntity>

    @Query("SELECT * FROM login_telemetry WHERE synced = 0")
    fun getUnsyncedTelemetryFlow(): Flow<List<LoginTelemetryEntity>>

    @Query("SELECT COUNT(*) FROM login_telemetry WHERE synced = 0")
    suspend fun countUnsyncedTelemetry(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(telemetry: LoginTelemetryEntity): Long

    @Update
    suspend fun updateTelemetry(telemetry: LoginTelemetryEntity)

    @Transaction
    @Query("UPDATE login_telemetry SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Transaction
    @Query("UPDATE login_telemetry SET syncAttempts = syncAttempts + 1, errorMessage = :error WHERE id = :id")
    suspend fun incrementSyncAttempts(id: Long, error: String?)

    @Query("DELETE FROM login_telemetry WHERE synced = 1 AND timestamp < :cutoffTime")
    suspend fun deleteOldSyncedTelemetry(cutoffTime: Long)

    @Query("DELETE FROM login_telemetry")
    suspend fun clearAllTelemetry()
    
    /**
     * Obtiene todos los logins (sincronizados y no sincronizados) en un rango de tiempo.
     * Útil para procesar historial completo de sesiones.
     */
    @Query("SELECT * FROM login_telemetry WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTelemetryInTimeRange(
        startTime: Long,
        endTime: Long
    ): List<LoginTelemetryEntity>
}

