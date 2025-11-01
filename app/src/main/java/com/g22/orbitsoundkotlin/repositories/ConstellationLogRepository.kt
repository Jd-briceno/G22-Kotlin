package com.g22.orbitsoundkotlin.repositories

import android.util.Log
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.models.ConstellationLog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing constellation selection logs in Firebase
 */
class ConstellationLogRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ConstellationLogRepo"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_CONSTELLATION_LOGS = "constellationLogs"
    }

    /**
     * Save a constellation selection log to Firebase
     * @param constellation The selected constellation
     * @return true if successful, false otherwise
     */
    suspend fun logConstellationSelection(constellation: Constellation): Boolean {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "No user logged in, cannot save constellation log")
                return false
            }

            val log = ConstellationLog(
                constellationId = constellation.id,
                constellationTitle = constellation.title,
                timestamp = Timestamp.now(),
                userId = userId
            )

            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CONSTELLATION_LOGS)
                .add(log.toMap())
                .await()

            Log.d(TAG, "Successfully logged constellation selection: ${constellation.title}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error logging constellation selection", e)
            false
        }
    }

    /**
     * Get all constellation logs for the current user
     */
    suspend fun getUserConstellationLogs(): List<ConstellationLog> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyList()

            val snapshot = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(COLLECTION_CONSTELLATION_LOGS)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    ConstellationLog(
                        constellationId = doc.getString("constellationId") ?: "",
                        constellationTitle = doc.getString("constellationTitle") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        userId = doc.getString("userId") ?: ""
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing constellation log", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching constellation logs", e)
            emptyList()
        }
    }
}

