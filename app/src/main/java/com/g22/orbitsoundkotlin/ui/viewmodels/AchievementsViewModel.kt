package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.repositories.AchievementRepository
import com.g22.orbitsoundkotlin.models.Achievement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Achievements screen.
 * Shows all unlocked achievements with timestamps.
 */
class AchievementsViewModel(
    private val userId: String,
    private val achievementRepository: AchievementRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AchievementsViewModel"
    }
    
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "AchievementsViewModel initialized")
        MusicAnalytics.trackAchievementsScreenView()
        loadAchievements()
    }
    
    /**
     * Load all unlocked achievements for the user
     */
    private fun loadAchievements() {
        viewModelScope.launch {
            try {
                val achievements = achievementRepository.getUnlockedAchievements(userId)
                _uiState.value = AchievementsUiState(
                    achievements = achievements,
                    isLoading = false
                )
                Log.d(TAG, "Loaded ${achievements.size} unlocked achievements")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading achievements", e)
                _uiState.value = AchievementsUiState(
                    isLoading = false,
                    error = "Failed to load achievements"
                )
            }
        }
    }
    
    data class AchievementsUiState(
        val achievements: List<Achievement> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null
    )
}

/**
 * Factory for AchievementsViewModel
 */
class AchievementsViewModelFactory(
    private val context: Context,
    private val userId: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AchievementsViewModel::class.java)) {
            val database = com.g22.orbitsoundkotlin.data.local.AppDatabase.getInstance(context)
            val achievementRepo = AchievementRepository(database)
            
            @Suppress("UNCHECKED_CAST")
            return AchievementsViewModel(
                userId = userId,
                achievementRepository = achievementRepo
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

