package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Patrón Outbox: todas las operaciones pendientes de sincronización.
 * Garantiza que ninguna operación se pierda.
 */
@Entity(
    tableName = "outbox",
    indices = [
        Index(value = ["synced"], unique = false),
        Index(value = ["operationType"], unique = false)
    ]
)
@TypeConverters(JsonConverter::class)
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: OutboxOperationType,
    val payload: JsonObject, // Datos serializados de la operación
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastError: String? = null
)

enum class OutboxOperationType {
    UPSERT_PROFILE,      // Signup: crear/actualizar perfil
    UPSERT_INTERESTS,    // InterestSelection: actualizar intereses
    SEND_TELEMETRY,      // Login: enviar métricas
    QUICK_ACTION_LIKE,   // Home: like/quick action
    UPDATE_MOOD          // Home: cambio de mood
}

/**
 * TypeConverter para almacenar JsonObject en Room.
 */
class JsonConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromJsonObject(value: JsonObject): String {
        return value.toString()
    }

    @TypeConverter
    fun toJsonObject(value: String): JsonObject {
        return JsonParser.parseString(value).asJsonObject
    }
}

