package com.g22.orbitsoundkotlin.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker que sincroniza intereses del usuario con Firestore.
 * Usa conflict resolution Last-Write-Wins basado en timestamp del servidor.
 */
class InterestsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getInstance(context)
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun doWork(): Result {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure()
            val localInterests = db.interestsDao().getInterestsByUid(currentUser.uid)

            if (localInterests == null || !localInterests.needsSync) {
                return Result.success()
            }

            // Obtener intereses del servidor
            val serverDoc = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()

            val serverInterests = serverDoc.get("interests") as? List<String> ?: emptyList()
            val serverTimestamp = serverDoc.getTimestamp("lastModified")?.seconds ?: 0L

            // Conflict resolution: Last-Write-Wins
            val serverWins = serverTimestamp > (localInterests.lastModified / 1000)

            if (serverWins && serverInterests.isNotEmpty()) {
                // Servidor gana: actualizar local con datos del servidor
                val updatedInterests = localInterests.copy(
                    interests = serverInterests,
                    serverTimestamp = serverTimestamp * 1000,
                    needsSync = false,
                    version = localInterests.version + 1
                )
                db.interestsDao().updateInterests(updatedInterests)
            } else {
                // Local gana: enviar al servidor
                val interestsData = mapOf(
                    "interests" to localInterests.interests,
                    "lastModified" to com.google.firebase.Timestamp.now()
                )

                firestore.collection(USERS_COLLECTION)
                    .document(currentUser.uid)
                    .update(interestsData)
                    .await()

                val updatedInterests = localInterests.copy(
                    needsSync = false,
                    serverTimestamp = com.google.firebase.Timestamp.now().seconds * 1000
                )
                db.interestsDao().updateInterests(updatedInterests)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

