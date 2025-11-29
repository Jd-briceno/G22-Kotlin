package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.GeminiService
import com.g22.orbitsoundkotlin.services.SpotifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla "Ares: Modo Emocional"
 * Maneja la generación de playlists basadas en IA (Gemini)
 */
class AresViewModel(
    private val geminiService: GeminiService = GeminiService.getInstance(),
    private val spotifyService: SpotifyService = SpotifyService.getInstance()
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
     * Genera playlist basada en el input emocional del usuario
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
                        recommendations = emptyList()
                    )
                }

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

                // Actualizar estado con resultados
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        recommendations = uniqueTracks,
                        error = if (uniqueTracks.isEmpty()) "No songs found. Try another description." else null
                    )
                }

                // Analytics
                MusicAnalytics.trackAresGeneration(
                    userInput = input,
                    queriesGenerated = queries.size,
                    tracksFound = uniqueTracks.size,
                    success = uniqueTracks.isNotEmpty()
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error generating playlist", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        error = "An error occurred: ${e.message ?: "Try again"}"
                    )
                }

                // Analytics de error
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
        val error: String? = null
    )
}

