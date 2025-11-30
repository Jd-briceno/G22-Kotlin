package com.g22.orbitsoundkotlin.data.repositories

import android.util.Log
import com.g22.orbitsoundkotlin.data.cache.MusicMemoryCache
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.AresCacheEntity
import com.g22.orbitsoundkotlin.models.Track
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Repository para cache de Ares usando triple-level SWR pattern:
 * Level 1 (L1): Memory cache (LRU) - instant (<1ms) - TTL: 1 hour
 * Level 2 (L2): Room cache (SQLite) - fast (~10ms) - TTL: 24 hours
 * Level 3 (L3): Network (Gemini + Spotify) - slow (~2s)
 * 
 * Estrategia de cache:
 * - Online: L1 → L2 válido (<6h) → servir | Cache expirado → fetch L3 + save L1+L2
 * - Offline híbrido: L1 → L2 <24h → servir | Sin cache/cache >24h → error
 */
class AresCacheRepository(
    private val db: AppDatabase,
    private val memoryCache: MusicMemoryCache = MusicMemoryCache()
) {
    private val aresCacheDao = db.aresCacheDao()
    private val gson = Gson()

    companion object {
        private const val TAG = "AresCacheRepository"
    }

    /**
     * Obtiene recomendaciones con triple-level SWR pattern + modo híbrido offline.
     * 
     * @param userInput Input del usuario (será normalizado)
     * @param userId ID del usuario actual
     * @param isOnline Estado de conectividad
     * @param fetchRemote Lambda para obtener datos remotos (Gemini + Spotify)
     * @return Resultado con tracks, queries y metadata de cache
     */
    suspend fun getRecommendations(
        userInput: String,
        userId: String,
        isOnline: Boolean,
        fetchRemote: suspend () -> Pair<List<String>, List<Track>> // (queries, tracks)
    ): AresCacheResult {
        val normalizedInput = normalizeInput(userInput)
        
        // ========== LEVEL 1: MEMORY CACHE (instant, <1ms) ==========
        val memoryResult = memoryCache.getAresResults(userInput)
        if (memoryResult != null) {
            val (queries, tracks) = memoryResult
            val cacheAge = memoryCache.getAresResultAge(userInput) ?: 0L
            Log.d(TAG, "L1 cache HIT (memory) - ${cacheAge / 1000}s old")
            return AresCacheResult.Success(
                queries = queries,
                tracks = tracks,
                fromCache = true,
                cacheAge = cacheAge,
                fromMemory = true
            )
        }
        Log.d(TAG, "L1 cache MISS (memory)")
        
        if (isOnline) {
            // ========== MODO ONLINE ==========
            Log.d(TAG, "Online mode - checking L2 cache (Room) for: $normalizedInput")
            
            // LEVEL 2: ROOM CACHE (~10ms)
            val validCache = aresCacheDao.getValidCache(normalizedInput)
            
            if (validCache != null && validCache.isFresh()) {
                // Cache fresco (<6h): retornar + guardar en L1
                val queries = parseQueries(validCache.geminiResponse)
                val tracks = parseTracks(validCache.trackData)
                Log.d(TAG, "L2 cache HIT (Room) - ${validCache.getAge() / 1000}s old")
                
                // Guardar en L1 para futuros accesos
                memoryCache.putAresResults(userInput, queries, tracks)
                
                return AresCacheResult.Success(
                    queries = queries,
                    tracks = tracks,
                    fromCache = true,
                    cacheAge = validCache.getAge(),
                    fromMemory = false
                )
            }
            
            Log.d(TAG, "L2 cache MISS (Room)")
            
            // LEVEL 3: NETWORK (~2s)
            Log.d(TAG, "Fetching from L3 (Network)")
            val (queries, tracks) = fetchRemote()
            
            // Guardar en L1 + L2
            memoryCache.putAresResults(userInput, queries, tracks)
            saveToCache(normalizedInput, userId, queries, tracks)
            Log.d(TAG, "Saved to L1+L2: ${tracks.size} tracks")
            
            return AresCacheResult.Success(
                queries = queries,
                tracks = tracks,
                fromCache = false,
                cacheAge = 0,
                fromMemory = false
            )
            
        } else {
            // ========== MODO OFFLINE - ESTRATEGIA HÍBRIDA ==========
            Log.d(TAG, "Offline mode - searching L2 cache (Room)")
            
            val anyCache = aresCacheDao.getAnyCacheByInput(normalizedInput)
            
            if (anyCache != null) {
                val cacheAge = anyCache.getAge()
                val oneDayMs = 24 * 60 * 60 * 1000L
                
                if (cacheAge < oneDayMs) {
                    // Cache <24h: retornar con indicador offline + guardar en L1
                    val queries = parseQueries(anyCache.geminiResponse)
                    val tracks = parseTracks(anyCache.trackData)
                    Log.d(TAG, "Offline L2 HIT: serving cache (${cacheAge / 1000}s old)")
                    
                    // Guardar en L1 para futuros accesos
                    memoryCache.putAresResults(userInput, queries, tracks)
                    
                    return AresCacheResult.OfflineSuccess(
                        queries = queries,
                        tracks = tracks,
                        cacheAge = cacheAge
                    )
                } else {
                    Log.d(TAG, "Offline: L2 cache too old (${cacheAge / 1000}s)")
                }
            } else {
                Log.d(TAG, "Offline: no L2 cache found")
            }
            
            // Sin cache o cache muy antiguo (>24h)
            return AresCacheResult.OfflineError("No internet connection. Connect to generate new playlists.")
        }
    }
    
    /**
     * Normaliza el input del usuario para usarlo como key de cache.
     * Lowercase + trim para evitar duplicados por diferencias de case/espacios.
     */
    private fun normalizeInput(input: String): String {
        return input.trim().lowercase()
    }
    
    /**
     * Guarda datos en L2 cache (Room) con TTL de 24 horas.
     */
    private suspend fun saveToCache(
        normalizedInput: String,
        userId: String,
        queries: List<String>,
        tracks: List<Track>
    ) {
        val entity = AresCacheEntity(
            userInput = normalizedInput,
            geminiResponse = gson.toJson(queries),
            trackIds = gson.toJson(tracks.map { "${it.title}_${it.artist}" }), // Simple ID
            trackData = gson.toJson(tracks),
            userId = userId,
            cachedAt = System.currentTimeMillis(),
            expiresAt = AresCacheEntity.calculateExpiresAt()
        )
        aresCacheDao.insertCache(entity)
    }
    
    /**
     * Limpia ambos niveles de cache (L1 memory + L2 Room).
     */
    suspend fun clearAllCache() {
        memoryCache.clearAresCache()
        aresCacheDao.clearAllCache()
        Log.d(TAG, "Cleared all Ares cache (L1 + L2)")
    }
    
    /**
     * Parsea queries desde JSON.
     */
    private fun parseQueries(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing queries", e)
            emptyList()
        }
    }
    
    /**
     * Parsea tracks desde JSON.
     */
    private fun parseTracks(json: String): List<Track> {
        return try {
            val type = object : TypeToken<List<Track>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tracks", e)
            emptyList()
        }
    }
    
    /**
     * Limpia caches expirados (>24h).
     * Llamar periódicamente para mantenimiento.
     */
    suspend fun cleanupExpiredCache() {
        val deleted = aresCacheDao.deleteExpiredCaches()
        Log.d(TAG, "Cleanup: deleted expired caches")
    }
    
    /**
     * Obtiene historial de generaciones de un usuario.
     */
    suspend fun getUserHistory(userId: String, limit: Int = 10): List<AresCacheEntity> {
        return aresCacheDao.getRecentCachesByUser(userId, limit)
    }
}

/**
 * Resultado de operación de cache con diferentes estados.
 */
sealed class AresCacheResult {
    /**
     * Éxito en modo online.
     * @param fromCache true si vino del cache, false si de APIs
     * @param fromMemory true si vino de L1 (memory), false si de L2 (Room)
     */
    data class Success(
        val queries: List<String>,
        val tracks: List<Track>,
        val fromCache: Boolean,
        val cacheAge: Long,
        val fromMemory: Boolean = false
    ) : AresCacheResult()
    
    /**
     * Éxito en modo offline usando cache antiguo (<24h).
     */
    data class OfflineSuccess(
        val queries: List<String>,
        val tracks: List<Track>,
        val cacheAge: Long
    ) : AresCacheResult()
    
    /**
     * Error en modo offline (sin cache o cache >24h).
     */
    data class OfflineError(val message: String) : AresCacheResult()
}

