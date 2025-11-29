package com.g22.orbitsoundkotlin.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Administrador de eventos de sincronización que permite comunicación
 * entre el WorkManager (EmotionSyncWorker) y los ViewModels.
 */
object SyncEventManager {
    private const val PREFS_NAME = "sync_events"
    private const val KEY_LAST_SYNC_TIME = "last_emotion_sync_time"
    private const val KEY_LAST_SYNC_COUNT = "last_emotion_sync_count"

    private val _emotionSyncEvents = MutableSharedFlow<EmotionSyncEvent>(replay = 0)
    val emotionSyncEvents: SharedFlow<EmotionSyncEvent> = _emotionSyncEvents.asSharedFlow()

    /**
     * Emite un evento de sincronización exitosa de emociones.
     * Llamado desde EmotionSyncWorker.
     */
    suspend fun emitEmotionSyncSuccess(count: Int) {
        _emotionSyncEvents.emit(EmotionSyncEvent.SyncSuccess(count))
    }

    /**
     * Emite un evento de fallo en la sincronización.
     */
    suspend fun emitEmotionSyncFailure(error: String) {
        _emotionSyncEvents.emit(EmotionSyncEvent.SyncFailure(error))
    }

    /**
     * Guarda el último tiempo de sincronización en SharedPreferences.
     * Esto persiste entre reinicios de la app.
     */
    fun saveLastSyncTime(context: Context, count: Int) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis())
            .putInt(KEY_LAST_SYNC_COUNT, count)
            .apply()
    }

    /**
     * Obtiene el tiempo de la última sincronización.
     */
    fun getLastSyncTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    /**
     * Obtiene el conteo de la última sincronización.
     */
    fun getLastSyncCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_SYNC_COUNT, 0)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    sealed class EmotionSyncEvent {
        data class SyncSuccess(val count: Int) : EmotionSyncEvent()
        data class SyncFailure(val error: String) : EmotionSyncEvent()
    }
}

