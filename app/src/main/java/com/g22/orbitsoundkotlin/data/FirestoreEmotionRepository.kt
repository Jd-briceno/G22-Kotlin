package com.g22.orbitsoundkotlin.data

import android.util.Log
import com.g22.orbitsoundkotlin.models.EmotionLog
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.toEmotionEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreEmotionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : EmotionRepository {

    companion object {
        private const val TAG = "EmotionRepository"
        private const val USERS_COLLECTION = "users"
        private const val EMOTION_LOGS_COLLECTION = "emotionLogs"
    }

    override suspend fun logEmotions(userId: String, emotions: EmotionLog): Result<Unit> {
        Log.d(TAG, "Attempting to log emotions for user: $userId")

        if (userId.isBlank()) {
            Log.e(TAG, "User ID is blank, cannot save emotions")
            return Result.failure(IllegalArgumentException("User ID cannot be blank"))
        }

        return try {
            val emotionsMap = hashMapOf(
                "timestamp" to emotions.timestamp,
                "emotions" to emotions.emotions.map { emotion ->
                    hashMapOf(
                        "id" to emotion.id,
                        "name" to emotion.name,
                        "source" to emotion.source
                    )
                }
            )

            Log.d(TAG, "Saving emotion data: $emotionsMap")

            val documentRef = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EMOTION_LOGS_COLLECTION)
                .document()

            Log.d(TAG, "Writing to path: ${documentRef.path}")

            documentRef.set(emotionsMap).await()

            Log.d(TAG, "Successfully saved emotions to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save emotions", e)
            Result.failure(e)
        }
    }

    override fun getEmotionLogs(userId: String): Flow<List<EmotionLog>> = callbackFlow {
        val listenerRegistration = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EMOTION_LOGS_COLLECTION)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val emotionLogs = snapshot?.documents?.mapNotNull { document ->
                    val timestamp = document.getTimestamp("timestamp") ?: Timestamp.now()
                    val emotions = document.get("emotions") as? List<Map<String, Any>> ?: emptyList()

                    val emotionEntries = emotions.map { emotionMap ->
                        EmotionLog.EmotionEntry(
                            id = emotionMap["id"] as? String ?: "",
                            name = emotionMap["name"] as? String ?: "",
                            source = emotionMap["source"] as? String ?: ""
                        )
                    }

                    EmotionLog(
                        userId = userId,
                        timestamp = timestamp,
                        emotions = emotionEntries
                    )
                } ?: emptyList()

                trySend(emotionLogs)
            }

        awaitClose { listenerRegistration.remove() }
    }

    // Create an EmotionLog from UI models
    fun createEmotionLog(userId: String, emotions: List<EmotionModel>): EmotionLog {
        return EmotionLog(
            userId = userId,
            timestamp = Timestamp.now(),
            emotions = emotions.map { it.toEmotionEntry() }
        )
    }
}