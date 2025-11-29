package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.repositories.AchievementRepository
import com.g22.orbitsoundkotlin.models.Achievement
import com.g22.orbitsoundkotlin.models.Track
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel para la pantalla de perfil del usuario
 * Carga datos reales del usuario y achievements desbloqueados
 */
class ProfileViewModel(
    private val userId: String,
    private val achievementRepository: AchievementRepository? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {
    
    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadUserData()
        loadAchievements()
    }
    
    /**
     * Load real user data from Firebase
     */
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                if (userId.isEmpty()) return@launch
                
                val userDoc = firestore.collection("users").document(userId).get().await()
                val username = userDoc.getString("name") ?: userDoc.getString("email")?.split("@")?.firstOrNull() ?: "Captain"
                val title = userDoc.getString("title") ?: "Music Explorer"
                val bio = userDoc.getString("bio") ?: "Discovering the universe of sound"
                
                _uiState.update {
                    it.copy(
                        username = username,
                        title = title,
                        bio = bio
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
            }
        }
    }
    
    /**
     * Load unlocked achievements from repository
     */
    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                if (achievementRepository == null || userId.isEmpty()) return@launch
                
                val achievements = achievementRepository.getUnlockedAchievements(userId)
                _uiState.update {
                    it.copy(achievements = achievements)
                }
                Log.d(TAG, "Loaded ${achievements.size} unlocked achievements")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading achievements", e)
            }
        }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun playNextTrack() {
        // TODO: Implementar lógica de siguiente canción
    }

    fun playPreviousTrack() {
        // TODO: Implementar lógica de canción anterior
    }

    data class ProfileUiState(
        val username: String = "Loading...",
        val title: String = "Music Explorer",
        val bio: String = "Discovering the universe of sound",
        val currentTrack: Track = Track(
            title = "No track playing",
            artist = "Select a song",
            albumArt = "",
            duration = "0:00",
            durationMs = 0
        ),
        val isPlaying: Boolean = false,
        val achievements: List<Achievement> = emptyList()
    )
}

/**
 * Factory for ProfileViewModel
 */
class ProfileViewModelFactory(
    private val context: Context,
    private val userId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val database = com.g22.orbitsoundkotlin.data.local.AppDatabase.getInstance(context)
            val achievementRepo = AchievementRepository(database)
            
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(
                userId = userId,
                achievementRepository = achievementRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
