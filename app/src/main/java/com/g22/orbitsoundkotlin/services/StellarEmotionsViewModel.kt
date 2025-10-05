// Presentation/StellarEmotionsViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.models.EmotionModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Use @HiltViewModel if you are using Hilt for dependency injection
@HiltViewModel
class StellarEmotionsViewModel @Inject constructor(
    private val repository: EmotionRepository,
//    private val userSessionManager: UserSessionManager
) : ViewModel() {

    // Still use a sealed class for one-time UI events (like showing a Toast/Snackbar)
    sealed class Event {
        data class ShowError(val message: String) : Event()
        // Removed ShipComplete event, as navigation is now handled in the Composable
    }

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    fun onReadyToShipClicked(emotionsData: List<EmotionModel>) {
        viewModelScope.launch { // Launch a coroutine for the background save
            // The UI will continue execution and navigate immediately after this line.

//            val userEmail = userSessionManager.getUserEmail() ?: run {
//                _event.emit(Event.ShowError("User email not found. Submission aborted."))
//                return@launch
//            }

            val submission = EmotionSubmission(
//                userEmail = userEmail,
                emotions = emotionsData
            )

            // Execute save operation and handle result using onSuccess/onFailure
            repository.saveEmotionData(submission)
                .onSuccess {
                    // Optional: Log success, but do NOT trigger navigation
                    println("Emotion data saved successfully to Firebase!")
                }
                .onFailure { exception ->
                    // Only emit an error event, not a navigation event
                    _event.emit(Event.ShowError("Submission failed: ${exception.message}"))
                }
        }
    }
}