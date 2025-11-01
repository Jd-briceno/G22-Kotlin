package com.g22.orbitsoundkotlin.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker que sincroniza telemetría de login a Firestore.
 * Ejecuta cada 5 minutos cuando hay red.
 */
class TelemetrySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TELEMETRY_COLLECTION = "login_telemetry"
    }

    override suspend fun doWork(): Result {
        return try {
            val unsyncedTelemetry = db.telemetryDao().getUnsyncedTelemetry()

            if (unsyncedTelemetry.isEmpty()) {
                return Result.success()
            }

            val batch = firestore.batch()
            val syncedIds = mutableListOf<Long>()

            unsyncedTelemetry.forEach { entity ->
                val docRef = firestore.collection(TELEMETRY_COLLECTION).document()
                val data = mapOf(
                    "email" to entity.email,
                    "loginType" to entity.loginType.name,
                    "success" to entity.success,
                    "timestamp" to com.google.firebase.Timestamp(entity.timestamp / 1000, 0),
                    "errorMessage" to (entity.errorMessage ?: "")
                )
                batch.set(docRef, data)
                syncedIds.add(entity.id)
            }

            batch.commit().await()

            // Mark as synced
            db.telemetryDao().markAsSynced(syncedIds)

            Result.success()
        } catch (e: Exception) {
            // Increment retry attempts for failed items
            val unsyncedTelemetry = db.telemetryDao().getUnsyncedTelemetry()
            unsyncedTelemetry.forEach { entity ->
                if (entity.syncAttempts < 3) { // Max 3 attempts
                    db.telemetryDao().incrementSyncAttempts(entity.id, e.message)
                }
            }

            Result.retry()
        }
    }
}

