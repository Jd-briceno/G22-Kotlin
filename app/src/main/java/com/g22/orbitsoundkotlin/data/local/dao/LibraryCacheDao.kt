package com.g22.orbitsoundkotlin.data.local.dao

import androidx.room.*
import com.g22.orbitsoundkotlin.data.local.entities.LibrarySectionCacheEntity
import com.g22.orbitsoundkotlin.data.local.entities.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Library cache operations.
 * Handles both section caching and search history persistence.
 */
@Dao
interface LibraryCacheDao {
    
    // ==================== Section Cache ====================
    
    /**
     * Gets cached section by key.
     * @param key Section key in format "userId_sectionNumber"
     * @return Cached section or null if not found
     */
    @Query("SELECT * FROM library_section_cache WHERE sectionKey = :key")
    suspend fun getSectionCache(key: String): LibrarySectionCacheEntity?
    
    /**
     * Gets all cached sections for a user.
     * @param userId User identifier
     * @return List of cached sections (up to 4)
     */
    @Query("SELECT * FROM library_section_cache WHERE sectionKey LIKE :userId || '_%'")
    suspend fun getAllUserSections(userId: String): List<LibrarySectionCacheEntity>
    
    /**
     * Inserts or replaces a section cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionCache(cache: LibrarySectionCacheEntity)
    
    /**
     * Inserts or replaces multiple section caches (batch operation).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSectionCaches(caches: List<LibrarySectionCacheEntity>)
    
    /**
     * Deletes expired section caches.
     * Should be called periodically to free storage.
     */
    @Query("DELETE FROM library_section_cache WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredSections(currentTime: Long = System.currentTimeMillis())
    
    /**
     * Deletes all section caches for a specific user.
     * Useful for cache invalidation on logout or data refresh.
     */
    @Query("DELETE FROM library_section_cache WHERE sectionKey LIKE :userId || '_%'")
    suspend fun deleteUserSections(userId: String)
    
    // ==================== Search History ====================
    
    /**
     * Gets recent searches for a user as Flow (reactive).
     * Results are ordered by timestamp descending (newest first).
     * 
     * @param uid User identifier
     * @param limit Maximum number of searches to return
     * @return Flow of search history entries
     */
    @Query("SELECT * FROM search_history WHERE userId = :uid ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearchesFlow(uid: String, limit: Int = 10): Flow<List<SearchHistoryEntity>>
    
    /**
     * Gets recent searches for a user synchronously.
     */
    @Query("SELECT * FROM search_history WHERE userId = :uid ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentSearches(uid: String, limit: Int = 10): List<SearchHistoryEntity>
    
    /**
     * Inserts a search history entry.
     * Duplicates are allowed to track search frequency.
     */
    @Insert
    suspend fun insertSearch(search: SearchHistoryEntity)
    
    /**
     * Clears all search history for a specific user.
     * Useful for privacy/settings feature.
     */
    @Query("DELETE FROM search_history WHERE userId = :uid")
    suspend fun clearSearchHistory(uid: String)
    
    /**
     * Deletes old search entries (older than specified timestamp).
     * Useful for automatic cleanup of very old searches.
     * 
     * @param cutoffTime Timestamp - searches older than this will be deleted
     */
    @Query("DELETE FROM search_history WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSearches(cutoffTime: Long)
    
    /**
     * Counts total search entries for a user.
     * Useful for analytics or storage management.
     */
    @Query("SELECT COUNT(*) FROM search_history WHERE userId = :uid")
    suspend fun getSearchCount(uid: String): Int
}

