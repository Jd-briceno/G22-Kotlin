package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.EmotionLog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine

class FirestoreEmotionRepository(
    private val firestore: FirebaseFirestore
) : EmotionRepository {

    private val collectionName = "desired_emotion_logs"

    override suspend fun logEmotions(emotionLog: EmotionLog): Result<Unit> {
        return try {
            val doc = firestore.collection(collectionName).document()
            val payload = hashMapOf<String, Any?>(
                "emotions" to emotionLog.emotions,
                "clientTs" to emotionLog.clientTs,
                "serverTs" to FieldValue.serverTimestamp(),
                "meta" to emotionLog.meta
            )

            suspendCancellableCoroutine { cont ->
                doc.set(payload)
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) {} }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) {} }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
