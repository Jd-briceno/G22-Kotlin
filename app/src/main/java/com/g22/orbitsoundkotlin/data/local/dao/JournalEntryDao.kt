package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.JournalEntryEntity

/**
 * DAO para entradas del diario de Activity Stats.
 * 
 * Soporta CRUD completo:
 * - Crear entrada
 * - Leer entradas por fecha
 * - Actualizar entrada
 * - Eliminar entrada
 */
@Dao
interface JournalEntryDao {
    
    /**
     * Inserta una nueva entrada del diario.
     */
    @Insert
    suspend fun insertEntry(entry: JournalEntryEntity): Long
    
    /**
     * Actualiza una entrada existente.
     */
    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)
    
    /**
     * Elimina una entrada por ID.
     */
    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)
    
    /**
     * Obtiene una entrada por ID.
     */
    @Query("SELECT * FROM journal_entries WHERE id = :id AND userId = :userId")
    suspend fun getEntryById(userId: String, id: Long): JournalEntryEntity?
    
    /**
     * Obtiene todas las entradas de un día específico para un usuario.
     * Ordenadas por fecha/hora de creación (más recientes primero).
     */
    @Query("""
        SELECT * FROM journal_entries 
        WHERE userId = :userId AND date = :date
        ORDER BY createdAt DESC
    """)
    suspend fun getEntriesForDate(userId: String, date: String): List<JournalEntryEntity>
    
    /**
     * Obtiene las últimas N entradas de un usuario.
     * Útil para mostrar entradas recientes.
     */
    @Query("""
        SELECT * FROM journal_entries 
        WHERE userId = :userId
        ORDER BY createdAt DESC
        LIMIT :limit
    """)
    suspend fun getRecentEntries(userId: String, limit: Int = 10): List<JournalEntryEntity>
    
    /**
     * Obtiene todas las fechas que tienen entradas para un usuario.
     * Útil para navegación entre días.
     */
    @Query("""
        SELECT DISTINCT date FROM journal_entries 
        WHERE userId = :userId
        ORDER BY date DESC
    """)
    suspend fun getDatesWithEntries(userId: String): List<String>
    
    /**
     * Elimina todas las entradas de un usuario.
     * Útil para logout o limpieza de datos.
     */
    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteAllEntriesForUser(userId: String)
    
    /**
     * Obtiene todas las entradas en un rango de fechas para un usuario.
     * Query simple que Room puede mapear sin problemas.
     * Usado para construir la timeline de días.
     */
    @Query("""
        SELECT * FROM journal_entries 
        WHERE userId = :userId 
        AND date >= :fromDate 
        AND date <= :toDate
        ORDER BY date DESC, createdAt DESC
    """)
    suspend fun getEntriesInDateRange(
        userId: String,
        fromDate: String,
        toDate: String
    ): List<JournalEntryEntity>
}



