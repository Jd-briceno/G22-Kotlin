package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Intereses del usuario con versionado para detección de conflictos.
 * Usa conflict resolution con Last-Write-Wins.
 */
@Entity(
    tableName = "user_interests",
    indices = []
)
@TypeConverters(StringListConverter::class)
data class UserInterestsEntity(
    @PrimaryKey
    val uid: String,
    val interests: List<String>,
    val version: Int,
    val lastModified: Long = System.currentTimeMillis(),
    val serverTimestamp: Long? = null, // Timestamp del servidor (Firestore)
    val needsSync: Boolean = false
)

/**
 * TypeConverter para almacenar List<String> como JSON en Room.
 */
class StringListConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return gson.fromJson(value, type)
    }
}

