package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.g22.orbitsoundkotlin.ui.theme.EncodeSansExpanded
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import com.g22.orbitsoundkotlin.ui.theme.RobotoMono

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InterestSelectionScreen(
    modifier: Modifier = Modifier,
    userId: String = "default_user", // UID del usuario
    viewModel: com.g22.orbitsoundkotlin.ui.viewmodels.InterestsViewModel? = null,
    onBack: () -> Unit = {},
    onSkip: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF010B19)
    val focusColor = Color(0xFF0095FC)
    val borderColor = Color(0xFFB4B1B8).copy(alpha = 0.3f)

    val interests = listOf(
        "Travel", "DnD session", "Workout", "Study", "Sleep", "Party",
        "Relax", "Focus", "Drive", "Chill", "Work", "Dance", "Cook",
        "Meditate", "Gaming", "Read", "Clean", "Coffee Afternoon", "Rain",
        "Sunset Vibes", "Friends", "Nature", "Love"
    )

    // ✅ CONECTIVIDAD EVENTUAL: Usar estado del ViewModel si está disponible
    val uiState = viewModel?.uiState?.collectAsState()
    val isSaving = uiState?.value?.isSaving ?: false
    
    var selectedInterests by rememberSaveable { mutableStateOf(listOf<String>()) }

    // Cargar intereses existentes al iniciar
    LaunchedEffect(userId) {
        viewModel?.loadInterests(userId)
    }
    
    // Actualizar selectedInterests cuando se carguen desde el ViewModel
    LaunchedEffect(uiState?.value?.selectedInterests) {
        val loadedInterests = uiState?.value?.selectedInterests
        if (loadedInterests != null && loadedInterests.isNotEmpty()) {
            selectedInterests = loadedInterests
        }
    }
    
    // Navegar cuando se guarde exitosamente
    LaunchedEffect(uiState?.value?.saveSuccess) {
        if (uiState?.value?.saveSuccess == true) {
            viewModel?.consumeSaveSuccess()
            onContinue()
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .padding(bottom = 120.dp)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Choose Your Interest",
                        style = TextStyle(
                            fontFamily = EncodeSansExpanded,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Transparent,
                        onClick = onBack,
                        enabled = !isSaving
                    ) {
                        Box(
                            modifier = Modifier.padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )

            Text(
                text = "Choose your interests and get the best music recommendations. Don't worry, you can always change them later.",
                style = TextStyle(
                    fontFamily = RobotoMono,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                interests.forEach { interest ->
                    val isSelected = selectedInterests.contains(interest)
                    Surface(
                        onClick = {
                            selectedInterests = if (isSelected) {
                                selectedInterests.filterNot { it == interest }
                            } else {
                                selectedInterests + interest
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(30.dp),
                        color = if (isSelected) focusColor else backgroundColor,
                        border = BorderStroke(1.dp, focusColor)
                    ) {
                        Text(
                            text = interest,
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            style = TextStyle(
                                fontFamily = RobotoMono,
                                color = if (isSelected) backgroundColor else Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = borderColor,
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, borderColor),
                contentPadding = PaddingValues(vertical = 16.dp)
            )  {
                Text(
                    text = "Skip",
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White
                    )
                )
            }

            Button(
                onClick = { 
                    // ✅ CONECTIVIDAD EVENTUAL: Guardar con versionado y Outbox
                    if (viewModel != null) {
                        viewModel.saveInterests(userId, selectedInterests)
                    } else {
                        // Fallback si no hay ViewModel
                        onContinue()
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = focusColor),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Continue",
                        style = TextStyle(
                            fontFamily = RobotoMono,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun InterestSelectionPreview() {
    OrbitSoundKotlinTheme(dynamicColor = false) {
        InterestSelectionScreen()
    }
}
