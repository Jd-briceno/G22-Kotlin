package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE uid = :uid")
    suspend fun getSessionByUid(uid: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE uid = :uid")
    fun getSessionByUidFlow(uid: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSession(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE expiresAt > 0 AND expiresAt < :currentTime")
    suspend fun getExpiredSessions(currentTime: Long = System.currentTimeMillis()): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity)

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Delete
    suspend fun deleteSession(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE uid = :uid")
    suspend fun deleteSessionByUid(uid: String)

    @Query("DELETE FROM sessions")
    suspend fun clearAllSessions()
}

