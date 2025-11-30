package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getUserAchievements(userId: String): List<AchievementEntity>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getUserAchievementsFlow(userId: String): Flow<List<AchievementEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)
    
    @Query("SELECT EXISTS(SELECT 1 FROM achievements WHERE userId = :userId AND achievementId = :achievementId)")
    suspend fun isAchievementUnlocked(userId: String, achievementId: String): Boolean
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND achievementId = :achievementId")
    suspend fun getAchievement(userId: String, achievementId: String): AchievementEntity?
    
    @Query("DELETE FROM achievements WHERE userId = :userId")
    suspend fun deleteUserAchievements(userId: String)
    
    @Query("DELETE FROM achievements")
    suspend fun clearAll()
}

