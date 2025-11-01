package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Representación local de un usuario en Room Database.
 * Usado para cache de perfiles y sincronización eventual.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val name: String,
    val syncStatus: SyncStatus,
    val localUid: String? = null, // Temporal hasta reconciliación
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L,
    val version: Int = 1
)

enum class SyncStatus {
    PENDING_SYNC,  // Creado localmente, no sincronizado
    SYNCED,        // Sincronizado con Firebase
    SYNC_ERROR,    // Error al sincronizar (intentar de nuevo)
    CONFLICT       // Conflicto detectado, requiere resolución
}

