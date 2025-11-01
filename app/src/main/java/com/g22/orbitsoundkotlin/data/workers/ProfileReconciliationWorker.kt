package com.g22.orbitsoundkotlin.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Worker que reconcilia UIDs de usuarios post-signup.
 * Actualiza UID local con el remoto de Firebase Auth.
 */
class ProfileReconciliationWorker(
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
            val usersNeedingReconciliation = db.userDao().getUsersNeedingReconciliation()

            if (usersNeedingReconciliation.isEmpty()) {
                return Result.success()
            }

            // Por ahora, solo reconciliamos el usuario autenticado actual
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure()
            }

            // Buscar en Room por localUid o email
            usersNeedingReconciliation.forEach { localUser ->
                if (localUser.email == currentUser.email) {
                    // Encontramos match, actualizar UID
                    val updatedUser = localUser.copy(
                        uid = currentUser.uid,
                        localUid = null,
                        syncStatus = SyncStatus.SYNCED,
                        lastSyncAt = System.currentTimeMillis()
                    )

                    db.userDao().updateUser(updatedUser)

                    // Actualizar o crear en Firestore
                    val userData = mapOf(
                        "email" to localUser.email,
                        "name" to localUser.name,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                    firestore.collection(USERS_COLLECTION)
                        .document(currentUser.uid)
                        .set(userData, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

