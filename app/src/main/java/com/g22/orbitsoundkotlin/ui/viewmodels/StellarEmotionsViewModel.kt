package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.EmotionRepository
import com.g22.orbitsoundkotlin.data.FirestoreEmotionRepository
import com.g22.orbitsoundkotlin.models.EmotionModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StellarEmotionsViewModel(
    private val userId: String,
    private val repository: EmotionRepository = FirestoreEmotionRepository()
) : ViewModel() {
    private val TAG = "StellarEmotionsViewModel"

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()

    fun onReadyToShipClicked(selectedEmotions: List<EmotionModel>) {
        Log.d(TAG, "Ready to ship clicked with ${selectedEmotions.size} emotions")

        if (selectedEmotions.isEmpty()) {
            viewModelScope.launch {
                _event.emit(Event.ShowError("Please select at least one emotion"))
            }
            return
        }

        if (userId.isBlank()) {
            Log.e(TAG, "User ID is blank, cannot proceed")
            viewModelScope.launch {
                _event.emit(Event.ShowError("User ID is missing, please log in again"))
            }
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                Log.d(TAG, "Creating emotion log for user $userId")
                val firestoreRepo = repository as? FirestoreEmotionRepository
                    ?: throw IllegalStateException("Repository must be FirestoreEmotionRepository")

                val emotionLog = firestoreRepo.createEmotionLog(userId, selectedEmotions)
                Log.d(TAG, "Created emotion log, calling repository")
                val result = repository.logEmotions(userId, emotionLog)

                if (result.isSuccess) {
                    Log.d(TAG, "Successfully logged emotions")
                    _uiState.value = UiState.Success
                    _event.emit(Event.NavigateNext)
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Failed to log emotions", exception)
                    val errorMessage = exception?.message ?: "Failed to save emotions"
                    _uiState.value = UiState.Error(errorMessage)
                    _event.emit(Event.ShowError(errorMessage))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in onReadyToShipClicked", e)
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
                _event.emit(Event.ShowError(e.message ?: "Unknown error occurred"))
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class Event {
        data class ShowError(val message: String) : Event()
        object NavigateNext : Event()
    }
}