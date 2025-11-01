package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid")
    fun getUserByIdFlow(uid: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUserById(uid: String)

    @Transaction
    @Query("SELECT * FROM users WHERE localUid IS NOT NULL")
    suspend fun getUsersNeedingReconciliation(): List<UserEntity>
}

