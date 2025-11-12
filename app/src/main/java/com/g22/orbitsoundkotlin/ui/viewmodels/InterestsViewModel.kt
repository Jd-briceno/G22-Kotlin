package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.repositories.InterestsRepository
import com.g22.orbitsoundkotlin.data.UserPreferencesRepository
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
    private val interestsRepository: InterestsRepository,
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(InterestsUiState())
    val uiState: StateFlow<InterestsUiState> = _uiState.asStateFlow()

    /**
     * Carga intereses del usuario al inicializar.
     */
    // MULTI-THREADING 1.3 Una input/output y una en main
    fun loadInterests(userId: String) {
        viewModelScope.launch { //corrutina main
            _uiState.update { it.copy(isLoading = true) }

            val cachedPreferences = preferencesRepository?.getLastSelectedInterests().orEmpty()
            if (cachedPreferences.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        selectedInterests = cachedPreferences,
                        error = null
                    )
                }
            }

            val interests = interestsRepository.getInterests(userId) // parte:I/O
            val finalSelection = when {
                !interests.isNullOrEmpty() -> interests
                cachedPreferences.isNotEmpty() -> cachedPreferences
                else -> emptyList()
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedInterests = finalSelection,
                    error = null
                )
            }

            preferencesRepository?.saveLastSelectedInterests(finalSelection)
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
                preferencesRepository?.saveLastSelectedInterests(interests)

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

