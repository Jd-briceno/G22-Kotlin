package com.g22.orbitsoundkotlin.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.OutboxOperationType
import com.g22.orbitsoundkotlin.data.local.entities.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.JsonObject
import kotlinx.coroutines.tasks.await

/**
 * Worker general que procesa todas las operaciones del Outbox.
 * Ejecuta cada 15 minutos cuando hay red.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val MAX_BATCH_SIZE = 50
    }

    override suspend fun doWork(): Result {
        return try {
            val unsyncedOps = db.outboxDao().getUnsyncedOperations(MAX_BATCH_SIZE)

            if (unsyncedOps.isEmpty()) {
                return Result.success()
            }

            val batch = firestore.batch()
            val syncedIds = mutableListOf<Long>()
            val currentUser = auth.currentUser

            unsyncedOps.forEach { operation ->
                try {
                    when (operation.operationType) {
                        OutboxOperationType.UPSERT_PROFILE -> {
                            if (currentUser != null) {
                                syncProfile(batch, operation, currentUser.uid)
                            }
                        }
                        OutboxOperationType.UPSERT_INTERESTS -> {
                            if (currentUser != null) {
                                syncInterests(batch, operation, currentUser.uid)
                            }
                        }
                        OutboxOperationType.QUICK_ACTION_LIKE,
                        OutboxOperationType.UPDATE_MOOD -> {
                            if (currentUser != null) {
                                syncQuickAction(batch, operation, currentUser.uid)
                            }
                        }
                        OutboxOperationType.LOG_EMOTIONS -> {
                            if (currentUser != null) {
                                syncEmotionLog(batch, operation, currentUser.uid)
                            }
                        }
                        OutboxOperationType.SEND_TELEMETRY -> {
                            // Ya manejado por TelemetrySyncWorker
                        }
                    }
                    syncedIds.add(operation.id)
                } catch (_: Exception) {
                    // Incrementar intentos
                    db.outboxDao().incrementSyncAttempts(operation.id, "Sync failed")
                }
            }

            batch.commit().await()
            db.outboxDao().markAsSynced(syncedIds)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncProfile(
        batch: com.google.firebase.firestore.WriteBatch,
        operation: com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity,
        uid: String
    ) {
        val data = operation.payload.toFirestoreMap()
        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())

        // Actualizar estado en Room
        val users = db.userDao().getPendingSyncUsers()
        users.filter { it.email == data["email"] as? String }.forEach { user ->
            db.userDao().updateUser(
                user.copy(
                    syncStatus = SyncStatus.SYNCED,
                    lastSyncAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun syncInterests(
        batch: com.google.firebase.firestore.WriteBatch,
        operation: com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity,
        uid: String
    ) {
        val data = operation.payload.toFirestoreMap()
        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        batch.set(docRef, data, com.google.firebase.firestore.SetOptions.merge())
    }

    private fun syncQuickAction(
        batch: com.google.firebase.firestore.WriteBatch,
        operation: com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity,
        uid: String
    ) {
        val data = operation.payload.toFirestoreMap()
        val timestamp = com.google.firebase.Timestamp.now()
        val docRef = firestore.collection("user_actions").document(uid)
        batch.set(docRef, data + ("timestamp" to timestamp), com.google.firebase.firestore.SetOptions.merge())
    }

    private fun syncEmotionLog(
        batch: com.google.firebase.firestore.WriteBatch,
        operation: com.g22.orbitsoundkotlin.data.local.entities.OutboxEntity,
        uid: String
    ) {
        val data = operation.payload.toFirestoreMap()
        val timestamp = com.google.firebase.Timestamp.now()
        val docRef = firestore.collection("emotion_logs").document()
        batch.set(docRef, data + mapOf(
            "userId" to uid,
            "timestamp" to timestamp
        ))
    }

    private fun JsonObject.toFirestoreMap(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            this@toFirestoreMap.entrySet().forEach { entry ->
                val value = when {
                    entry.value.isJsonPrimitive -> {
                        val prim = entry.value.asJsonPrimitive
                        when {
                            prim.isBoolean -> prim.asBoolean
                            prim.isNumber -> prim.asNumber.toDouble()
                            else -> prim.asString
                        }
                    }
                    entry.value.isJsonArray -> entry.value.asJsonArray.map { it.asString }
                    else -> entry.value.toString()
                }
                put(entry.key, value)
            }
        }
    }
}

