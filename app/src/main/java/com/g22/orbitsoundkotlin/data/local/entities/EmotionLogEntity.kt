package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entidad local para almacenar emotion logs con patrón eventual connectivity.
 * Permite que la app funcione offline y sincronice cuando hay conexión.
 */
@Entity(tableName = "emotion_logs")
@TypeConverters(EmotionListConverter::class)
data class EmotionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val timestamp: Long, // Timestamp en milisegundos
    val emotions: List<EmotionEntryData>, // Lista de emociones en este log
    val synced: Boolean = false, // Si ya fue sincronizado con Firestore
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Datos de una emoción individual dentro de un log.
 */
data class EmotionEntryData(
    val id: String,
    val name: String,
    val source: String // "manual" o "camera"
)

/**
 * TypeConverter para almacenar la lista de emociones en Room.
 */
class EmotionListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromEmotionList(emotions: List<EmotionEntryData>): String {
        return gson.toJson(emotions)
    }

    @TypeConverter
    fun toEmotionList(emotionsJson: String): List<EmotionEntryData> {
        val listType = object : TypeToken<List<EmotionEntryData>>() {}.type
        return gson.fromJson(emotionsJson, listType)
    }
}

