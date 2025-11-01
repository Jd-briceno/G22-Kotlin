package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.WeatherCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE uid = :uid AND expiresAt > :currentTime")
    suspend fun getValidCacheByUid(
        uid: String,
        currentTime: Long = System.currentTimeMillis()
    ): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache WHERE uid = :uid")
    suspend fun getCacheByUid(uid: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache WHERE uid = :uid")
    fun getCacheByUidFlow(uid: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache WHERE expiresAt <= :currentTime")
    suspend fun getExpiredCaches(currentTime: Long = System.currentTimeMillis()): List<WeatherCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: WeatherCacheEntity)

    @Update
    suspend fun updateCache(cache: WeatherCacheEntity)

    @Delete
    suspend fun deleteCache(cache: WeatherCacheEntity)

    @Query("DELETE FROM weather_cache WHERE expiresAt <= :currentTime")
    suspend fun deleteExpiredCaches(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM weather_cache")
    suspend fun clearAllCache()
}

