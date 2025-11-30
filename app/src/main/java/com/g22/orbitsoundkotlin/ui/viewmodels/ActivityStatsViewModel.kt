package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.repositories.ActivityStatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel para ActivityStatsScreen.
 * 
 * Maneja el estado de la UI y la lógica de presentación.
 * Usa ActivityStatsRepository para obtener datos reales desde Room.
 * 
 * CONCURRENCIA: Todas las operaciones pesadas corren en viewModelScope.launch
 * (el Repository maneja Dispatchers.IO internamente)
 */
data class ActivityStatsUiState(
    val selectedPeriod: ActivityStatsPeriod = ActivityStatsPeriod.LAST_24H,
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Journal Timeline
    val journalTimeline: List<ActivityStatsRepository.JournalDaySummary> = emptyList(),
    val selectedTimelineDate: Long = System.currentTimeMillis(),
    val isLoadingTimeline: Boolean = false,
    
    // Recent activity (mantener)
    val recentActivities: List<ActivityItem> = emptyList(),
    
    // Journal state (mantener)
    val selectedDate: Long = System.currentTimeMillis(), // Timestamp del día seleccionado
    val entriesOfSelectedDate: List<JournalEntry> = emptyList(),
    val availableDates: List<Long> = emptyList(), // Fechas que tienen entradas
    
    // Journal editor state (mantener)
    val isEditingEntry: Boolean = false,
    val editingEntryId: String? = null,
    val editorText: String = "",
    val editorCharacterCount: Int = 0,
    val maxJournalCharacters: Int = 300,
    val isSavingEntry: Boolean = false
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
    val timestamp: Long,
    val time: String // Formato: "8:03 PM"
)

class ActivityStatsViewModel(
    private val repository: ActivityStatsRepository,
    private val userId: String,
    private val userEmail: String?
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActivityStatsUiState())
    val uiState: StateFlow<ActivityStatsUiState> = _uiState.asStateFlow()
    
    private val TAG = "ActivityStatsViewModel"
    
    init {
        // Cargar datos iniciales (NO timeline en init para evitar crashes)
        viewModelScope.launch {
            try {
                loadRecentActivity(ActivityStatsPeriod.LAST_24H)
                loadJournalForDate(System.currentTimeMillis())
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading data"
                    )
                }
            }
        }
    }
    
    /**
     * Carga la timeline de los últimos 30 días.
     * Se llama explícitamente desde la UI, NO en init.
     */
    fun loadJournalTimeline() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingTimeline = true) }
            try {
                val timeline = repository.getJournalTimeline(userId, daysBack = 30)
                _uiState.update {
                    it.copy(
                        journalTimeline = timeline,
                        isLoadingTimeline = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading journal timeline", e)
                _uiState.update {
                    it.copy(
                        journalTimeline = emptyList(),
                        isLoadingTimeline = false
                    )
                }
            }
        }
    }
    
    /**
     * Selecciona un día desde la timeline y carga sus entradas.
     */
    fun selectTimelineDate(dateTimestamp: Long) {
        _uiState.update {
            it.copy(selectedTimelineDate = dateTimestamp, selectedDate = dateTimestamp)
        }
        viewModelScope.launch {
            loadJournalForDate(dateTimestamp)
        }
    }
    
    /**
     * Cambia el período seleccionado y recarga datos.
     */
    fun selectPeriod(period: ActivityStatsPeriod) {
        _uiState.update { 
            it.copy(
                selectedPeriod = period,
                isLoading = true,
                error = null
            )
        }
        
        viewModelScope.launch {
            try {
                loadRecentActivity(period)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data for period: ${period.displayName}", e)
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading data"
                    )
                }
            }
        }
    }
    
    
    /**
     * Carga la actividad reciente para un período.
     */
    private suspend fun loadRecentActivity(period: ActivityStatsPeriod) {
        try {
            val activities = repository.getRecentActivity(userId, userEmail, period)
            _uiState.update {
                it.copy(
                    recentActivities = activities,
                    isLoading = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading recent activity", e)
            throw e
        }
    }
    
    /**
     * Selecciona una fecha para mostrar sus entradas.
     */
    fun selectDate(dateTimestamp: Long) {
        _uiState.update {
            it.copy(
                selectedDate = getDayStartTimestamp(dateTimestamp),
                isLoading = true
            )
        }
        
        viewModelScope.launch {
            try {
                loadJournalForDate(dateTimestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading journal for date", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading journal"
                    )
                }
            }
        }
    }
    
    /**
     * Carga las entradas del journal para una fecha.
     */
    private suspend fun loadJournalForDate(dateTimestamp: Long) {
        try {
            val entries = repository.getJournalEntriesForDate(userId, dateTimestamp)
            val availableDates = repository.getAvailableJournalDates(userId)
            
            _uiState.update {
                it.copy(
                    entriesOfSelectedDate = entries,
                    availableDates = availableDates,
                    isLoading = false
                )
            }
            Log.d(TAG, "✅ Journal loaded: ${entries.size} entries for date")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading journal", e)
            throw e
        }
    }
    
    /**
     * Navega al día anterior que tenga entradas.
     */
    fun navigateToPreviousDay() {
        val currentDate = _uiState.value.selectedDate
        val availableDates = _uiState.value.availableDates.sorted()
        val currentIndex = availableDates.indexOf(currentDate)
        
        if (currentIndex > 0) {
            selectDate(availableDates[currentIndex - 1])
        }
    }
    
    /**
     * Navega al día siguiente que tenga entradas.
     */
    fun navigateToNextDay() {
        val currentDate = _uiState.value.selectedDate
        val availableDates = _uiState.value.availableDates.sorted()
        val currentIndex = availableDates.indexOf(currentDate)
        
        if (currentIndex >= 0 && currentIndex < availableDates.size - 1) {
            selectDate(availableDates[currentIndex + 1])
        }
    }
    
    /**
     * Abre el editor para crear una nueva entrada.
     */
    fun openNewEntryEditor() {
        _uiState.update {
            it.copy(
                isEditingEntry = true,
                editingEntryId = null,
                editorText = "",
                editorCharacterCount = 0
            )
        }
    }
    
    /**
     * Abre el editor para editar una entrada existente.
     */
    fun openEditEntryEditor(entryId: String) {
        val entry = _uiState.value.entriesOfSelectedDate.find { it.id == entryId }
        entry?.let {
            _uiState.update {
                it.copy(
                    isEditingEntry = true,
                    editingEntryId = entryId,
                    editorText = entry.text,
                    editorCharacterCount = entry.text.length
                )
            }
        }
    }
    
    /**
     * Cierra el editor sin guardar.
     */
    fun closeEntryEditor() {
        _uiState.update {
            it.copy(
                isEditingEntry = false,
                editingEntryId = null,
                editorText = "",
                editorCharacterCount = 0
            )
        }
    }
    
    /**
     * Actualiza el texto del editor.
     */
    fun updateEditorText(text: String) {
        val trimmed = text.take(300) // Limitar a 300 caracteres
        _uiState.update {
            it.copy(
                editorText = trimmed,
                editorCharacterCount = trimmed.length
            )
        }
    }
    
    /**
     * Guarda la entrada (nueva o editada).
     */
    fun saveEntry() {
        val text = _uiState.value.editorText.trim()
        if (text.isEmpty()) return
        
        val selectedDate = _uiState.value.selectedDate
        val editingEntryId = _uiState.value.editingEntryId
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingEntry = true) }
            
            try {
                if (editingEntryId != null) {
                    // Editar entrada existente
                    val success = repository.updateJournalEntry(userId, editingEntryId, text)
                    if (!success) {
                        throw Exception("Failed to update entry")
                    }
                    Log.d(TAG, "✅ Journal entry updated: $editingEntryId")
                } else {
                    // Crear nueva entrada
                    repository.addJournalEntry(userId, selectedDate, text)
                    Log.d(TAG, "✅ Journal entry created")
                }
                
                // Recargar entradas del día y timeline
                loadJournalForDate(selectedDate)
                loadJournalTimeline()
                
                _uiState.update {
                    it.copy(
                        isSavingEntry = false,
                        isEditingEntry = false,
                        editingEntryId = null,
                        editorText = "",
                        editorCharacterCount = 0
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving journal entry", e)
                _uiState.update {
                    it.copy(
                        isSavingEntry = false,
                        error = e.message ?: "Error saving entry"
                    )
                }
            }
        }
    }
    
    /**
     * Elimina una entrada.
     */
    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteJournalEntry(userId, entryId)
                if (!success) {
                    throw Exception("Failed to delete entry")
                }
                
                Log.d(TAG, "✅ Journal entry deleted: $entryId")
                
                // Recargar entradas del día y timeline
                val selectedDate = _uiState.value.selectedDate
                loadJournalForDate(selectedDate)
                loadJournalTimeline()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting journal entry", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Error deleting entry")
                }
            }
        }
    }
    
    
    /**
     * Obtiene el timestamp del inicio del día (00:00:00).
     */
    private fun getDayStartTimestamp(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
}

