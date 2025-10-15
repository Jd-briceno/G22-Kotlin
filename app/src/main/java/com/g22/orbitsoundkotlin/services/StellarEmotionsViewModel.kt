
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.auth.AuthService
import com.g22.orbitsoundkotlin.data.EmotionRepository
import com.g22.orbitsoundkotlin.models.EmotionLog
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.StellarEmotionsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StellarEmotionsViewModel @Inject constructor(
    private val repository: EmotionRepository,

    ) : ViewModel() {
    private val _uiState = MutableStateFlow(StellarEmotionsUiState())
    val uiState: StateFlow<StellarEmotionsUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun updateSelectedEmotions(emotions: List<EmotionModel>) {
        _uiState.update { it.copy(selectedEmotions = emotions) }
    }

    fun submitEmotions() {
        val emotions = _uiState.value.selectedEmotions.map { it.id } // store ids
        val log = EmotionLog(
            emotions = emotions,
            clientTs = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = repository.logEmotions(log)

            _uiState.update { it.copy(isLoading = false) }
            result.fold(
                onSuccess = {
                    _events.send(UiEvent.Success("Emotions logged"))
                    _uiState.update { it.copy(lastSubmittedAt = System.currentTimeMillis()) }
                },
                onFailure = { e ->
                    _events.send(UiEvent.ShowError(e.localizedMessage ?: "Failed to log emotions"))
                }
            )
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String): UiEvent()
        data class Success(val message: String): UiEvent()
        object NavigateToConstellations: UiEvent()
    }
}
