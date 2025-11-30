package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entrada del diario de Activity Stats.
 * 
 * Permite múltiples entradas por día.
 * Almacenamiento local persistente en Room.
 * 
 * SEGURIDAD: Filtrado por userId para evitar exposición de datos de otros usuarios.
 */
@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["userId", "date"], unique = false),
        Index(value = ["userId", "createdAt"], unique = false)
    ]
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String, // Formato: "YYYY-MM-DD" (solo fecha, sin hora)
    val text: String, // Contenido de la entrada (máx ~300 caracteres)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)



