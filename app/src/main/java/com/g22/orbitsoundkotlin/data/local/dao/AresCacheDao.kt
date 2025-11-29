package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.AresCacheEntity

/**
 * DAO para cache de recomendaciones de Ares.
 * Implementa queries para SWR pattern y modo híbrido offline.
 */
@Dao
interface AresCacheDao {
    
    /**
     * Obtiene cache válido (no expirado) para un input específico.
     * Usado en modo online para servir cache fresco (<24h).
     */
    @Query("SELECT * FROM ares_cache WHERE userInput = :input AND expiresAt > :currentTime")
    suspend fun getValidCache(
        input: String,
        currentTime: Long = System.currentTimeMillis()
    ): AresCacheEntity?
    
    /**
     * Obtiene cualquier cache (incluso expirado) para un input.
     * Usado en modo híbrido offline para servir cache <24h.
     */
    @Query("SELECT * FROM ares_cache WHERE userInput = :input")
    suspend fun getAnyCacheByInput(input: String): AresCacheEntity?
    
    /**
     * Inserta o actualiza cache (REPLACE si ya existe).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: AresCacheEntity)
    
    /**
     * Actualiza un cache existente.
     */
    @Update
    suspend fun updateCache(cache: AresCacheEntity)
    
    /**
     * Elimina un cache específico.
     */
    @Delete
    suspend fun deleteCache(cache: AresCacheEntity)
    
    /**
     * Limpia todos los caches expirados (>24h).
     * Llamar periódicamente para mantenimiento.
     */
    @Query("DELETE FROM ares_cache WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredCaches(currentTime: Long = System.currentTimeMillis())
    
    /**
     * Obtiene todos los caches de un usuario, ordenados por más reciente.
     * Útil para historial de generaciones.
     */
    @Query("SELECT * FROM ares_cache WHERE userId = :userId ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun getRecentCachesByUser(userId: String, limit: Int = 10): List<AresCacheEntity>
    
    /**
     * Obtiene todos los caches de un usuario.
     */
    @Query("SELECT * FROM ares_cache WHERE userId = :userId ORDER BY cachedAt DESC")
    suspend fun getAllCachesByUser(userId: String): List<AresCacheEntity>
    
    /**
     * Cuenta cuántos caches tiene un usuario.
     */
    @Query("SELECT COUNT(*) FROM ares_cache WHERE userId = :userId")
    suspend fun getCacheCountByUser(userId: String): Int
    
    /**
     * Limpia todos los caches (útil para logout).
     */
    @Query("DELETE FROM ares_cache")
    suspend fun clearAllCache()
    
    /**
     * Limpia caches de un usuario específico.
     */
    @Query("DELETE FROM ares_cache WHERE userId = :userId")
    suspend fun clearCacheByUser(userId: String)
}
