package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.EmotionRepository
import com.g22.orbitsoundkotlin.data.FacialEmotionAnalyzer
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
    private val repository: EmotionRepository = FirestoreEmotionRepository(),
    private val context: Context? = null
) : ViewModel() {
    private val TAG = "StellarEmotionsViewModel"

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()

    private val _capturedPhotoUri = MutableStateFlow<Uri?>(null)
    val capturedPhotoUri: StateFlow<Uri?> = _capturedPhotoUri.asStateFlow()

    private val _isAnalyzingEmotion = MutableStateFlow(false)
    val isAnalyzingEmotion: StateFlow<Boolean> = _isAnalyzingEmotion.asStateFlow()

    fun onPhotoCaptured(uri: Uri) {
        Log.d(TAG, "Photo captured: $uri")
        _capturedPhotoUri.value = uri
        // Analyze the emotion immediately
        analyzeEmotionFromPhoto(uri)
    }

    private fun analyzeEmotionFromPhoto(uri: Uri) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot analyze emotion")
            viewModelScope.launch {
                _event.emit(Event.ShowError("Unable to analyze emotion: context not available"))
            }
            return
        }

        viewModelScope.launch {
            _isAnalyzingEmotion.value = true
            _uiState.value = UiState.Loading
            try {
                Log.d(TAG, "Starting emotion analysis...")
                val analyzer = FacialEmotionAnalyzer(context)
                val detectedEmotion = analyzer.analyzeEmotion(uri)

                if (detectedEmotion != null) {
                    Log.d(TAG, "Emotion detected: $detectedEmotion")

                    // Create EmotionModel with detected emotion
                    val emotionModel = createEmotionModelFromDetection(detectedEmotion)

                    // Log the emotion to database with "camera" as source
                    logDetectedEmotion(emotionModel)
                } else {
                    Log.e(TAG, "Failed to detect emotion")
                    _uiState.value = UiState.Idle
                    _event.emit(Event.ShowError("Could not detect emotion from the image. Please try again."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during emotion analysis", e)
                _uiState.value = UiState.Idle
                _event.emit(Event.ShowError("Error analyzing emotion: ${e.message}"))
            } finally {
                _isAnalyzingEmotion.value = false
            }
        }
    }

    private fun createEmotionModelFromDetection(emotionName: String): EmotionModel {
        // Map the detected emotion name to an EmotionModel
        // Using a normalized ID (lowercase) and "camera" as source
        return EmotionModel(
            id = emotionName.lowercase(),
            name = emotionName,
            description = "Detected from camera",
            color = androidx.compose.ui.graphics.Color.White, // Default color
            iconRes = 0, // No icon needed for camera detection
            source = "camera"
        )
    }

    private suspend fun logDetectedEmotion(emotionModel: EmotionModel) {
        if (userId.isBlank()) {
            Log.e(TAG, "User ID is blank, cannot proceed")
            _uiState.value = UiState.Idle
            _event.emit(Event.ShowError("User ID is missing, please log in again"))
            return
        }

        try {
            Log.d(TAG, "Logging detected emotion: ${emotionModel.name} with source: ${emotionModel.source}")
            val firestoreRepo = repository as? FirestoreEmotionRepository
                ?: throw IllegalStateException("Repository must be FirestoreEmotionRepository")

            val emotionLog = firestoreRepo.createEmotionLog(userId, listOf(emotionModel))
            val result = repository.logEmotions(userId, emotionLog)

            if (result.isSuccess) {
                Log.d(TAG, "Successfully logged detected emotion")
                _uiState.value = UiState.Success
                _event.emit(Event.EmotionDetected(emotionModel.name))
                _event.emit(Event.NavigateNext)
            } else {
                val exception = result.exceptionOrNull()
                Log.e(TAG, "Failed to log detected emotion", exception)
                val errorMessage = exception?.message ?: "Failed to save emotion"
                _uiState.value = UiState.Error(errorMessage)
                _event.emit(Event.ShowError(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception logging detected emotion", e)
            _uiState.value = UiState.Error(e.message ?: "Unknown error")
            _event.emit(Event.ShowError(e.message ?: "Unknown error occurred"))
        }
    }

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
        data class EmotionDetected(val emotion: String) : Event()
        object NavigateNext : Event()
    }
}