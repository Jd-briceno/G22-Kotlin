package com.g22.orbitsoundkotlin.data.repositories

import com.g22.orbitsoundkotlin.data.local.dao.LibraryCacheDao
import com.g22.orbitsoundkotlin.data.local.entities.LibrarySectionCacheEntity
import com.g22.orbitsoundkotlin.data.local.entities.SearchHistoryEntity
import com.g22.orbitsoundkotlin.models.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Library screen local storage using Room Database.
 * 
 * Provides persistent storage for:
 * - Playlist sections (4 sections with tracks)
 * - Search history
 * 
 * Storage Strategy:
 * - Sections: JSON storage with 15 min TTL
 * - Search history: Persistent until manually cleared
 * 
 * Benefits:
 * - Instant loading from local storage
 * - Offline functionality
 * - Reduced Spotify API calls
 */
class LibraryCacheRepository(
    private val dao: LibraryCacheDao
) {
    private val gson = Gson()
    
    // ==================== Section Cache Operations ====================
    
    /**
     * Caches a playlist section with its tracks.
     * 
     * @param userId User identifier
     * @param sectionNumber Section number (1-4)
     * @param sectionTitle Section display title
     * @param tracks List of tracks in this section
     */
    suspend fun cacheSection(
        userId: String,
        sectionNumber: Int,
        sectionTitle: String,
        tracks: List<Track>
    ) {
        val key = LibrarySectionCacheEntity.generateKey(userId, sectionNumber)
        val entity = LibrarySectionCacheEntity(
            sectionKey = key,
            sectionTitle = sectionTitle,
            tracksJson = gson.toJson(tracks),
            cachedAt = System.currentTimeMillis(),
            expiresAt = LibrarySectionCacheEntity.calculateExpiresAt()
        )
        dao.insertSectionCache(entity)
    }
    
    /**
     * Caches all 4 sections at once (batch operation).
     * More efficient than calling cacheSection 4 times.
     * 
     * @param userId User identifier
     * @param sectionsData Map of sectionNumber to Pair(title, tracks)
     */
    suspend fun cacheAllSections(
        userId: String,
        sectionsData: Map<Int, Pair<String, List<Track>>>
    ) {
        val entities = sectionsData.map { (sectionNumber, data) ->
            val (title, tracks) = data
            val key = LibrarySectionCacheEntity.generateKey(userId, sectionNumber)
            LibrarySectionCacheEntity(
                sectionKey = key,
                sectionTitle = title,
                tracksJson = gson.toJson(tracks),
                cachedAt = System.currentTimeMillis(),
                expiresAt = LibrarySectionCacheEntity.calculateExpiresAt()
            )
        }
        dao.insertSectionCaches(entities)
    }
    
    /**
     * Retrieves a cached section.
     * Returns null if section doesn't exist or has expired.
     * 
     * @param userId User identifier
     * @param sectionNumber Section number (1-4)
     * @return Pair of (sectionTitle, tracks) or null if not found/expired
     */
    suspend fun getSection(
        userId: String,
        sectionNumber: Int
    ): Pair<String, List<Track>>? {
        val key = LibrarySectionCacheEntity.generateKey(userId, sectionNumber)
        val cached = dao.getSectionCache(key)
        
        return if (cached != null && !cached.isExpired()) {
            try {
                val tracksType = object : TypeToken<List<Track>>() {}.type
                val tracks = gson.fromJson<List<Track>>(cached.tracksJson, tracksType)
                Pair(cached.sectionTitle, tracks)
            } catch (e: Exception) {
                // JSON parsing error - return null to trigger refresh
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Retrieves all cached sections for a user.
     * Useful for checking if full cache exists.
     * 
     * @param userId User identifier
     * @return Map of sectionNumber to Pair(title, tracks)
     */
    suspend fun getAllSections(userId: String): Map<Int, Pair<String, List<Track>>> {
        val cached = dao.getAllUserSections(userId)
        val result = mutableMapOf<Int, Pair<String, List<Track>>>()
        
        cached.forEach { entity ->
            if (!entity.isExpired()) {
                try {
                    // Extract section number from key "userId_sectionNumber"
                    val sectionNumber = entity.sectionKey.substringAfterLast("_").toIntOrNull()
                    if (sectionNumber != null) {
                        val tracksType = object : TypeToken<List<Track>>() {}.type
                        val tracks = gson.fromJson<List<Track>>(entity.tracksJson, tracksType)
                        result[sectionNumber] = Pair(entity.sectionTitle, tracks)
                    }
                } catch (e: Exception) {
                    // Skip malformed entries
                }
            }
        }
        
        return result
    }
    
    /**
     * Checks if a section is cached and not expired.
     * 
     * @param userId User identifier
     * @param sectionNumber Section number (1-4)
     * @return true if valid cache exists
     */
    suspend fun isSectionCached(userId: String, sectionNumber: Int): Boolean {
        val key = LibrarySectionCacheEntity.generateKey(userId, sectionNumber)
        val cached = dao.getSectionCache(key)
        return cached != null && !cached.isExpired()
    }
    
    /**
     * Clears all cached sections for a user.
     * Useful for forcing a refresh or on logout.
     */
    suspend fun clearUserCache(userId: String) {
        dao.deleteUserSections(userId)
    }
    
    /**
     * Cleans up expired section caches.
     * Should be called periodically (e.g., on app start).
     */
    suspend fun cleanupExpiredSections() {
        dao.deleteExpiredSections()
    }
    
    // ==================== Search History Operations ====================
    
    /**
     * Saves a search query to history.
     * 
     * @param userId User identifier
     * @param query Search query string
     */
    suspend fun saveSearch(userId: String, query: String) {
        if (query.isBlank()) return
        
        val search = SearchHistoryEntity(
            userId = userId,
            query = query.trim(),
            timestamp = System.currentTimeMillis()
        )
        dao.insertSearch(search)
    }
    
    /**
     * Gets recent searches as Flow for reactive UI.
     * 
     * @param userId User identifier
     * @param limit Maximum number of searches to return
     * @return Flow of search history entries
     */
    fun getRecentSearchesFlow(userId: String, limit: Int = 10): Flow<List<SearchHistoryEntity>> {
        return dao.getRecentSearchesFlow(userId, limit)
    }
    
    /**
     * Gets recent searches synchronously.
     * 
     * @param userId User identifier
     * @param limit Maximum number of searches to return
     * @return List of search history entries
     */
    suspend fun getRecentSearches(userId: String, limit: Int = 10): List<SearchHistoryEntity> {
        return dao.getRecentSearches(userId, limit)
    }
    
    /**
     * Clears all search history for a user.
     * Useful for privacy settings.
     */
    suspend fun clearSearchHistory(userId: String) {
        dao.clearSearchHistory(userId)
    }
    
    /**
     * Cleans up old search entries (older than specified days).
     * 
     * @param daysOld Age threshold in days
     */
    suspend fun cleanupOldSearches(daysOld: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        dao.deleteOldSearches(cutoffTime)
    }
    
    /**
     * Gets search history statistics.
     * 
     * @param userId User identifier
     * @return Number of saved searches
     */
    suspend fun getSearchCount(userId: String): Int {
        return dao.getSearchCount(userId)
    }
}

