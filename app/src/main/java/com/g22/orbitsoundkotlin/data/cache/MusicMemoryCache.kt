package com.g22.orbitsoundkotlin.data.cache

import android.util.LruCache
import com.g22.orbitsoundkotlin.data.MusicRecommendationEngine
import com.g22.orbitsoundkotlin.models.Track

/**
 * In-memory cache manager for music library data using LRU eviction strategy.
 * 
 * Provides three independent LRU caches:
 * - Track cache: 100 items (~500KB)
 * - Section cache: 20 items (~100KB)
 * - Search cache: 10 queries (~250KB) with 5-minute TTL
 * 
 * All caches are thread-safe with automatic eviction when capacity is reached.
 * Data is volatile and cleared on app termination.
 * 
 * Total memory footprint: ~850KB
 * 
 * @see putTrack
 * @see putSection
 * @see putSearchResults
 */
class MusicMemoryCache {
    
    /**
     * LRU cache for individual track objects.
     * Capacity: 100 tracks
     */
    private val trackCache = object : LruCache<String, Track>(100) {
        override fun sizeOf(key: String, value: Track): Int = 1
    }
    
    /**
     * LRU cache for playlist sections with their tracks.
     * Capacity: 20 sections
     */
    private val sectionCache = object : LruCache<String, PlaylistSectionData>(20) {
        override fun sizeOf(key: String, value: PlaylistSectionData): Int = 1
    }
    
    /**
     * LRU cache for search results with 5-minute TTL.
     * Capacity: 10 queries
     */
    private val searchCache = object : LruCache<String, CachedSearchResult>(10) {
        override fun sizeOf(key: String, value: CachedSearchResult): Int = 1
    }
    
    /**
     * LRU cache for Ares AI-generated playlists with 1-hour TTL.
     * Capacity: 10 queries
     */
    private val aresCache = object : LruCache<String, CachedAresResult>(10) {
        override fun sizeOf(key: String, value: CachedAresResult): Int = 1
    }
    
    // Track Cache Operations
    
    /** Generates unique cache key for a track (title + artist). */
    private fun getTrackKey(track: Track): String {
        return "${track.title}_${track.artist}".lowercase()
    }
    
    /** Adds a single track to cache. */
    fun putTrack(track: Track) {
        trackCache.put(getTrackKey(track), track)
    }
    
    /** Adds multiple tracks to cache in a batch operation. */
    fun putTracks(tracks: List<Track>) {
        tracks.forEach { track ->
            trackCache.put(getTrackKey(track), track)
        }
    }
    
    /** Returns cached track by ID, or null if not found. */
    fun getTrack(trackId: String): Track? {
        return trackCache.get(trackId)
    }
    
    /** Returns true if track is currently cached. */
    fun hasTrack(trackId: String): Boolean {
        return trackCache.get(trackId) != null
    }
    
    /** Removes track from cache. */
    fun removeTrack(trackId: String) {
        trackCache.remove(trackId)
    }
    
    // Section Cache Operations
    
    /**
     * Adds playlist section with tracks to cache.
     * Also caches individual tracks for quick access.
     * 
     * @param sectionId Unique section identifier
     * @param section Section metadata
     * @param tracks Associated tracks
     */
    fun putSection(
        sectionId: String,
        section: MusicRecommendationEngine.PlaylistSection,
        tracks: List<Track>
    ) {
        sectionCache.put(sectionId, PlaylistSectionData(section, tracks))
        putTracks(tracks)
    }
    
    /** Returns cached section, or null if not found. */
    fun getSection(sectionId: String): PlaylistSectionData? {
        return sectionCache.get(sectionId)
    }
    
    /** Removes section from cache. */
    fun removeSection(sectionId: String) {
        sectionCache.remove(sectionId)
    }
    
    // Search Cache Operations
    
    /**
     * Caches search results with timestamp for TTL validation.
     * Query is normalized (lowercased and trimmed).
     * Also caches individual tracks.
     * 
     * @param query Search query
     * @param tracks Result tracks
     */
    fun putSearchResults(query: String, tracks: List<Track>) {
        val normalizedQuery = query.lowercase().trim()
        searchCache.put(
            normalizedQuery,
            CachedSearchResult(normalizedQuery, tracks, System.currentTimeMillis())
        )
        putTracks(tracks)
    }
    
    /**
     * Returns cached search results if available and not expired.
     * Entries older than 5 minutes are automatically removed.
     * 
     * @param query Search query
     * @return Cached tracks, or null if not found or expired
     */
    fun getSearchResults(query: String): List<Track>? {
        val normalizedQuery = query.lowercase().trim()
        val cached = searchCache.get(normalizedQuery) ?: return null
        
        val age = System.currentTimeMillis() - cached.timestamp
        val ttl = 5 * 60 * 1000L
        
        return if (age < ttl) cached.tracks else {
            searchCache.remove(normalizedQuery)
            null
        }
    }
    
    /** Returns true if search query is cached and not expired. */
    fun hasSearchResults(query: String): Boolean {
        return getSearchResults(query) != null
    }
    
    // Ares Cache Operations
    
    /**
     * Caches Ares AI-generated playlist results.
     * Query is normalized (lowercased and trimmed).
     * TTL: 1 hour (shorter than Room cache to prioritize freshness).
     * 
     * @param userInput User's emotional input
     * @param queries Gemini-generated queries
     * @param tracks Result tracks
     */
    fun putAresResults(userInput: String, queries: List<String>, tracks: List<Track>) {
        val normalizedInput = userInput.lowercase().trim()
        aresCache.put(
            normalizedInput,
            CachedAresResult(
                userInput = normalizedInput,
                queries = queries,
                tracks = tracks,
                timestamp = System.currentTimeMillis()
            )
        )
        putTracks(tracks)
    }
    
    /**
     * Returns cached Ares results if available and not expired.
     * Entries older than 1 hour are automatically removed.
     * 
     * @param userInput User's emotional input
     * @return Pair of (queries, tracks), or null if not found or expired
     */
    fun getAresResults(userInput: String): Pair<List<String>, List<Track>>? {
        val normalizedInput = userInput.lowercase().trim()
        val cached = aresCache.get(normalizedInput) ?: return null
        
        val age = System.currentTimeMillis() - cached.timestamp
        val ttl = 60 * 60 * 1000L // 1 hour
        
        return if (age < ttl) {
            Pair(cached.queries, cached.tracks)
        } else {
            aresCache.remove(normalizedInput)
            null
        }
    }
    
    /** Returns true if Ares query is cached and not expired. */
    fun hasAresResults(userInput: String): Boolean {
        return getAresResults(userInput) != null
    }
    
    /** Returns age in milliseconds of cached Ares result, or null if not cached. */
    fun getAresResultAge(userInput: String): Long? {
        val normalizedInput = userInput.lowercase().trim()
        val cached = aresCache.get(normalizedInput) ?: return null
        return System.currentTimeMillis() - cached.timestamp
    }
    
    // Cache Management
    
    /** Clears all caches. Use on logout or memory warnings. */
    fun clearAll() {
        trackCache.evictAll()
        sectionCache.evictAll()
        searchCache.evictAll()
        aresCache.evictAll()
    }
    
    /** Clears search cache only. */
    fun clearSearchCache() {
        searchCache.evictAll()
    }
    
    /** Clears Ares cache only. */
    fun clearAresCache() {
        aresCache.evictAll()
    }
    
    /**
     * Trims all caches to specified percentage of capacity.
     * 
     * @param percent Target percentage (0-100)
     */
    fun trimToSize(percent: Int) {
        val trackTargetSize = (100 * percent) / 100
        val sectionTargetSize = (20 * percent) / 100
        val searchTargetSize = (10 * percent) / 100
        
        trackCache.trimToSize(trackTargetSize)
        sectionCache.trimToSize(sectionTargetSize)
        searchCache.trimToSize(searchTargetSize)
    }
    
    /** Returns current cache statistics for monitoring. */
    fun getStats(): CacheStats {
        return CacheStats(
            trackCount = trackCache.size(),
            trackMaxCount = trackCache.maxSize(),
            trackHitCount = trackCache.hitCount().toLong(),
            trackMissCount = trackCache.missCount().toLong(),
            sectionCount = sectionCache.size(),
            sectionMaxCount = sectionCache.maxSize(),
            searchCount = searchCache.size(),
            searchMaxCount = searchCache.maxSize(),
            aresCount = aresCache.size(),
            aresMaxCount = aresCache.maxSize()
        )
    }
    
    // Data Classes
    
    /** Container for playlist section with associated tracks. */
    data class PlaylistSectionData(
        val section: MusicRecommendationEngine.PlaylistSection,
        val tracks: List<Track>
    )
    
    /** Search result with timestamp for TTL validation. */
    private data class CachedSearchResult(
        val query: String,
        val tracks: List<Track>,
        val timestamp: Long
    )
    
    /** Ares AI-generated result with timestamp for TTL validation. */
    private data class CachedAresResult(
        val userInput: String,
        val queries: List<String>,
        val tracks: List<Track>,
        val timestamp: Long
    )
    
    /** Cache statistics container for monitoring and debugging. */
    data class CacheStats(
        val trackCount: Int,
        val trackMaxCount: Int,
        val trackHitCount: Long,
        val trackMissCount: Long,
        val sectionCount: Int,
        val sectionMaxCount: Int,
        val searchCount: Int,
        val searchMaxCount: Int,
        val aresCount: Int,
        val aresMaxCount: Int
    ) {
        /** Cache hit rate as percentage (0-100) */
        val trackHitRate: Float
            get() {
                val total = trackHitCount + trackMissCount
                return if (total > 0) (trackHitCount.toFloat() / total) * 100 else 0f
            }
        
        /** Track cache utilization as percentage (0-100) */    
        val trackUsagePercent: Float
            get() = if (trackMaxCount > 0) {
                (trackCount.toFloat() / trackMaxCount) * 100
            } else 0f
        
        /** Section cache utilization as percentage (0-100) */    
        val sectionUsagePercent: Float
            get() = if (sectionMaxCount > 0) {
                (sectionCount.toFloat() / sectionMaxCount) * 100
            } else 0f
        
        /** Search cache utilization as percentage (0-100) */    
        val searchUsagePercent: Float
            get() = if (searchMaxCount > 0) {
                (searchCount.toFloat() / searchMaxCount) * 100
            } else 0f
    }
}

