package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.SpotifyService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val spotifyService: SpotifyService = SpotifyService.Companion.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadPlaylists()
    }

    fun searchTracks(query: String) {
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(searchLoading = true) }
            try {
                val tracks = spotifyService.searchTracks(query)
                _uiState.update { it.copy(
                    searchResults = tracks,
                    searchLoading = false
                )}
            } catch (e: Exception) {
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

    private fun loadPlaylists() {
        viewModelScope.launch {
            _uiState.update { it.copy(playlistsLoading = true) }
            try {
                val starlightDeferred = async { spotifyService.searchTracks("lofi music") }
                val djNovaDeferred = async { spotifyService.searchTracks("electronic dance music") }
                val eternalHitsDeferred = async { spotifyService.searchTracks("rock music") }
                val orbitCrewDeferred = async { spotifyService.searchTracks("pop hits") }

                _uiState.update { it.copy(
                    starlightSongs = starlightDeferred.await(),
                    djNovaSongs = djNovaDeferred.await(),
                    eternalHitsSongs = eternalHitsDeferred.await(),
                    orbitCrewSongs = orbitCrewDeferred.await(),
                    playlistsLoading = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    starlightSongs = getFallbackTracks(),
                    djNovaSongs = getFallbackTracks(),
                    eternalHitsSongs = getFallbackTracks(),
                    orbitCrewSongs = getFallbackTracks(),
                    playlistsLoading = false,
                    error = e.message
                )}
            }
        }
    }

    private fun getFallbackTracks() = listOf(
        Track("Lofi Study", "Chill Beats", "3:45", 225000, ""),
        Track("Peaceful Morning", "Ambient Sounds", "4:12", 252000, ""),
        Track("Coffee Shop Vibes", "Relaxing Music", "3:30", 210000, "")
    )

    data class LibraryUiState(
        val searchResults: List<Track> = emptyList(),
        val searchLoading: Boolean = false,
        val starlightSongs: List<Track> = emptyList(),
        val djNovaSongs: List<Track> = emptyList(),
        val eternalHitsSongs: List<Track> = emptyList(),
        val orbitCrewSongs: List<Track> = emptyList(),
        val playlistsLoading: Boolean = false,
        val selectedTrack: Track? = null,
        val error: String? = null
    )
}