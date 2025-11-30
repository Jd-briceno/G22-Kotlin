package com.g22.orbitsoundkotlin.data.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.UserDailyActivitySummaryEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker que sincroniza resúmenes diarios de actividad a Firestore.
 * 
 * EVENTUAL CONNECTIVITY:
 * - Lee summaries con isSynced = false
 * - Sube a Firestore (colección: user_activity_stats)
 * - Marca como sincronizados si el upload es exitoso
 * - Si falla (sin conexión, error de Firestore), se reintenta en próxima ejecución
 * 
 * INFRAESTRUCTURA PARA BQ:
 * - Los summaries subidos a Firestore pueden ser usados por un dashboard futuro
 * - Formato del documento: userId/date -> summary data
 * 
 * SEGURIDAD: Solo sube métricas agregadas, nunca datos crudos sensibles
 */
class ActivityStatsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ActivityStatsSyncWorker"
        private const val COLLECTION_NAME = "user_activity_stats"
        private const val MAX_BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, skipping sync")
                return Result.success()
            }

            val userId = currentUser.uid
            val unsyncedSummaries = db.userDailyActivitySummaryDao().getUnsyncedSummaries(userId)

            if (unsyncedSummaries.isEmpty()) {
                Log.d(TAG, "No unsynced summaries to sync")
                return Result.success()
            }

            Log.d(TAG, "Syncing ${unsyncedSummaries.size} summaries for user $userId")

            // Procesar en batches para evitar límites de Firestore
            val batches = unsyncedSummaries.chunked(MAX_BATCH_SIZE)
            val syncedIds = mutableListOf<Long>()

            batches.forEach { batch ->
                val firestoreBatch = firestore.batch()

                batch.forEach { summary ->
                    try {
                        // Crear documento: user_activity_stats/{userId}/summaries/{date}
                        val docRef = firestore
                            .collection(COLLECTION_NAME)
                            .document(userId)
                            .collection("summaries")
                            .document(summary.date)

                        val data = mapOf(
                            "userId" to summary.userId,
                            "date" to summary.date,
                            "sessionsCount" to summary.sessionsCount,
                            "totalTimeMinutes" to summary.totalTimeMinutes,
                            "mostCommonAction" to summary.mostCommonAction,
                            "lastUpdatedAt" to com.google.firebase.Timestamp(
                                summary.lastUpdatedAt / 1000,
                                0
                            ),
                            "syncedAt" to com.google.firebase.Timestamp.now()
                        )

                        firestoreBatch.set(docRef, data)
                        syncedIds.add(summary.id)

                        Log.d(TAG, "Queued summary for date ${summary.date}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error preparing summary ${summary.id} for sync", e)
                    }
                }

                // Commit batch
                firestoreBatch.commit().await()
                Log.d(TAG, "Committed batch of ${batch.size} summaries")
            }

            // Marcar como sincronizados
            if (syncedIds.isNotEmpty()) {
                db.userDailyActivitySummaryDao().markAsSynced(syncedIds)
                Log.d(TAG, "✅ Marked ${syncedIds.size} summaries as synced")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error syncing activity stats", e)
            // Retry en próxima ejecución si hay error de red o Firestore
            Result.retry()
        }
    }
}



