// Updated StellarEmotionsScreen.kt

package com.example.stellar.ui

import StellarEmotionsViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.g22.orbitsoundkotlin.models.EmotionModel

@Composable
fun StellarEmotionsScreen(
    username: String,
    onNavigateToConstellations: () -> Unit,
    viewModel: StellarEmotionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Listen to one-off UI events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StellarEmotionsViewModel.UiEvent.ShowError ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is StellarEmotionsViewModel.UiEvent.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    onNavigateToConstellations()
                }
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hi $username! How are you feeling today?",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Replace old local selectedEmotions state with ViewModel-managed state
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(EmotionData.emotions.size) { index ->
                val emotion = EmotionData.emotions[index]
                val isSelected = uiState.selectedEmotions.contains(emotion)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            val updatedList = if (checked) {
                                (uiState.selectedEmotions + emotion).take(3) // limit to 3
                            } else {
                                uiState.selectedEmotions - emotion
                            }
                            viewModel.updateSelectedEmotions(updatedList)
                        }
                    )
                    Text(text = emotion.name)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.submitEmotions()
            },
            enabled = uiState.selectedEmotions.isNotEmpty() && !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Ready to ship?")
            }
        }
    }
}

// Simple static emotion data (you can keep your existing EmotionModel data class)
object EmotionData {
    val emotions = listOf(
        EmotionModel("Happy"),
        EmotionModel("Sad"),
        EmotionModel("Excited"),
        EmotionModel("Anxious"),
        EmotionModel("Relaxed"),
        EmotionModel("Angry")
    )
}
