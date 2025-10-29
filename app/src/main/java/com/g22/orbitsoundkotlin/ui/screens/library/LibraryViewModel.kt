package com.g22.orbitsoundkotlin.ui.screens.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.MusicRecommendationEngine
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.SpotifyService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val spotifyService: SpotifyService = SpotifyService.getInstance()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    
    init {
        Log.d("LibraryViewModel", "Inicializando LibraryViewModel")
        loadPersonalizedPlaylists()
    }
    
    /**
     * Actualiza las recomendaciones basadas en constelaciones y emociones del usuario.
     * Puede ser llamado cuando el usuario actualice sus preferencias.
     */
    fun refreshRecommendations(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        loadPersonalizedPlaylists(userConstellations, recentEmotions)
    }
    
    fun searchTracks(query: String) {
        if (query.isEmpty()) return
        
        Log.d("LibraryViewModel", "Buscando tracks: $query")
        viewModelScope.launch {
            _uiState.update { it.copy(searchLoading = true) }
            try {
                val tracks = spotifyService.searchTracks(query)
                Log.d("LibraryViewModel", "Tracks encontrados: ${tracks.size}")
                _uiState.update { it.copy(
                    searchResults = tracks,
                    searchLoading = false
                )}
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error buscando tracks", e)
                _uiState.update { it.copy(
                    searchLoading = false,
                    error = e.message
                )}
            }
        }
    }
    
    fun selectTrack(track: Track) {
        _uiState.update { it.copy(selectedTrack = track) }
    }
    
    fun dismissTrackDetail() {
        _uiState.update { it.copy(selectedTrack = null) }
    }
    
    /**
     * Carga playlists personalizadas usando el motor de recomendaciones.
     */
    private fun loadPersonalizedPlaylists(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        Log.d("LibraryViewModel", "Cargando playlists personalizadas...")
        Log.d("LibraryViewModel", "Constelaciones: ${userConstellations.size}, Emociones: ${recentEmotions.size}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(playlistsLoading = true) }
            try {
                // Generar secciones personalizadas
                val sections = MusicRecommendationEngine.generatePlaylistSections(
                    userConstellations = userConstellations,
                    recentEmotions = recentEmotions
                )
                
                Log.d("LibraryViewModel", "Secciones generadas: ${sections.joinToString { it.title }}")
                Log.d("LibraryViewModel", "Iniciando búsquedas paralelas")
                
                // Cargar tracks para cada sección en paralelo
                val section1Deferred = async { 
                    spotifyService.searchTracks(sections[0].query)
                }
                val section2Deferred = async { 
                    spotifyService.searchTracks(sections[1].query)
                }
                val section3Deferred = async { 
                    spotifyService.searchTracks(sections[2].query)
                }
                val section4Deferred = async { 
                    spotifyService.searchTracks(sections[3].query)
                }
                
                val section1Songs = section1Deferred.await()
                val section2Songs = section2Deferred.await()
                val section3Songs = section3Deferred.await()
                val section4Songs = section4Deferred.await()
                
                Log.d("LibraryViewModel", "Playlists cargadas: ${section1Songs.size}, ${section2Songs.size}, ${section3Songs.size}, ${section4Songs.size}")
                
                _uiState.update { it.copy(
                    section1 = PlaylistSectionData(sections[0], section1Songs),
                    section2 = PlaylistSectionData(sections[1], section2Songs),
                    section3 = PlaylistSectionData(sections[2], section3Songs),
                    section4 = PlaylistSectionData(sections[3], section4Songs),
                    playlistsLoading = false
                )}
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error cargando playlists personalizadas", e)
                _uiState.update { it.copy(
                    playlistsLoading = false,
                    error = e.message
                )}
            }
        }
    }
    
    /**
     * Data class que combina información de la sección con sus tracks.
     */
    data class PlaylistSectionData(
        val section: MusicRecommendationEngine.PlaylistSection,
        val tracks: List<Track>
    )
    
    /**
     * Estado de la UI de LibraryScreen.
     */
    data class LibraryUiState(
        // Búsqueda
        val searchResults: List<Track> = emptyList(),
        val searchLoading: Boolean = false,
        
        // Secciones personalizadas de playlists
        val section1: PlaylistSectionData? = null,
        val section2: PlaylistSectionData? = null,
        val section3: PlaylistSectionData? = null,
        val section4: PlaylistSectionData? = null,
        
        // Estado general
        val playlistsLoading: Boolean = false,
        val selectedTrack: Track? = null,
        val error: String? = null
    ) {
        // Propiedades de compatibilidad con código legacy (LibraryScreen)
        @Deprecated("Use section1.tracks instead", ReplaceWith("section1?.tracks ?: emptyList()"))
        val starlightSongs: List<Track> get() = section1?.tracks ?: emptyList()
        
        @Deprecated("Use section2.tracks instead", ReplaceWith("section2?.tracks ?: emptyList()"))
        val djNovaSongs: List<Track> get() = section2?.tracks ?: emptyList()
        
        @Deprecated("Use section3.tracks instead", ReplaceWith("section3?.tracks ?: emptyList()"))
        val eternalHitsSongs: List<Track> get() = section3?.tracks ?: emptyList()
        
        @Deprecated("Use section4.tracks instead", ReplaceWith("section4?.tracks ?: emptyList()"))
        val orbitCrewSongs: List<Track> get() = section4?.tracks ?: emptyList()
    }
}

