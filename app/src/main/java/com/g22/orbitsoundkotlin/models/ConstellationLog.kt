package com.g22.orbitsoundkotlin.models

import com.google.firebase.Timestamp

/**
 * Data model for constellation selection logs
 */
data class ConstellationLog(
    val constellationId: String = "",
    val constellationTitle: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val userId: String = ""
) {
    /**
     * Convert to Map for Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "constellationId" to constellationId,
            "constellationTitle" to constellationTitle,
            "timestamp" to timestamp,
            "userId" to userId
        )
    }
}

