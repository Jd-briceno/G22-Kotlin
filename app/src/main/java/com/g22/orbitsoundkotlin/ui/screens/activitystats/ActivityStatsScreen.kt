package com.g22.orbitsoundkotlin.ui.screens.activitystats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsViewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsPeriod
import kotlinx.coroutines.launch

/**
 * Pantalla principal de Activity Stats (una vez desbloqueada).
 */
@Composable
fun ActivityStatsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ActivityStatsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Mostrar snackbar cuando se guarda el diario
    LaunchedEffect(uiState.journalSaved) {
        if (uiState.journalSaved) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Journal saved for today.")
                viewModel.clearJournalSavedMessage()
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF010B19)
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF010B19))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Header
            ActivityStatsHeader(
                selectedPeriod = uiState.selectedPeriod,
                onPeriodSelected = viewModel::selectPeriod
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Summary Cards
            ActivityStatsSummaryCards(
                sessionsCount = uiState.sessionsCount,
                totalTimeMinutes = uiState.totalTimeMinutes,
                mostCommonAction = uiState.mostCommonAction
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Recent Activity Section
            RecentActivitySection(
                activities = uiState.recentActivities
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Today's Journal Section
            TodaysJournalSection(
                journalText = uiState.todaysJournalText,
                characterCount = uiState.journalCharacterCount,
                maxCharacters = uiState.maxJournalCharacters,
                isSaving = uiState.isSavingJournal,
                onTextChange = viewModel::updateJournalText,
                onSave = viewModel::saveTodaysJournal
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Previous Entries Section
            PreviousEntriesSection(
                entries = uiState.previousEntries
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActivityStatsHeader(
    selectedPeriod: ActivityStatsPeriod,
    onPeriodSelected: (ActivityStatsPeriod) -> Unit
) {
    Column {
        Text(
            text = "Activity Stats",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "See your recent activity and reflect on your day.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PeriodSelectorChips(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = onPeriodSelected
        )
    }
}

@Composable
private fun ActivityStatsSummaryCards(
    sessionsCount: Int,
    totalTimeMinutes: Int,
    mostCommonAction: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActivityStatCard(
            title = "Sessions in this period",
            value = sessionsCount.toString(),
            icon = Icons.Outlined.PlayArrow,
            modifier = Modifier.weight(1f)
        )
        
        ActivityStatCard(
            title = "Total time in app",
            value = formatTime(totalTimeMinutes),
            icon = Icons.Outlined.AccessTime,
            modifier = Modifier.weight(1f)
        )
        
        ActivityStatCard(
            title = "Most common action",
            value = mostCommonAction,
            icon = Icons.Outlined.MusicNote,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RecentActivitySection(
    activities: List<com.g22.orbitsoundkotlin.ui.viewmodels.ActivityItem>
) {
    Column {
        Text(
            text = "Recent activity",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (activities.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF24292E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No recent activity. Start using the app to see your stats here.",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        } else {
            activities.forEach { activity ->
                ActivityListItem(activity = activity)
            }
        }
    }
}

@Composable
private fun TodaysJournalSection(
    journalText: String,
    characterCount: Int,
    maxCharacters: Int,
    isSaving: Boolean,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column {
        Text(
            text = "Today's Journal",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Write a short reflection about today.",
            style = TextStyle(
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextField(
            value = journalText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = {
                Text(
                    text = "Today I discovered new tracks that helped me focus…",
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF24292E),
                unfocusedContainerColor = Color(0xFF24292E),
                focusedIndicatorColor = Color(0xFF5099BA),
                unfocusedIndicatorColor = Color(0xFF5099BA).copy(alpha = 0.5f),
                cursorColor = Color(0xFF5099BA)
            ),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 20.sp
            ),
            maxLines = 5
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
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
            
            Button(
                onClick = onSave,
                enabled = journalText.trim().isNotEmpty() && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5099BA),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isSaving) "Saving..." else "Save entry",
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun PreviousEntriesSection(
    entries: List<com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry>
) {
    Column {
        Text(
            text = "Previous entries",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (entries.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF24292E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "You don't have any journal entries yet. Start by writing something about today.",
                    modifier = Modifier.padding(16.dp),
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }
        } else {
            entries.forEach { entry ->
                JournalEntryCard(
                    entry = entry,
                    onClick = {
                        // TODO: Abrir bottom sheet con entrada completa
                    }
                )
            }
        }
    }
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

