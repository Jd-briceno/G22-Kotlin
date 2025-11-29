package com.g22.orbitsoundkotlin.data.repositories

import android.util.Log
import com.g22.orbitsoundkotlin.data.AchievementDefinitions
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.AchievementEntity
import com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity
import com.g22.orbitsoundkotlin.data.local.entities.OutboxOperationType
import com.g22.orbitsoundkotlin.data.local.entities.SyncStatus
import com.g22.orbitsoundkotlin.models.Achievement
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for achievements with offline-first pattern.
 * Saves to Room + Outbox for Firebase sync.
 */
class AchievementRepository(
    private val db: AppDatabase
) {
    private val achievementDao = db.achievementDao()
    private val outboxDao = db.outboxDao()
    private val gson = Gson()
    
    companion object {
        private const val TAG = "AchievementRepository"
        private const val COLLECTION_ACHIEVEMENTS = "achievements"
    }
    
    /**
     * Get all unlocked achievements for a user.
     * Merges definitions with unlock status from Room.
     */
    suspend fun getUserAchievements(userId: String): List<Achievement> {
        val unlockedEntities = achievementDao.getUserAchievements(userId)
        val unlockedMap = unlockedEntities.associateBy { it.achievementId }
        
        return AchievementDefinitions.ALL.map { definition ->
            val entity = unlockedMap[definition.id]
            definition.copy(
                isUnlocked = entity != null,
                unlockedAt = entity?.unlockedAt
            )
        }
    }
    
    /**
     * Get achievements as Flow for reactive UI updates.
     */
    fun getUserAchievementsFlow(userId: String): Flow<List<Achievement>> {
        return achievementDao.getUserAchievementsFlow(userId).map { entities ->
            val unlockedMap = entities.associateBy { it.achievementId }
            
            AchievementDefinitions.ALL.map { definition ->
                val entity = unlockedMap[definition.id]
                definition.copy(
                    isUnlocked = entity != null,
                    unlockedAt = entity?.unlockedAt
                )
            }
        }
    }
    
    /**
     * Get only unlocked achievements for a user.
     */
    suspend fun getUnlockedAchievements(userId: String): List<Achievement> {
        return getUserAchievements(userId).filter { it.isUnlocked }
    }
    
    /**
     * Check if a specific achievement is already unlocked.
     */
    suspend fun isAchievementUnlocked(userId: String, achievementId: String): Boolean {
        return achievementDao.isAchievementUnlocked(userId, achievementId)
    }
    
    /**
     * Unlock an achievement. Saves to Room and enqueues for Firebase sync.
     * Returns true if newly unlocked, false if already unlocked.
     */
    suspend fun unlockAchievement(userId: String, achievementId: String): Boolean {
        Log.d(TAG, "Attempting to unlock achievement: $achievementId for user: $userId")
        
        // Check if already unlocked
        if (isAchievementUnlocked(userId, achievementId)) {
            Log.d(TAG, "⚠️ Achievement $achievementId already unlocked for user $userId")
            return false
        }
        
        val now = System.currentTimeMillis()
        val entity = AchievementEntity(
            id = "${userId}_${achievementId}",
            userId = userId,
            achievementId = achievementId,
            unlockedAt = now,
            syncStatus = SyncStatus.PENDING_SYNC
        )
        
        // Save to Room
        achievementDao.insertAchievement(entity)
        Log.d(TAG, "✅ Achievement $achievementId SAVED to Room for user $userId")
        
        // Add to Outbox for Firebase sync
        enqueueForSync(userId, achievementId, now)
        
        return true
    }
    
    /**
     * Check and unlock achievement if not already unlocked.
     * Returns the Achievement object with updated status if newly unlocked, null otherwise.
     */
    suspend fun checkAndUnlock(userId: String, achievementId: String): Achievement? {
        val wasUnlocked = unlockAchievement(userId, achievementId)
        
        return if (wasUnlocked) {
            AchievementDefinitions.getById(achievementId)?.copy(
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    /**
     * Add achievement to Outbox for Firebase sync.
     */
    private suspend fun enqueueForSync(userId: String, achievementId: String, unlockedAt: Long) {
        val payloadMap = mapOf(
            "userId" to userId,
            "achievementId" to achievementId,
            "unlockedAt" to unlockedAt,
            "collection" to COLLECTION_ACHIEVEMENTS
        )
        
        val payloadJson = gson.toJsonTree(payloadMap).asJsonObject
        
        val outboxEntity = OutboxEntity(
            operationType = OutboxOperationType.LOG_EMOTIONS, // Reutilizando tipo existente
            payload = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        
        outboxDao.insertOperation(outboxEntity)
        Log.d(TAG, "Achievement $achievementId enqueued for sync")
    }
    
    /**
     * Clear all achievements for a user (e.g., on logout).
     */
    suspend fun clearUserAchievements(userId: String) {
        achievementDao.deleteUserAchievements(userId)
    }
}

