package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.EmotionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para manejar emotion logs locales con eventual connectivity
 */
@Dao
interface EmotionLogDao {

    /**
     * Inserta un nuevo emotion log localmente.
     * Retorna el ID del registro insertado.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmotionLog(emotionLog: EmotionLogEntity): Long

    /**
     * Obtiene todos los emotion logs no sincronizados.
     */
    @Query("SELECT * FROM emotion_logs WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedEmotionLogs(): List<EmotionLogEntity>

    /**
     * Obtiene todos los emotion logs de un usuario (sincronizados y no sincronizados).
     */
    @Query("SELECT * FROM emotion_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getEmotionLogs(userId: String): Flow<List<EmotionLogEntity>>

    /**
     * Obtiene emotion logs no sincronizados de un usuario específico.
     */
    @Query("SELECT * FROM emotion_logs WHERE userId = :userId AND synced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedEmotionLogsByUser(userId: String): List<EmotionLogEntity>

    /**
     * Marca un emotion log como sincronizado.
     */
    @Query("UPDATE emotion_logs SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    /**
     * Marca múltiples emotion logs como sincronizados.
     */
    @Query("UPDATE emotion_logs SET synced = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsSynced(ids: List<Long>)

    /**
     * Incrementa los intentos de sincronización y guarda el error.
     */
    @Query("UPDATE emotion_logs SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE id = :id")
    suspend fun incrementSyncAttempts(id: Long, error: String?)

    /**
     * Cuenta cuántos emotion logs no sincronizados hay.
     */
    @Query("SELECT COUNT(*) FROM emotion_logs WHERE synced = 0")
    suspend fun countUnsyncedLogs(): Int

    /**
     * Flow que emite el conteo de logs no sincronizados.
     */
    @Query("SELECT COUNT(*) FROM emotion_logs WHERE synced = 0")
    fun countUnsyncedLogsFlow(): Flow<Int>

    /**
     * Elimina emotion logs antiguos ya sincronizados (para limpieza).
     */
    @Query("DELETE FROM emotion_logs WHERE synced = 1 AND createdAt < :cutoffTime")
    suspend fun deleteOldSyncedLogs(cutoffTime: Long)

    /**
     * Actualiza un emotion log.
     */
    @Update
    suspend fun updateEmotionLog(emotionLog: EmotionLogEntity)

    /**
     * Elimina un emotion log.
     */
    @Delete
    suspend fun deleteEmotionLog(emotionLog: EmotionLogEntity)
}

