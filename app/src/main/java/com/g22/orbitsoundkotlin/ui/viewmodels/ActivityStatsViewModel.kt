package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para ActivityStatsScreen.
 * 
 * Maneja el estado de la UI y la lógica de presentación.
 * La lógica de negocio (procesamiento de datos) se implementará en el bloque FEATURE.
 */
data class ActivityStatsUiState(
    val selectedPeriod: ActivityStatsPeriod = ActivityStatsPeriod.LAST_24H,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Stats simples (dummy data por ahora - se conectará a FEATURE)
    val sessionsCount: Int = 0,
    val totalTimeMinutes: Int = 0,
    val mostCommonAction: String = "",
    
    // Recent activity (dummy data por ahora)
    val recentActivities: List<ActivityItem> = emptyList(),
    
    // Today's journal
    val todaysJournalText: String = "",
    val journalCharacterCount: Int = 0,
    val maxJournalCharacters: Int = 300,
    val isSavingJournal: Boolean = false,
    val journalSaved: Boolean = false,
    
    // Previous entries (dummy data por ahora)
    val previousEntries: List<JournalEntry> = emptyList()
)

/**
 * Períodos de actividad para la vista ActivityStatsScreen.
 * Diferente de ActivityPeriod (usado en CaptainsLogViewModel para la FEATURE).
 */
enum class ActivityStatsPeriod(val displayName: String) {
    LAST_24H("Last 24h"),
    LAST_7_DAYS("Last 7 days"),
    LAST_30_DAYS("Last 30 days")
}

data class ActivityItem(
    val id: String,
    val dateTime: String, // Formato: "Tue · 8:03 PM"
    val summary: String,
    val stats: List<ActivityStatChip> // Ej: "3 searches", "2 plays"
)

data class ActivityStatChip(
    val label: String,
    val value: Int
)

data class JournalEntry(
    val id: String,
    val date: String, // Formato: "Mon · Nov 24"
    val text: String,
    val timestamp: Long
)

class ActivityStatsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActivityStatsUiState())
    val uiState: StateFlow<ActivityStatsUiState> = _uiState.asStateFlow()
    
    init {
        // TODO: Cargar datos reales desde Repository cuando se implemente la FEATURE
        loadDummyData()
    }
    
    /**
     * Cambia el período seleccionado.
     */
    fun selectPeriod(period: ActivityStatsPeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        // TODO: Recargar datos para el nuevo período desde Repository
        loadDummyData()
    }
    
    /**
     * Actualiza el texto del diario de hoy.
     */
    fun updateJournalText(text: String) {
        val trimmed = text.take(300) // Limitar a 300 caracteres
        _uiState.update {
            it.copy(
                todaysJournalText = trimmed,
                journalCharacterCount = trimmed.length,
                journalSaved = false
            )
        }
    }
    
    /**
     * Guarda la entrada del diario de hoy.
     */
    fun saveTodaysJournal() {
        val text = _uiState.value.todaysJournalText.trim()
        if (text.isEmpty()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingJournal = true) }
            
            // TODO: Guardar en Repository cuando se implemente la FEATURE
            // Por ahora, simulamos guardado
            kotlinx.coroutines.delay(500)
            
            _uiState.update {
                it.copy(
                    isSavingJournal = false,
                    journalSaved = true,
                    todaysJournalText = "", // Limpiar después de guardar
                    journalCharacterCount = 0
                )
            }
            
            // Recargar entradas previas
            loadDummyData()
        }
    }
    
    /**
     * Carga datos dummy para desarrollo.
     * TODO: Reemplazar con carga real desde Repository cuando se implemente la FEATURE.
     */
    private fun loadDummyData() {
        _uiState.update {
            it.copy(
                sessionsCount = 12,
                totalTimeMinutes = 135, // 2h 15m
                mostCommonAction = "Searching music",
                recentActivities = listOf(
                    ActivityItem(
                        id = "1",
                        dateTime = "Tue · 8:03 PM",
                        summary = "You searched for 'lofi beats' and played 3 tracks",
                        stats = listOf(
                            ActivityStatChip("searches", 3),
                            ActivityStatChip("plays", 2),
                            ActivityStatChip("favorites", 1)
                        )
                    ),
                    ActivityItem(
                        id = "2",
                        dateTime = "Tue · 6:45 PM",
                        summary = "You explored recommendations and saved 2 tracks",
                        stats = listOf(
                            ActivityStatChip("recommendations", 5),
                            ActivityStatChip("saves", 2)
                        )
                    )
                ),
                previousEntries = emptyList() // Se llenará cuando haya entradas guardadas
            )
        }
    }
    
    /**
     * Limpia el mensaje de éxito del diario.
     */
    fun clearJournalSavedMessage() {
        _uiState.update { it.copy(journalSaved = false) }
    }
}

