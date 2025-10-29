package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.g22.orbitsoundkotlin.models.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun playNextTrack() {
        // Lógica para siguiente canción
        // Por ahora solo cambiamos el estado a playing
        _uiState.update { it.copy(isPlaying = true) }
    }

    fun playPreviousTrack() {
        // Lógica para canción anterior
        // Por ahora solo cambiamos el estado a playing
        _uiState.update { it.copy(isPlaying = true) }
    }

    data class ProfileUiState(
        val currentTrack: Track = Track(
            title = "Vengeance",
            artist = "Coldrain",
            duration = "3:45",
            durationMs = 225000,
            albumArt = "assets/images/Coldrain.jpg"
        ),
        val isPlaying: Boolean = true,
        val username: String = "Higan",
        val title: String = "Hero X",
        val bio: String = "From calm seas to wild storms — I have a track for it 🌊⚡"
    )
}