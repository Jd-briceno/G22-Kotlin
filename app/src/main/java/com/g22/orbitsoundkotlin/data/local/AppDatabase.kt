package com.g22.orbitsoundkotlin.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.g22.orbitsoundkotlin.data.local.dao.*
import com.g22.orbitsoundkotlin.data.local.entities.*

/**
 * Room Database principal para estrategias de conectividad eventual.
 * Implementa el patrón Outbox y SWR para todas las operaciones.
 * 
 * Version 2: Added Library cache entities (LibrarySectionCacheEntity, SearchHistoryEntity)
 * Version 3: Added EmotionLogEntity for offline emotion logging
 */
@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        LoginTelemetryEntity::class,
        UserInterestsEntity::class,
        WeatherCacheEntity::class,
        OutboxEntity::class,
        // Library cache entities (v2)
        LibrarySectionCacheEntity::class,
        SearchHistoryEntity::class,
        // Emotion logs (v3)
        EmotionLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(StringListConverter::class, JsonConverter::class, EmotionListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun interestsDao(): InterestsDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun outboxDao(): OutboxDao
    abstract fun libraryCacheDao(): LibraryCacheDao
    abstract fun emotionLogDao(): EmotionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbitsounds_database"
                )
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

