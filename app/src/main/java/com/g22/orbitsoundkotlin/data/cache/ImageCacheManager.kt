package com.g22.orbitsoundkotlin.data.cache

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.io.File

/**
 * Centralized image cache manager using Coil library.
 * 
 * Provides a singleton [ImageLoader] with optimized multi-layer caching:
 * - Memory cache: 25% of available heap (volatile)
 * - Disk cache: 50MB persistent storage
 * - Network cache: 10MB HTTP response cache
 * 
 * All caches use LRU eviction policy and respect HTTP cache headers.
 * Thread-safe with lazy initialization.
 * 
 * @see getImageLoader
 * @see clearAllCaches
 */
object ImageCacheManager {
    
    private var imageLoader: ImageLoader? = null
    
    /**
     * Creates memory cache configuration.
     * 
     * Allocates 25% of available heap with strong references enabled
     * to prevent premature garbage collection of cached bitmaps.
     */
    private fun createMemoryCache(context: Context): MemoryCache {
        return MemoryCache.Builder(context)
            .maxSizePercent(0.25) // 25% of available memory
            .strongReferencesEnabled(true) // Keep strong references
            .build()
    }
    
    /**
     * Creates disk cache configuration.
     * 
     * Uses app-specific cache directory with 50MB size limit.
     * LRU eviction automatically removes least recently used images when full.
     * Persists across app restarts but cleared on app uninstall.
     */
    private fun createDiskCache(context: Context): DiskCache {
        return DiskCache.Builder()
            .directory(File(context.cacheDir, "image_cache")) // /data/data/.../cache/image_cache/
            .maxSizeBytes(50 * 1024 * 1024) // 50 MB
            .build()
    }
    
    /**
     * Creates OkHttp client with HTTP response caching.
     * 
     * Respects server cache directives (Cache-Control, ETag, Last-Modified).
     * Uses 10MB cache for network responses.
     */
    private fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .cache(
                coil.network.CacheStrategy.Companion.cacheDir(context)?.let { cacheDir ->
                    okhttp3.Cache(
                        directory = File(cacheDir, "http_cache"),
                        maxSize = 10L * 1024L * 1024L // 10 MB
                    )
                }
            )
            .build()
    }
    
    /**
     * Returns the singleton [ImageLoader] instance, creating it if necessary.
     * 
     * Thread-safe with double-checked locking. Images are loaded from
     * memory → disk → network cache → network download in that order.
     * 
     * @param context Application or activity context
     * @return Configured ImageLoader instance
     */
    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: buildImageLoader(context).also { imageLoader = it }
        }
    }
    
    /**
     * Builds ImageLoader with configured cache layers and policies.
     */
    private fun buildImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache { createMemoryCache(context) }
            .diskCache { createDiskCache(context) }
            .okHttpClient { createOkHttpClient(context) }
            .respectCacheHeaders(true) // Honor server cache directives
            .crossfade(true) // Smooth fade-in animation (300ms)
            .crossfade(300)
            // Cache policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    
    /**
     * Clears both memory and disk caches.
     * 
     * Use when clearing user data on logout or recovering from cache corruption.
     * 
     * @param context Application context for accessing cache directories
     */
    fun clearAllCaches(context: Context) {
        imageLoader?.let { loader ->
            loader.memoryCache?.clear()
            loader.diskCache?.clear()
        }
    }
    
    /**
     * Clears memory cache only, preserving disk cache.
     * 
     * Should be called in response to low memory warnings (onTrimMemory).
     */
    fun clearMemoryCache() {
        imageLoader?.memoryCache?.clear()
    }
    
    /**
     * Returns current cache statistics for monitoring and debugging.
     * 
     * @return [CacheStats] containing memory and disk usage information
     */
    fun getCacheStats(): CacheStats {
        val loader = imageLoader ?: return CacheStats()
        
        return CacheStats(
            memorySizeBytes = loader.memoryCache?.size ?: 0,
            memoryMaxSizeBytes = loader.memoryCache?.maxSize ?: 0,
            diskSizeBytes = loader.diskCache?.size ?: 0,
            diskMaxSizeBytes = loader.diskCache?.maxSize ?: 0
        )
    }
    
    /**
     * Cache statistics container.
     * 
     * @property memorySizeBytes Current memory cache size in bytes
     * @property memoryMaxSizeBytes Maximum memory cache size in bytes
     * @property diskSizeBytes Current disk cache size in bytes
     * @property diskMaxSizeBytes Maximum disk cache size in bytes
     */
    data class CacheStats(
        val memorySizeBytes: Long = 0,
        val memoryMaxSizeBytes: Long = 0,
        val diskSizeBytes: Long = 0,
        val diskMaxSizeBytes: Long = 0
    ) {
        /** Memory cache usage as percentage (0-100) */
        val memoryUsagePercent: Float
            get() = if (memoryMaxSizeBytes > 0) {
                (memorySizeBytes.toFloat() / memoryMaxSizeBytes) * 100
            } else 0f
        
        /** Disk cache usage as percentage (0-100) */    
        val diskUsagePercent: Float
            get() = if (diskMaxSizeBytes > 0) {
                (diskSizeBytes.toFloat() / diskMaxSizeBytes) * 100
            } else 0f
    }
}

