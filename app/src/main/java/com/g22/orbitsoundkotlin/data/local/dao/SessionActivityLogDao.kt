package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.SessionActivityLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones con logs de actividad de sesiones.
 * Proporciona queries eficientes usando índices en userId y sessionStartTimestamp.
 */
@Dao
interface SessionActivityLogDao {
    
    /**
     * Obtiene todos los logs de sesiones para un usuario, ordenados por fecha descendente.
     * Usa índice en userId para consulta eficiente.
     */
    @Query("""
        SELECT * FROM session_activity_logs 
        WHERE userId = :userId 
        ORDER BY sessionStartTimestamp DESC
    """)
    fun getAllLogsForUser(userId: String): Flow<List<SessionActivityLogEntity>>
    
    /**
     * Obtiene logs de sesiones para un usuario en un rango de tiempo.
     * Usa índice compuesto (userId, sessionStartTimestamp) para consulta eficiente.
     */
    @Query("""
        SELECT * FROM session_activity_logs 
        WHERE userId = :userId 
        AND sessionStartTimestamp >= :startTimestamp 
        AND sessionStartTimestamp <= :endTimestamp
        ORDER BY sessionStartTimestamp DESC
    """)
    suspend fun getLogsInTimeRange(
        userId: String,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<SessionActivityLogEntity>
    
    /**
     * Obtiene logs de sesiones válidos (no expirados) para un usuario.
     * Útil para SWR pattern: servir cache válido inmediatamente.
     */
    @Query("""
        SELECT * FROM session_activity_logs 
        WHERE userId = :userId 
        AND expiresAt > :currentTime
        ORDER BY sessionStartTimestamp DESC
        LIMIT :limit
    """)
    suspend fun getValidCache(
        userId: String,
        currentTime: Long = System.currentTimeMillis(),
        limit: Int = 100
    ): List<SessionActivityLogEntity>?
    
    /**
     * Obtiene el log más reciente para un usuario.
     */
    @Query("""
        SELECT * FROM session_activity_logs 
        WHERE userId = :userId 
        ORDER BY sessionStartTimestamp DESC 
        LIMIT 1
    """)
    suspend fun getMostRecentLog(userId: String): SessionActivityLogEntity?
    
    /**
     * Inserta un nuevo log de sesión.
     * Si ya existe un log con el mismo userId y sessionStartTimestamp, lo reemplaza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SessionActivityLogEntity): Long
    
    /**
     * Inserta múltiples logs en batch (más eficiente que insertar uno por uno).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<SessionActivityLogEntity>)
    
    /**
     * Elimina logs expirados para liberar espacio.
     * Debe llamarse periódicamente (ej. al iniciar la app o en un Worker).
     */
    @Query("""
        DELETE FROM session_activity_logs 
        WHERE expiresAt < :currentTime
    """)
    suspend fun deleteExpiredLogs(currentTime: Long = System.currentTimeMillis())
    
    /**
     * Elimina todos los logs de un usuario.
     * Útil para limpieza de datos o logout.
     */
    @Query("DELETE FROM session_activity_logs WHERE userId = :userId")
    suspend fun deleteAllLogsForUser(userId: String)
    
    /**
     * Cuenta el total de logs para un usuario.
     */
    @Query("SELECT COUNT(*) FROM session_activity_logs WHERE userId = :userId")
    suspend fun countLogsForUser(userId: String): Int
    
    /**
     * Obtiene logs de sesiones para un período específico (último día/semana/mes).
     */
    @Query("""
        SELECT * FROM session_activity_logs 
        WHERE userId = :userId 
        AND sessionStartTimestamp >= :periodStartTimestamp
        ORDER BY sessionStartTimestamp DESC
    """)
    suspend fun getLogsForPeriod(
        userId: String,
        periodStartTimestamp: Long
    ): List<SessionActivityLogEntity>
}


