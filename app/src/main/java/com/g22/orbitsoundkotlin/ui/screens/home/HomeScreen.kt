package com.g22.orbitsoundkotlin.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.services.AuthUser
import com.g22.orbitsoundkotlin.ui.viewmodels.HomeViewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.HomeViewModelFactory

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    user: AuthUser,
    onNavigateToStellarEmotions: () -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCaptainsLog: () -> Unit = {}
) {
    val context = LocalContext.current
    // ✅ CONECTIVIDAD EVENTUAL: Inyectar Context al HomeViewModel
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        viewModel.onAppear()
        onDispose { viewModel.onDisappear() }
    }

    LaunchedEffect(Unit) {
        viewModel.loadWeather(context)
    }

    val infinite = rememberInfiniteTransition(label = "space_time")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "space_cycle"
    )

    var colorPhaseTarget by remember { mutableStateOf(1f) }
    val colorPhase by animateFloatAsState(
        targetValue = colorPhaseTarget,
        animationSpec = tween(2000, easing = LinearEasing),
        label = "colorPhase"
    )

    LaunchedEffect(uiState.starPrevColors, uiState.starCurrColors) {
        colorPhaseTarget = 0f
    }
    LaunchedEffect(colorPhase) {
        if (colorPhase == 0f) colorPhaseTarget = 1f
    }

    RememberLocationPermissionRequester { viewModel.loadWeather(context) }

    val lightningProgress by animateFloatAsState(
        targetValue = if (uiState.lightningActive) 1f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "lightning"
    )

    val screenH = LocalConfiguration.current.screenHeightDp
    val astronautHeight = (screenH * 0.50f).dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
    ) {
        val starColors = rememberInterpolatedColors(
            uiState.starPrevColors,
            uiState.starCurrColors,
            colorPhase
        )
        StarField(
            modifier = Modifier.fillMaxSize(),
            globalTime = time * (2f * kotlin.math.PI.toFloat()),
            starColors = starColors
        )

        if (lightningProgress > 0f) {
            LightningLayer(
                modifier = Modifier.fillMaxSize(),
                progress = lightningProgress
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            OrbitNavbar(
                username = user.email?.split("@")?.firstOrNull() ?: "Explorer",
                title = "Ninja",
                subtitle = null,
                profilePainter = null,
                onNavigateToHome = {},
                onNavigateToProfile = onNavigateToProfile
            )

            Spacer(Modifier.height(24.dp))

            if (uiState.locationError) {
                Text(
                    text = "Ubicación no disponible: usando condiciones estelares por defecto.",
                    color = Color(0xFFFFCDD2),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            uiState.weather?.let { weather ->
                Text(
                    text = "${weather.description} • ${"%.1f".format(weather.temperatureC)} °C",
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentAlignment = Alignment.Center
            ) {
                Astronaut(
                    floatYOffsetDp = (kotlin.math.sin(time * (2f * kotlin.math.PI.toFloat())) * 10f).dp,
                    heightDp = astronautHeight
                )
            }

            Spacer(Modifier.height(24.dp))

            PlayerPill(
                albumName = "coldrain", // REQUIRED_IMAGE: coldrain.jpg (o .png)
                songTitle = "One last adventure",
                artist = "Evan Call",
                onPrev = {},
                onPlayPause = {},
                onNext = {}
            )

            Spacer(Modifier.height(24.dp))

            ShortcutsFive(
                items = listOf(
                    ShortcutSpec("Stellar Emotions", R.drawable.stellar_emotions),
                    ShortcutSpec("Star Archive", R.drawable.star_archive),
                    ShortcutSpec("Activity Stats", R.drawable.captain_log),
                    ShortcutSpec("Crew members", R.drawable.crew_members),
                    ShortcutSpec("Command profile", R.drawable.command_profile)
                ),
                onShortcutClick = { shortcut ->
                    when (shortcut.label) {
                        "Stellar Emotions" -> onNavigateToStellarEmotions()
                        "Star Archive" -> onNavigateToLibrary()
                        "Activity Stats" -> onNavigateToCaptainsLog()
                        "Command profile" -> onNavigateToProfile()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RememberLocationPermissionRequester(onGranted: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fine || coarse) onGranted()
    }
    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}
