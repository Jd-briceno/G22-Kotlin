package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity
import com.g22.orbitsoundkotlin.data.local.entities.OutboxOperationType
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedOperations(): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE synced = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getUnsyncedOperations(limit: Int): List<OutboxEntity>

    @Query("SELECT * FROM outbox WHERE synced = 0")
    fun getUnsyncedOperationsFlow(): Flow<List<OutboxEntity>>

    @Query("SELECT * FROM outbox WHERE synced = 0 AND operationType = :operationType ORDER BY createdAt ASC")
    suspend fun getUnsyncedOperationsByType(operationType: OutboxOperationType): List<OutboxEntity>

    @Query("SELECT COUNT(*) FROM outbox WHERE synced = 0")
    suspend fun countUnsyncedOperations(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: OutboxEntity): Long

    @Update
    suspend fun updateOperation(operation: OutboxEntity)

    @Delete
    suspend fun deleteOperation(operation: OutboxEntity)

    @Transaction
    @Query("UPDATE outbox SET synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Transaction
    @Query("UPDATE outbox SET syncAttempts = syncAttempts + 1, lastError = :error WHERE id = :id")
    suspend fun incrementSyncAttempts(id: Long, error: String?)

    @Query("DELETE FROM outbox WHERE synced = 1 AND createdAt < :cutoffTime")
    suspend fun deleteOldSyncedOperations(cutoffTime: Long)

    @Query("DELETE FROM outbox")
    suspend fun clearAllOperations()
    
    /**
     * Obtiene todas las operaciones (sincronizadas y no sincronizadas) en un rango de tiempo.
     * Útil para procesar historial completo de actividad.
     */
    @Query("SELECT * FROM outbox WHERE createdAt >= :startTime AND createdAt <= :endTime")
    suspend fun getOperationsInTimeRange(
        startTime: Long,
        endTime: Long
    ): List<OutboxEntity>
}

