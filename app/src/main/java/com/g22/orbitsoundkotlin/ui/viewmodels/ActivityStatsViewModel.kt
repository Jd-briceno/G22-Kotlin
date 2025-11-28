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
    
    // Journal state
    val selectedDate: Long = System.currentTimeMillis(), // Timestamp del día seleccionado
    val entriesOfSelectedDate: List<JournalEntry> = emptyList(),
    val availableDates: List<Long> = emptyList(), // Fechas que tienen entradas
    
    // Journal editor state
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

class ActivityStatsViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(ActivityStatsUiState())
    val uiState: StateFlow<ActivityStatsUiState> = _uiState.asStateFlow()
    
    // Dummy storage: mapa de fecha (timestamp del inicio del día) -> lista de entradas
    private val journalEntriesByDate = mutableMapOf<Long, MutableList<JournalEntry>>()
    
    init {
        // TODO: Cargar datos reales desde Repository cuando se implemente la FEATURE
        loadDummyData()
        initializeDummyJournalEntries()
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
     * Selecciona una fecha para mostrar sus entradas.
     */
    fun selectDate(dateTimestamp: Long) {
        val dayStart = getDayStartTimestamp(dateTimestamp)
        _uiState.update {
            it.copy(
                selectedDate = dayStart,
                entriesOfSelectedDate = journalEntriesByDate[dayStart]?.toList() ?: emptyList()
            )
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
            
            // Simular guardado
            kotlinx.coroutines.delay(300)
            
            if (editingEntryId != null) {
                // Editar entrada existente
                updateEntry(selectedDate, editingEntryId, text)
            } else {
                // Crear nueva entrada
                addEntry(selectedDate, text)
            }
            
            // Actualizar estado
            val entries = journalEntriesByDate[selectedDate]?.toList() ?: emptyList()
            val availableDates = journalEntriesByDate.keys.toList()
            
            _uiState.update {
                it.copy(
                    isSavingEntry = false,
                    isEditingEntry = false,
                    editingEntryId = null,
                    editorText = "",
                    editorCharacterCount = 0,
                    entriesOfSelectedDate = entries,
                    availableDates = availableDates
                )
            }
        }
    }
    
    /**
     * Elimina una entrada.
     */
    fun deleteEntry(entryId: String) {
        val selectedDate = _uiState.value.selectedDate
        val entries = journalEntriesByDate[selectedDate] ?: return
        
        entries.removeAll { it.id == entryId }
        
        // Si no quedan entradas, remover la fecha de availableDates
        if (entries.isEmpty()) {
            journalEntriesByDate.remove(selectedDate)
        }
        
        // Actualizar estado
        val updatedEntries = journalEntriesByDate[selectedDate]?.toList() ?: emptyList()
        val availableDates = journalEntriesByDate.keys.toList()
        
        _uiState.update {
            it.copy(
                entriesOfSelectedDate = updatedEntries,
                availableDates = availableDates
            )
        }
    }
    
    /**
     * Agrega una nueva entrada al día especificado.
     */
    private fun addEntry(dateTimestamp: Long, text: String) {
        val dayStart = getDayStartTimestamp(dateTimestamp)
        val entryId = "entry_${System.currentTimeMillis()}"
        val timestamp = System.currentTimeMillis()
        
        val entry = JournalEntry(
            id = entryId,
            date = formatDate(dayStart),
            text = text,
            timestamp = timestamp,
            time = formatTime(timestamp)
        )
        
        val entries = journalEntriesByDate.getOrPut(dayStart) { mutableListOf() }
        entries.add(entry)
        entries.sortByDescending { it.timestamp } // Más recientes primero
    }
    
    /**
     * Actualiza una entrada existente.
     */
    private fun updateEntry(dateTimestamp: Long, entryId: String, newText: String) {
        val dayStart = getDayStartTimestamp(dateTimestamp)
        val entries = journalEntriesByDate[dayStart] ?: return
        
        val index = entries.indexOfFirst { it.id == entryId }
        if (index >= 0) {
            val oldEntry = entries[index]
            entries[index] = oldEntry.copy(
                text = newText,
                timestamp = System.currentTimeMillis() // Actualizar timestamp
            )
            entries.sortByDescending { it.timestamp }
        }
    }
    
    /**
     * Inicializa algunas entradas dummy para desarrollo.
     */
    private fun initializeDummyJournalEntries() {
        val today = System.currentTimeMillis()
        val yesterday = today - (24 * 60 * 60 * 1000L)
        val twoDaysAgo = yesterday - (24 * 60 * 60 * 1000L)
        
        // Entrada de hoy
        addEntry(today, "Today I discovered some amazing lofi tracks that helped me focus while studying. The ambient sounds really improved my productivity.")
        
        // Entrada de ayer
        addEntry(yesterday, "Yesterday was a great day for music discovery. Found several new artists that I'm excited to explore more.")
        
        // Entrada de hace dos días
        addEntry(twoDaysAgo, "Started the week with a fresh playlist. Music really sets the mood for the day ahead.")
        
        // Actualizar estado inicial
        val dayStart = getDayStartTimestamp(today)
        val entries = journalEntriesByDate[dayStart]?.toList() ?: emptyList()
        val availableDates = journalEntriesByDate.keys.toList()
        
        _uiState.update {
            it.copy(
                selectedDate = dayStart,
                entriesOfSelectedDate = entries,
                availableDates = availableDates
            )
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
    
    /**
     * Formatea una fecha para mostrar.
     */
    private fun formatDate(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val today = java.util.Calendar.getInstance()
        val yesterday = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        
        return when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, yesterday) -> "Yesterday"
            else -> {
                val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val month = calendar.get(java.util.Calendar.MONTH)
                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${dayNames[dayOfWeek - 1]} · ${monthNames[month]} $day"
            }
        }
    }
    
    /**
     * Formatea una hora para mostrar.
     */
    private fun formatTime(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
    
    /**
     * Verifica si dos calendarios representan el mismo día.
     */
    private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
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
            )
        }
    }
}

