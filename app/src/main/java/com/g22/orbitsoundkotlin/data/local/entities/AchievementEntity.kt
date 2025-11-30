package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing unlocked achievements locally.
 * Syncs with Firebase using offline-first pattern.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val id: String, // Composite: userId_achievementId
    val userId: String,
    val achievementId: String,
    val unlockedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_SYNC
)

