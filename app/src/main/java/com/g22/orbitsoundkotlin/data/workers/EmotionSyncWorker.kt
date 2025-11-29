package com.g22.orbitsoundkotlin.data.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.EmotionLogEntity
import com.g22.orbitsoundkotlin.utils.SyncEventManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker específico para sincronizar emotion logs con Firestore.
 * Se ejecuta cuando hay conexión a internet disponible.
 */
class EmotionSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "EmotionSyncWorker"
        private const val USERS_COLLECTION = "users"
        private const val EMOTION_LOGS_COLLECTION = "emotionLogs"
        private const val NOTIFICATION_CHANNEL_ID = "emotion_sync_channel"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting emotion sync worker")

        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "No authenticated user, skipping sync")
                return Result.success()
            }

            val unsyncedLogs = db.emotionLogDao().getUnsyncedEmotionLogs()

            if (unsyncedLogs.isEmpty()) {
                Log.d(TAG, "No unsynced emotion logs to process")
                return Result.success()
            }

            Log.d(TAG, "Found ${unsyncedLogs.size} unsynced emotion logs")

            var successCount = 0
            var failureCount = 0
            val syncedIds = mutableListOf<Long>()

            unsyncedLogs.forEach { emotionLog ->
                try {
                    // Verificar si no hemos excedido los intentos máximos
                    if (emotionLog.syncAttempts >= MAX_RETRY_ATTEMPTS) {
                        Log.w(TAG, "Max retry attempts reached for emotion log ${emotionLog.id}")
                        failureCount++
                        return@forEach
                    }

                    // Sincronizar con Firestore
                    syncEmotionLogToFirestore(emotionLog, currentUser.uid)

                    // Marcar como sincronizado
                    db.emotionLogDao().markAsSynced(emotionLog.id)
                    syncedIds.add(emotionLog.id)
                    successCount++

                    Log.d(TAG, "Successfully synced emotion log ${emotionLog.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync emotion log ${emotionLog.id}", e)
                    db.emotionLogDao().incrementSyncAttempts(emotionLog.id, e.message)
                    failureCount++
                }
            }

            Log.d(TAG, "Sync completed. Success: $successCount, Failures: $failureCount")

            // Mostrar notificación si se sincronizaron logs
            if (successCount > 0) {
                showSyncNotification(successCount)

                // Emitir evento de sincronización exitosa para que el ViewModel pueda mostrar toast
                SyncEventManager.emitEmotionSyncSuccess(successCount)

                // Guardar el tiempo de sincronización
                SyncEventManager.saveLastSyncTime(context, successCount)
            }

            // Limpiar logs antiguos ya sincronizados (más de 30 días)
            val cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            db.emotionLogDao().deleteOldSyncedLogs(cutoffTime)

            // Si hubo fallos, reintentar
            if (failureCount > 0 && successCount > 0) {
                Result.retry()
            } else if (failureCount > 0) {
                Result.failure()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in emotion sync worker", e)
            Result.retry()
        }
    }

    private suspend fun syncEmotionLogToFirestore(emotionLog: EmotionLogEntity, userId: String) {
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
    }

    private fun showSyncNotification(count: Int) {
        createNotificationChannel()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Emociones sincronizadas")
            .setContentText("$count registro(s) de emociones se han guardado en la nube")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Sincronización de Emociones",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones sobre la sincronización de registros de emociones"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

