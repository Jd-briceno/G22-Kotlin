package com.g22.orbitsoundkotlin.ui.screens.activitystats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsViewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsPeriod
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsViewModelFactory
import kotlinx.coroutines.launch

/**
 * Pantalla principal de Activity Stats (una vez desbloqueada).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityStatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ActivityStatsViewModel = viewModel(
        factory = ActivityStatsViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedEntryForView by remember { mutableStateOf<com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF010B19),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color(0xFF010B19))
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header
                ActivityStatsHeader()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Card elegante para nueva entrada (siempre visible)
                NewEntryCard(
                    text = uiState.editorText,
                    characterCount = uiState.editorCharacterCount,
                    maxCharacters = uiState.maxJournalCharacters,
                    isSaving = uiState.isSavingEntry,
                    isEditing = uiState.isEditingEntry,
                    onTextChange = viewModel::updateEditorText,
                    onSave = viewModel::saveEntry,
                    onCancel = viewModel::closeEntryEditor,
                    onStartEditing = { viewModel.openNewEntryEditor() }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Navegación rápida con flechas
                JournalDateSelector(
                    selectedDate = uiState.selectedDate,
                    availableDates = uiState.availableDates,
                    onPreviousDay = { viewModel.navigateToPreviousDay() },
                    onNextDay = { viewModel.navigateToNextDay() }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Journal Timeline Calendar (carga lazy cuando se muestra la pantalla)
                LaunchedEffect(Unit) {
                    viewModel.loadJournalTimeline()
                }
                
                JournalTimelineCalendar(
                    timeline = uiState.journalTimeline,
                    selectedDate = uiState.selectedTimelineDate,
                    isLoading = uiState.isLoadingTimeline,
                    onDateSelected = viewModel::selectTimelineDate
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Entradas del día seleccionado (abajo del calendario)
                if (uiState.entriesOfSelectedDate.isNotEmpty()) {
                    Text(
                        text = "Entries for selected day",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                JournalEntriesList(
                    entries = uiState.entriesOfSelectedDate,
                    onEntryClick = { entry ->
                        selectedEntryForView = entry
                    },
                    onEdit = { viewModel.openEditEntryEditor(it.id) },
                    onDelete = { entry ->
                        viewModel.deleteEntry(entry.id)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Bottom sheet para ver entrada completa
            if (selectedEntryForView != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedEntryForView = null },
                    sheetState = bottomSheetState,
                    containerColor = Color(0xFF24292E)
                ) {
                    JournalEntryBottomSheet(
                        entry = selectedEntryForView!!,
                        onEdit = { entry ->
                            selectedEntryForView = null
                            viewModel.openEditEntryEditor(entry.id)
                        },
                        onDelete = { entry ->
                            selectedEntryForView = null
                            viewModel.deleteEntry(entry.id)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Entry deleted")
                            }
                        },
                        onDismiss = { selectedEntryForView = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStatsHeader() {
    Text(
        text = "Journaling Therapy",
        style = TextStyle(
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    )
}

@Composable
private fun JournalTimelineCalendar(
    timeline: List<com.g22.orbitsoundkotlin.data.repositories.ActivityStatsRepository.JournalDaySummary>,
    selectedDate: Long,
    isLoading: Boolean,
    onDateSelected: (Long) -> Unit
) {
    Column {
        Text(
            text = "Journal Calendar",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF24292E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Loading calendar...",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        } else if (timeline.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF24292E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No journal entries yet. Start writing to see your calendar.",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        } else {
            // Calendario mensual
            val calendar = java.util.Calendar.getInstance()
            val currentMonth = calendar.get(java.util.Calendar.MONTH)
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            
            // Filtrar timeline para el mes actual
            val currentMonthTimeline = timeline.filter { day ->
                val dayCalendar = java.util.Calendar.getInstance()
                dayCalendar.timeInMillis = day.dateTimestamp
                dayCalendar.get(java.util.Calendar.MONTH) == currentMonth &&
                dayCalendar.get(java.util.Calendar.YEAR) == currentYear
            }
            
            val countMap = currentMonthTimeline.associate { 
                val dayCalendar = java.util.Calendar.getInstance()
                dayCalendar.timeInMillis = it.dateTimestamp
                dayCalendar.get(java.util.Calendar.DAY_OF_MONTH) to it.entryCount
            }
            
            // Construir calendario del mes actual
            val monthCalendar = java.util.Calendar.getInstance()
            monthCalendar.set(currentYear, currentMonth, 1)
            val firstDayOfWeek = monthCalendar.get(java.util.Calendar.DAY_OF_WEEK)
            val daysInMonth = monthCalendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            
            // Nombres de días de la semana
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF24292E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header del mes
                    val monthNames = listOf("January", "February", "March", "April", "May", "June",
                                           "July", "August", "September", "October", "November", "December")
                    Text(
                        text = "${monthNames[currentMonth]} $currentYear",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        dayNames.forEach { dayName ->
                            Text(
                                text = dayName,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Días del mes
                    var dayCounter = 1
                    var weekCounter = 0
                    
                    while (dayCounter <= daysInMonth || weekCounter == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayOfWeek in 1..7) {
                                if (weekCounter == 0 && dayOfWeek < firstDayOfWeek) {
                                    // Espacios vacíos antes del primer día
                                    Spacer(modifier = Modifier.weight(1f))
                                } else if (dayCounter <= daysInMonth) {
                                    val dayNumber = dayCounter
                                    val entryCount = countMap[dayNumber] ?: 0
                                    val isSelected = isSameDay(
                                        dayNumber,
                                        currentMonth,
                                        currentYear,
                                        selectedDate
                                    )
                                    
                                    // Calcular timestamp del día
                                    val dayCalendar = java.util.Calendar.getInstance()
                                    dayCalendar.set(currentYear, currentMonth, dayNumber)
                                    dayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                    dayCalendar.set(java.util.Calendar.MINUTE, 0)
                                    dayCalendar.set(java.util.Calendar.SECOND, 0)
                                    dayCalendar.set(java.util.Calendar.MILLISECOND, 0)
                                    val dayTimestamp = dayCalendar.timeInMillis
                                    
                                    CalendarDayCell(
                                        dayNumber = dayNumber,
                                        entryCount = entryCount,
                                        isSelected = isSelected,
                                        onClick = { onDateSelected(dayTimestamp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    dayCounter++
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        weekCounter++
                        if (dayCounter > daysInMonth) break
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNumber: Int,
    entryCount: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF5099BA) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) Color(0xFF5099BA).copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayNumber.toString(),
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF5099BA) else Color.White
                )
            )
            
            if (entryCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF5099BA),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = entryCount.toString(),
                        style = TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

private fun isSameDay(dayNumber: Int, month: Int, year: Int, timestamp: Long): Boolean {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return calendar.get(java.util.Calendar.DAY_OF_MONTH) == dayNumber &&
           calendar.get(java.util.Calendar.MONTH) == month &&
           calendar.get(java.util.Calendar.YEAR) == year
}

@Composable
private fun NewEntryCard(
    text: String,
    characterCount: Int,
    maxCharacters: Int,
    isSaving: Boolean,
    isEditing: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onStartEditing: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF5099BA).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = Color(0xFF5099BA).copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (!isEditing) {
                // Estado inicial: mostrar placeholder y botón para empezar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onStartEditing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = Color(0xFF5099BA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Write a new entry...",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF5099BA).copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                // Estado de edición: mostrar TextField y controles
                Text(
                    text = "New Entry",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5099BA)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = {
                        Text(
                            text = "What's on your mind?",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF010B19).copy(alpha = 0.5f),
                        unfocusedContainerColor = Color(0xFF010B19).copy(alpha = 0.5f),
                        focusedIndicatorColor = Color(0xFF5099BA),
                        unfocusedIndicatorColor = Color(0xFF5099BA).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF5099BA)
                    ),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$characterCount/$maxCharacters",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color(0xFF5099BA).copy(alpha = 0.7f)
                        )
                    )
                    
                    Row {
                        TextButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF5099BA).copy(alpha = 0.7f)
                            )
                        ) {
                            Text("Cancel", style = TextStyle(fontSize = 14.sp))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = onSave,
                            enabled = text.trim().isNotEmpty() && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5099BA),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF5099BA).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isSaving) {
                                Text("Saving...", style = TextStyle(fontSize = 14.sp))
                            } else {
                                Text("Save", style = TextStyle(fontSize = 14.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalDateSelector(
    selectedDate: Long,
    availableDates: List<Long>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    calendar.timeInMillis = selectedDate
    val today = java.util.Calendar.getInstance()
    val yesterday = java.util.Calendar.getInstance().apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    
    val dateText = when {
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
    
    val sortedDates = availableDates.sorted()
    val currentIndex = sortedDates.indexOf(selectedDate)
    val canGoPrevious = currentIndex > 0
    val canGoNext = currentIndex >= 0 && currentIndex < sortedDates.size - 1
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousDay,
            enabled = canGoPrevious
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Previous day",
                tint = if (canGoPrevious) Color(0xFF5099BA) else Color.White.copy(alpha = 0.3f)
            )
        }
        
        Text(
            text = dateText,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        IconButton(
            onClick = onNextDay,
            enabled = canGoNext
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Next day",
                tint = if (canGoNext) Color(0xFF5099BA) else Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun JournalEntriesList(
    entries: List<com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry>,
    onEntryClick: (com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry) -> Unit,
    onEdit: (com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry) -> Unit,
    onDelete: (com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF24292E)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "No entries for this day. Tap \"+ New entry\" to create one.",
                modifier = Modifier.padding(16.dp),
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            )
        }
    } else {
        entries.forEach { entry ->
            JournalEntryListItem(
                entry = entry,
                onClick = { onEntryClick(entry) },
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) }
            )
        }
    }
}

@Composable
private fun JournalEntryListItem(
    entry: com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF24292E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.time,
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
                
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF5099BA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (entry.text.length > 150) {
                    entry.text.take(150) + "..."
                } else {
                    entry.text
                },
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                ),
                modifier = Modifier.clickable(onClick = onClick),
                maxLines = 4
            )
        }
    }
}

@Composable
private fun JournalEntryEditor(
    text: String,
    characterCount: Int,
    maxCharacters: Int,
    isSaving: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF24292E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (text.isEmpty()) "New Entry" else "Edit Entry",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = {
                        Text(
                            text = "Write your thoughts...",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF010B19),
                        unfocusedContainerColor = Color(0xFF010B19),
                        focusedIndicatorColor = Color(0xFF5099BA),
                        unfocusedIndicatorColor = Color(0xFF5099BA).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF5099BA)
                    ),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White,
                        lineHeight = 20.sp
                    ),
                    maxLines = 10
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$characterCount/$maxCharacters",
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                    
                    Row {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = onSave,
                            enabled = text.trim().isNotEmpty() && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5099BA),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (isSaving) "Saving..." else "Save",
                                style = TextStyle(fontSize = 14.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryBottomSheet(
    entry: com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry,
    onEdit: (com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry) -> Unit,
    onDelete: (com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = entry.date,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = entry.time,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
            
            Row {
                TextButton(onClick = { onEdit(entry) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = Color(0xFF5099BA),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = Color(0xFF5099BA))
                }
                
                TextButton(onClick = { onDelete(entry) }) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = Color(0xFFFF5252))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = entry.text,
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 22.sp
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5099BA),
                contentColor = Color.White
            )
        ) {
            Text("Close")
        }
    }
}

private fun isSameDay(cal1: java.util.Calendar, cal2: java.util.Calendar): Boolean {
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

/**
 * Formatea minutos en formato legible (ej. "2h 15m").
 */
private fun formatTime(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

