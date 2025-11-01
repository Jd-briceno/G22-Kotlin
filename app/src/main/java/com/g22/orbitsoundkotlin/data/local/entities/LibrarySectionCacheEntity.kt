package com.g22.orbitsoundkotlin.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

/**
 * Cache entity for Library playlist sections.
 * Stores a section's tracks as JSON for simple persistence.
 * 
 * Each section represents one of the 4 personalized playlist groups shown in LibraryScreen.
 * TTL (Time To Live) is 15 minutes - data is considered stale after this period.
 */
@Entity(tableName = "library_section_cache")
data class LibrarySectionCacheEntity(
    @PrimaryKey
    val sectionKey: String, // Composite key: "userId_sectionNumber" e.g. "user123_1"
    val sectionTitle: String, // Title of the section e.g. "Starlight Mix"
    @TypeConverters(JsonConverter::class)
    val tracksJson: String, // JSON array of Track objects
    val cachedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
) {
    companion object {
        const val CACHE_TTL_MS = 15 * 60 * 1000L // 15 minutes
        
        /**
         * Generates a section key for storage.
         * @param userId User identifier
         * @param sectionNumber Section number (1-4)
         */
        fun generateKey(userId: String, sectionNumber: Int): String {
            return "${userId}_$sectionNumber"
        }
        
        /**
         * Calculates expiration timestamp based on TTL.
         */
        fun calculateExpiresAt(): Long {
            return System.currentTimeMillis() + CACHE_TTL_MS
        }
    }
    
    /**
     * Checks if cached data has expired.
     * @return true if data is older than TTL
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
}

