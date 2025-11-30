package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.repositories.AresCacheRepository
import com.g22.orbitsoundkotlin.data.repositories.AresCacheResult
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.GeminiService
import com.g22.orbitsoundkotlin.services.SpotifyService
import com.g22.orbitsoundkotlin.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla "Ares: Modo Emocional"
 * Maneja la generación de playlists basadas en IA (Gemini) con cache y soporte offline
 */
class AresViewModel(
    private val geminiService: GeminiService = GeminiService.getInstance(),
    private val spotifyService: SpotifyService = SpotifyService.getInstance(),
    private val aresCacheRepository: AresCacheRepository? = null,
    private val networkMonitor: NetworkMonitor? = null,
    private val userId: String = "",
    private val achievementService: com.g22.orbitsoundkotlin.services.AchievementService? = null
) : ViewModel() {

    companion object {
        private const val TAG = "AresViewModel"
        private const val MAX_TRACKS = 15
    }

    private val _uiState = MutableStateFlow(AresUiState())
    val uiState: StateFlow<AresUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "AresViewModel initialized")
        MusicAnalytics.trackAresScreenView()
    }

    /**
     * Actualiza el input del usuario
     */
    fun onInputChange(text: String) {
        _uiState.update { it.copy(userInput = text, error = null) }
    }

    /**
     * Genera playlist basada en el input emocional del usuario.
     * Usa cache con SWR pattern y soporte offline híbrido.
     */
    fun generatePlaylist() {
        val input = _uiState.value.userInput.trim()

        // Validación
        if (input.isEmpty()) {
            _uiState.update {
                it.copy(error = "Please write how you feel to generate recommendations")
            }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isLoading = true,
                        loadingMessage = "Analyzing emotions...",
                        error = null,
                        recommendations = emptyList(),
                        isOfflineMode = false,
                        fromCache = false
                    )
                }

                // Verificar conectividad
                val isOnline = networkMonitor?.isConnected() ?: true
                Log.d(TAG, "Network status: ${if (isOnline) "Online" else "Offline"}")

                // Si hay repository, usar cache con SWR pattern
                if (aresCacheRepository != null && userId.isNotEmpty()) {
                    val result = aresCacheRepository.getRecommendations(
                        userInput = input,
                        userId = userId,
                        isOnline = isOnline,
                        fetchRemote = {
                            // Lambda para fetch remoto (Gemini + Spotify)
                            fetchFromApis(input)
                        }
                    )
                    
                    handleCacheResult(result, input)
                } else {
                    // Fallback sin cache: llamar APIs directamente
                    Log.w(TAG, "No cache repository, fetching directly")
                    val (queries, tracks) = fetchFromApis(input)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadingMessage = null,
                            recommendations = tracks,
                            error = if (tracks.isEmpty()) "No songs found. Try another description." else null
                        )
                    }
                    
                    MusicAnalytics.trackAresGeneration(
                        userInput = input,
                        queriesGenerated = queries.size,
                        tracksFound = tracks.size,
                        success = tracks.isNotEmpty()
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating playlist", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred: ${e.message ?: "Try again"}"
                    )
                }

                MusicAnalytics.trackAresGeneration(
                    userInput = input,
                    queriesGenerated = 0,
                    tracksFound = 0,
                    success = false
                )
            }
        }
    }
    
    /**
     * Maneja el resultado del cache (online/offline/error).
     */
    private fun handleCacheResult(result: AresCacheResult, originalInput: String) {
        when (result) {
            is AresCacheResult.Success -> {
                val cacheAgeHours = (result.cacheAge / (60 * 60 * 1000)).toInt()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        recommendations = result.tracks,
                        fromCache = result.fromCache,
                        isOfflineMode = false,
                        cacheAgeHours = cacheAgeHours,
                        error = if (result.tracks.isEmpty()) "No songs found. Try another description." else null
                    )
                }
                
                // Analytics
                if (result.fromCache) {
                    MusicAnalytics.trackAresCacheHit(
                        cacheAgeHours = cacheAgeHours,
                        fromMemory = result.fromMemory,
                        fromRoom = !result.fromMemory
                    )
                }
                
                MusicAnalytics.trackAresGeneration(
                    userInput = originalInput,
                    queriesGenerated = result.queries.size,
                    tracksFound = result.tracks.size,
                    success = result.tracks.isNotEmpty()
                )
                
                // Check AI Maestro achievement (only on successful generation with tracks)
                if (result.tracks.isNotEmpty() && userId.isNotEmpty()) {
                    Log.d(TAG, "Checking AI Maestro achievement for user: $userId")
                    if (achievementService != null) {
                        achievementService.checkAIMaestro(userId)
                    } else {
                        Log.w(TAG, "AchievementService is null, cannot unlock AI Maestro")
                    }
                } else {
                    Log.d(TAG, "Skipping AI Maestro: tracks=${result.tracks.size}, userId=$userId")
                }
            }
            
            is AresCacheResult.OfflineSuccess -> {
                val cacheAgeHours = (result.cacheAge / (60 * 60 * 1000)).toInt()
                
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        recommendations = result.tracks,
                        isOfflineMode = true,
                        fromCache = true,
                        cacheAgeHours = cacheAgeHours
                    )
                }
                
                // Analytics offline
                MusicAnalytics.trackAresOfflineMode(
                    cacheAgeHours = cacheAgeHours,
                    hadResults = result.tracks.isNotEmpty()
                )
            }
            
            is AresCacheResult.OfflineError -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = result.message
                    )
                }
                
                // Analytics offline sin resultados
                MusicAnalytics.trackAresOfflineMode(
                    cacheAgeHours = 0,
                    hadResults = false
                )
            }
        }
    }
    
    /**
     * Llama a las APIs (Gemini + Spotify) y retorna queries y tracks.
     */
    private suspend fun fetchFromApis(input: String): Pair<List<String>, List<Track>> = coroutineScope {
        // Paso 1: Construir prompt y obtener queries de Gemini
        Log.d(TAG, "Requesting queries from Gemini for input: $input")
        val prompt = buildPromptForEmotionalMusic(input)
        val geminiResponse = geminiService.generateContent(prompt)

        // Parsear respuesta de Gemini
        var queries: List<String>? = null
        if (geminiResponse != null) {
            queries = parseQueriesFromResponse(geminiResponse)
            Log.d(TAG, "Received ${queries?.size ?: 0} queries from Gemini")
        }

        // Si Gemini no devolvió queries válidas, usar búsqueda directa con el input del usuario
        if (queries.isNullOrEmpty()) {
            Log.w(TAG, "Gemini failed or no queries, using direct user input as query")
            _uiState.update { it.copy(loadingMessage = "Searching for music...") }
            queries = listOf(input) // Usar el input del usuario directamente como query
        } else {
            _uiState.update { it.copy(loadingMessage = "Searching for perfect songs...") }
        }

        // Paso 2: Buscar tracks en Spotify en paralelo
        val trackLists = queries.map { query ->
            async(Dispatchers.IO) {
                Log.d(TAG, "Searching Spotify with query: $query")
                spotifyService.searchTracks(query)
            }
        }.map { it.await() }

        // Paso 3: Combinar y deduplicar resultados
        val allTracks = trackLists.flatten()
        Log.d(TAG, "Found ${allTracks.size} total tracks before deduplication")

        val uniqueTracks = allTracks
            .distinctBy { it.title + it.artist } // Deduplicar por título + artista
            .take(MAX_TRACKS)

        Log.d(TAG, "Final playlist: ${uniqueTracks.size} unique tracks")
        
        Pair(queries, uniqueTracks)
    }

    /**
     * Limpia el error actual
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Limpia las recomendaciones (para empezar de nuevo)
     */
    fun clearRecommendations() {
        _uiState.update {
            it.copy(
                userInput = "",
                recommendations = emptyList(),
                error = null
            )
        }
    }

    /**
     * Construye el prompt para Gemini basado en el input emocional
     */
    private fun buildPromptForEmotionalMusic(userInput: String): String {
        return """The user wrote: "$userInput"

IMPORTANT: Analyze EXACTLY what the user wants to feel. If they want to feel sad, depressed, melancholic, or any specific emotion, RESPECT that intention completely. Do NOT try to make them feel better if they don't want to.

Based on their emotional input, generate 3-4 music search queries for Spotify that MATCH their desired emotional state.

Rules:
- If they want sad music → generate queries for sad/melancholic music
- If they want happy music → generate queries for happy/upbeat music
- If they want to transition emotions → generate queries for that transition
- Queries must be in English, concise, and effective for music search
- Return ONLY the queries separated by line breaks, no explanations, no numbering, no markdown

Example format:
sad melancholic piano music
heartbreak emotional songs
depressing indie acoustic
rainy day mood music"""
    }

    /**
     * Parsea la respuesta de Gemini para extraer las queries
     */
    private fun parseQueriesFromResponse(response: String): List<String>? {
        val queries = response.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("#") } // Eliminar comentarios
            .filter { !it.matches(Regex("^\\d+[.)].+")) } // Eliminar numeración
            .take(4) // Máximo 4 queries

        return if (queries.isEmpty()) null else queries
    }

    /**
     * Estado de la UI para AresScreen
     */
    data class AresUiState(
        val userInput: String = "",
        val isLoading: Boolean = false,
        val loadingMessage: String? = null,
        val recommendations: List<Track> = emptyList(),
        val error: String? = null,
        val isOfflineMode: Boolean = false, // Indica si se está usando cache offline
        val fromCache: Boolean = false, // Indica si los resultados vienen del cache
        val cacheAgeHours: Int = 0 // Edad del cache en horas
    )
}

/**
 * Factory para crear AresViewModel con dependencias.
 */
class AresViewModelFactory(
    private val context: Context,
    private val userId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AresViewModel::class.java)) {
            val database = AppDatabase.getInstance(context)
            val aresCacheRepository = AresCacheRepository(database)
            val networkMonitor = NetworkMonitor(context)
            val achievementRepo = com.g22.orbitsoundkotlin.data.repositories.AchievementRepository(database)
            val achievementService = com.g22.orbitsoundkotlin.services.AchievementService.getInstance(context, achievementRepo)
            
            @Suppress("UNCHECKED_CAST")
            return AresViewModel(
                geminiService = GeminiService.getInstance(),
                spotifyService = SpotifyService.getInstance(),
                aresCacheRepository = aresCacheRepository,
                networkMonitor = networkMonitor,
                userId = userId,
                achievementService = achievementService
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

