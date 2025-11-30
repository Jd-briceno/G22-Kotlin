package com.g22.orbitsoundkotlin.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.DefaultEmotionLogFactory
import com.g22.orbitsoundkotlin.data.EmotionLogFactory
import com.g22.orbitsoundkotlin.data.EmotionRepository
import com.g22.orbitsoundkotlin.data.FacialEmotionAnalyzer
import com.g22.orbitsoundkotlin.data.OfflineFirstEmotionRepository
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.utils.SyncEventManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StellarEmotionsViewModel(
    private val userId: String,
    private val context: Context,
    private val repository: EmotionRepository = OfflineFirstEmotionRepository(context),
    private val emotionLogFactory: EmotionLogFactory = DefaultEmotionLogFactory
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

    init {
        // Escuchar eventos de sincronización en segundo plano
        observeSyncEvents()

        // Verificar si hubo sincronizaciones recientes mientras la app estaba cerrada
        checkRecentSync()
    }

    private fun checkRecentSync() {
        viewModelScope.launch {
            val lastSyncTime = SyncEventManager.getLastSyncTime(context)
            val lastSyncCount = SyncEventManager.getLastSyncCount(context)

            // Si hubo una sincronización en los últimos 10 segundos, mostrar el toast
            if (lastSyncTime > 0 && System.currentTimeMillis() - lastSyncTime < 10_000L && lastSyncCount > 0) {
                _event.emit(Event.ShowSuccess("Emotions successfully logged ($lastSyncCount synced to cloud)"))
                // Limpiar para no mostrar de nuevo
                SyncEventManager.saveLastSyncTime(context, 0)
            }
        }
    }

    private fun observeSyncEvents() {
        viewModelScope.launch {
            SyncEventManager.emotionSyncEvents.collect { event ->
                when (event) {
                    is SyncEventManager.EmotionSyncEvent.SyncSuccess -> {
                        Log.d(TAG, "Received sync success event: ${event.count} emotions synced")
                        _event.emit(Event.ShowSuccess("Emotions successfully logged (${event.count} synced to cloud)"))
                    }
                    is SyncEventManager.EmotionSyncEvent.SyncFailure -> {
                        Log.e(TAG, "Received sync failure event: ${event.error}")
                        // Opcionalmente mostrar error, pero no es crítico ya que se reintentará
                    }
                }
            }
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        Log.d(TAG, "Photo captured: $uri")
        _capturedPhotoUri.value = uri
        // Analyze the emotion immediately
        analyzeEmotionFromPhoto(uri)
    }

    private fun analyzeEmotionFromPhoto(uri: Uri) {

        // Launch on Main dispatcher (viewModelScope default) for UI state management
        viewModelScope.launch {
            _isAnalyzingEmotion.value = true
            _uiState.value = UiState.Loading
            try {
                Log.d(TAG, "Starting emotion analysis...")

                // Perform emotion analysis on IO dispatcher (network/file operations)
                val detectedEmotion = withContext(Dispatchers.IO) {
                    val analyzer = FacialEmotionAnalyzer(context)
                    analyzer.analyzeEmotion(uri)
                }

                if (detectedEmotion != null) {
                    Log.d(TAG, "Emotion detected: $detectedEmotion")

                    // Create EmotionModel on Default dispatcher (CPU-bound work)
                    val emotionModel = withContext(Dispatchers.Default) {
                        createEmotionModelFromDetection(detectedEmotion)
                    }

                    // Log the emotion to database (IO operation)
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

            // Verificar estado de la red antes de guardar
            val isOnline = (repository as? OfflineFirstEmotionRepository)?.isNetworkAvailable() ?: true

            val emotionLog = emotionLogFactory.createEmotionLog(userId, listOf(emotionModel))
            val result = repository.logEmotions(userId, emotionLog)

            if (result.isSuccess) {
                Log.d(TAG, "Successfully logged detected emotion")
                _uiState.value = UiState.Success
                _event.emit(Event.EmotionDetected(emotionModel.name))

                // Mostrar mensaje apropiado según el estado de la red
                if (isOnline) {
                    _event.emit(Event.ShowSuccess("Emoción registrada exitosamente"))
                } else {
                    _event.emit(Event.ShowOfflineSuccess("Emoción guardada. Se sincronizará cuando vuelva la conexión"))
                }
                
                // Check Emotion Explorer achievement
                val database = com.g22.orbitsoundkotlin.data.local.AppDatabase.getInstance(context)
                val achievementRepo = com.g22.orbitsoundkotlin.data.repositories.AchievementRepository(database)
                val achievementService = com.g22.orbitsoundkotlin.services.AchievementService.getInstance(context, achievementRepo)
                achievementService.checkEmotionExplorer(userId)

                // Notify navigation - the emotion is saved (locally if offline, will sync later)
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

                // Verificar estado de la red antes de guardar
                val isOnline = (repository as? OfflineFirstEmotionRepository)?.isNetworkAvailable() ?: true

                val emotionLog = emotionLogFactory.createEmotionLog(userId, selectedEmotions)
                Log.d(TAG, "Created emotion log, calling repository")
                val result = repository.logEmotions(userId, emotionLog)

                if (result.isSuccess) {
                    Log.d(TAG, "Successfully logged emotions (saved locally, will sync to cloud when online)")
                    _uiState.value = UiState.Success

                    // Mostrar mensaje apropiado según el estado de la red
                    if (isOnline) {
                        _event.emit(Event.ShowSuccess("Emotions successfully logged"))
                    } else {
                        _event.emit(Event.ShowOfflineSuccess("Emotions saved. They will be synchronized when back online"))
                    }
                    
                    // Check Emotion Explorer achievement
                    val database = com.g22.orbitsoundkotlin.data.local.AppDatabase.getInstance(context)
                    val achievementRepo = com.g22.orbitsoundkotlin.data.repositories.AchievementRepository(database)
                    val achievementService = com.g22.orbitsoundkotlin.services.AchievementService.getInstance(context, achievementRepo)
                    achievementService.checkEmotionExplorer(userId)

                    // Navigate immediately - data is saved locally and will sync in background
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
        data class ShowSuccess(val message: String) : Event()
        data class ShowOfflineSuccess(val message: String) : Event()
        data class EmotionDetected(val emotion: String) : Event()
        object NavigateNext : Event()
    }
}