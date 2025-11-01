package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.UserInterestsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestsDao {
    @Query("SELECT * FROM user_interests WHERE uid = :uid")
    suspend fun getInterestsByUid(uid: String): UserInterestsEntity?

    @Query("SELECT * FROM user_interests WHERE uid = :uid")
    fun getInterestsByUidFlow(uid: String): Flow<UserInterestsEntity?>

    @Query("SELECT * FROM user_interests WHERE needsSync = 1")
    suspend fun getInterestsNeedingSync(): List<UserInterestsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterests(interests: UserInterestsEntity)

    @Update
    suspend fun updateInterests(interests: UserInterestsEntity)

    @Delete
    suspend fun deleteInterests(interests: UserInterestsEntity)

    @Query("DELETE FROM user_interests WHERE uid = :uid")
    suspend fun deleteInterestsByUid(uid: String)

    @Transaction
    @Query("SELECT * FROM user_interests WHERE uid = :uid")
    suspend fun getInterestsForConflictResolution(uid: String): UserInterestsEntity?
}

