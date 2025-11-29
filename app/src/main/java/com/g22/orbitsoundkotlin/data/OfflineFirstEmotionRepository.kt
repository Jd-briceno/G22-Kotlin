package com.g22.orbitsoundkotlin.data

import android.content.Context
import android.util.Log
import androidx.work.*
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.EmotionEntryData
import com.g22.orbitsoundkotlin.data.local.entities.EmotionLogEntity
import com.g22.orbitsoundkotlin.data.workers.EmotionSyncWorker
import com.g22.orbitsoundkotlin.models.EmotionLog
import com.g22.orbitsoundkotlin.utils.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

/**
 * Implementación del EmotionRepository con estrategia de eventual connectivity.
 *
 * Estrategia:
 * 1. Write locally immediately (optimistic/local-first)
 * 2. Persist to local store (Room/SQLite)
 * 3. Enqueue for background sync (WorkManager)
 * 4. Retry/sync to server when network returns
 * 5. Notify user when synced
 */
class OfflineFirstEmotionRepository(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EmotionRepository {

    private val db = AppDatabase.getInstance(context)
    private val emotionLogDao = db.emotionLogDao()
    private val networkMonitor = NetworkMonitor(context)

    companion object {
        private const val TAG = "OfflineFirstEmotionRepo"
        private const val USERS_COLLECTION = "users"
        private const val EMOTION_LOGS_COLLECTION = "emotionLogs"
        private const val SYNC_WORK_NAME = "emotion_sync_work"
    }

    /**
     * Log emotions with local-first strategy.
     * Always succeeds locally, then syncs in background.
     */
    override suspend fun logEmotions(userId: String, emotions: EmotionLog): Result<Unit> {
        Log.d(TAG, "Logging emotions locally for user: $userId")

        if (userId.isBlank()) {
            Log.e(TAG, "User ID is blank, cannot save emotions")
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }

        val isOnline = networkMonitor.isConnected()
        Log.d(TAG, "Network status: ${if (isOnline) "ONLINE" else "OFFLINE"}")

        return try {
            // 1. Write locally immediately (local-first)
            val emotionLogEntity = EmotionLogEntity(
                userId = userId,
                timestamp = emotions.timestamp.seconds * 1000,
                emotions = emotions.emotions.map { emotion ->
                    EmotionEntryData(
                        id = emotion.id,
                        name = emotion.name,
                        source = emotion.source
                    )
                },
                synced = false
            )

            // 2. Persist to local store (Room)
            val insertedId = emotionLogDao.insertEmotionLog(emotionLogEntity)
            Log.d(TAG, "Successfully saved emotions locally with ID: $insertedId")

            // 3. Enqueue for background sync (WorkManager)
            scheduleEmotionSync()

            // Intentar sincronización inmediata optimista si hay conexión
            if (isOnline) {
                // Launch in separate coroutine to avoid blocking
                CoroutineScope(Dispatchers.IO).launch {
                    val savedEntity = emotionLogEntity.copy(id = insertedId)
                    tryImmediateSync(savedEntity, userId)
                }
            }

            // Siempre retornamos éxito localmente inmediatamente
            // La información de si está online se puede verificar con networkMonitor
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save emotions locally", e)
            Result.failure(e)
        }
    }

    /**
     * Get emotion logs from local database.
     * Returns Flow that emits both synced and unsynced logs.
     */
    override fun getEmotionLogs(userId: String): Flow<List<EmotionLog>> {
        return emotionLogDao.getEmotionLogs(userId).map { entities ->
            entities.map { entity ->
                EmotionLog(
                    userId = entity.userId,
                    timestamp = Timestamp(entity.timestamp / 1000, 0),
                    emotions = entity.emotions.map { emotion ->
                        EmotionLog.EmotionEntry(
                            id = emotion.id,
                            name = emotion.name,
                            source = emotion.source
                        )
                    }
                )
            }
        }
    }

    /**
     * Get count of unsynced emotion logs as a Flow.
     */
    fun getUnsyncedLogsCount(): Flow<Int> {
        return emotionLogDao.countUnsyncedLogsFlow()
    }

    /**
     * Check if network is currently available.
     */
    fun isNetworkAvailable(): Boolean {
        return networkMonitor.isConnected()
    }

    /**
     * Schedule background sync using WorkManager with constraints.
     */
    private fun scheduleEmotionSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<EmotionSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )

        Log.d(TAG, "Scheduled emotion sync work")
    }

    /**
     * Try immediate sync if network is available (optimistic sync).
     * Non-blocking - failures are handled by background worker.
     */
    private suspend fun tryImmediateSync(emotionLog: EmotionLogEntity, userId: String) {
        try {
            // Add timeout to prevent blocking when offline
            withTimeout(5000L) { // 5 second timeout
                val emotionsMap = hashMapOf(
                    "timestamp" to Timestamp(emotionLog.timestamp / 1000, 0),
                    "emotions" to emotionLog.emotions.map { emotion ->
                        hashMapOf(
                            "id" to emotion.id,
                            "name" to emotion.name,
                            "source" to emotion.source
                        )
                    }
                )

                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EMOTION_LOGS_COLLECTION)
                    .document()
                    .set(emotionsMap)
                    .await()

                // Marcar como sincronizado si tiene éxito
                emotionLogDao.markAsSynced(emotionLog.id)
                Log.d(TAG, "Immediate sync successful for emotion log ${emotionLog.id}")
            }
        } catch (e: TimeoutCancellationException) {
            // Timeout - background worker will retry
            Log.w(TAG, "Immediate sync timeout, will retry in background")
        } catch (e: Exception) {
            // Log error but don't fail - background worker will retry
            Log.w(TAG, "Immediate sync failed, will retry in background", e)
        }
    }
}

