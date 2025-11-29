package com.g22.orbitsoundkotlin.services

import android.content.Context
import android.util.Log
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.AchievementDefinitions
import com.g22.orbitsoundkotlin.data.repositories.AchievementRepository
import com.g22.orbitsoundkotlin.utils.AchievementNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service for managing achievement unlocks and notifications.
 * Singleton pattern for easy access throughout the app.
 */
class AchievementService private constructor(
    private val repository: AchievementRepository,
    private val notificationHelper: AchievementNotificationHelper,
    context: Context
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val TAG = "AchievementService"
        
        @Volatile
        private var INSTANCE: AchievementService? = null
        
        fun getInstance(context: Context, repository: AchievementRepository): AchievementService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AchievementService(
                    repository = repository,
                    notificationHelper = AchievementNotificationHelper.getInstance(context),
                    context = context.applicationContext
                ).also { INSTANCE = it }
            }
        }
        
        /**
         * Initialize singleton. Should be called in Application or MainActivity.
         */
        fun initialize(context: Context, repository: AchievementRepository) {
            getInstance(context, repository)
        }
    }
    
    /**
     * Check and unlock "First Login" achievement.
     * Triggered on user's first successful login.
     */
    fun checkFirstLogin(userId: String) {
        Log.d(TAG, "checkFirstLogin called for user: $userId")
        serviceScope.launch {
            try {
                val achievement = repository.checkAndUnlock(userId, AchievementDefinitions.ID_FIRST_LOGIN)
                if (achievement != null) {
                    Log.d(TAG, "✅ First Login achievement UNLOCKED for user $userId")
                    notificationHelper.showAchievementUnlocked(achievement, userId)
                    MusicAnalytics.trackAchievementUnlocked(achievement.id, achievement.name)
                } else {
                    Log.d(TAG, "First Login already unlocked for user $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking First Login achievement", e)
            }
        }
    }
    
    /**
     * Check and unlock "First Like" achievement.
     * Triggered when user likes their first track.
     */
    fun checkFirstLike(userId: String) {
        serviceScope.launch {
            try {
                val achievement = repository.checkAndUnlock(userId, AchievementDefinitions.ID_FIRST_LIKE)
                if (achievement != null) {
                    Log.d(TAG, "First Like achievement unlocked for user $userId")
                    notificationHelper.showAchievementUnlocked(achievement, userId)
                    MusicAnalytics.trackAchievementUnlocked(achievement.id, achievement.name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking First Like achievement", e)
            }
        }
    }
    
    /**
     * Check and unlock "Emotion Explorer" achievement.
     * Triggered when user adds their first emotion.
     */
    fun checkEmotionExplorer(userId: String) {
        Log.d(TAG, "checkEmotionExplorer called for user: $userId")
        serviceScope.launch {
            try {
                val achievement = repository.checkAndUnlock(userId, AchievementDefinitions.ID_EMOTION_EXPLORER)
                if (achievement != null) {
                    Log.d(TAG, "✅ Emotion Explorer achievement UNLOCKED for user $userId")
                    notificationHelper.showAchievementUnlocked(achievement, userId)
                    MusicAnalytics.trackAchievementUnlocked(achievement.id, achievement.name)
                } else {
                    Log.d(TAG, "Emotion Explorer already unlocked for user $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking Emotion Explorer achievement", e)
            }
        }
    }
    
    /**
     * Check and unlock "AI Maestro" achievement.
     * Triggered when user generates their first Ares playlist.
     */
    fun checkAIMaestro(userId: String) {
        Log.d(TAG, "checkAIMaestro called for user: $userId")
        serviceScope.launch {
            try {
                val achievement = repository.checkAndUnlock(userId, AchievementDefinitions.ID_AI_MAESTRO)
                if (achievement != null) {
                    Log.d(TAG, "✅ AI Maestro achievement UNLOCKED for user $userId")
                    notificationHelper.showAchievementUnlocked(achievement, userId)
                    MusicAnalytics.trackAchievementUnlocked(achievement.id, achievement.name)
                } else {
                    Log.d(TAG, "AI Maestro already unlocked for user $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking AI Maestro achievement", e)
            }
        }
    }
    
    /**
     * Generic unlock method for future achievements.
     */
    private suspend fun unlockAndNotify(userId: String, achievementId: String) {
        val achievement = repository.checkAndUnlock(userId, achievementId)
        if (achievement != null) {
            Log.d(TAG, "Achievement $achievementId unlocked for user $userId")
            notificationHelper.showAchievementUnlocked(achievement, userId)
            MusicAnalytics.trackAchievementUnlocked(achievement.id, achievement.name)
        }
    }
}

