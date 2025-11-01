package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.repositories.InterestsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para la selección de intereses musicales.
 * Usa InterestsRepository con versionado y outbox pattern.
 */
class InterestsViewModel(
    private val interestsRepository: InterestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestsUiState())
    val uiState: StateFlow<InterestsUiState> = _uiState.asStateFlow()

    /**
     * Carga intereses del usuario al inicializar.
     */
    fun loadInterests(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val interests = interestsRepository.getInterests(userId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedInterests = interests ?: emptyList(),
                    error = null
                )
            }
        }
    }

    /**
     * Guarda intereses seleccionados localmente y encola sincronización.
     */
    fun saveInterests(userId: String, interests: List<String>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    error = null
                )
            }

            try {
                interestsRepository.saveInterests(userId, interests)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        selectedInterests = interests,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Error al guardar intereses"
                    )
                }
            }
        }
    }

    /**
     * Consume el evento de éxito después de guardar.
     */
    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    data class InterestsUiState(
        val selectedInterests: List<String> = emptyList(),
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val error: String? = null
    )
}

