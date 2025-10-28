package com.g22.orbitsoundkotlin.ui.screens

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import com.g22.orbitsoundkotlin.services.AuthUser
import com.g22.orbitsoundkotlin.ui.screens.shared.painterByNameOrNull


// ───────────────────────────── Models / Service (stub) ─────────────────────────────

data class Weather(
    val temperatureC: Double,
    val description: String,
    val condition: String // "clear", "clouds", "rain", "thunderstorm"
)

object WeatherService {
    suspend fun fetchWeather(lat: Double, lon: Double): Weather {
        return runCatching { fetchFromApi(lat, lon) }
            .getOrElse {
                // Fallback to a deterministic sample set if the API fails.
                val fallback = listOf(
                    Weather(22.0, "Parcialmente nublado", "clouds"),
                    Weather(26.0, "Cielo despejado", "clear"),
                    Weather(18.0, "Lluvia ligera", "rain"),
                    Weather(20.0, "Tormenta eléctrica aislada", "thunderstorm")
                )
                fallback.random()
            }
    }

    private suspend fun fetchFromApi(lat: Double, lon: Double): Weather = withContext(Dispatchers.IO) {
        val endpoint =
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true&timezone=auto&temperature_unit=celsius"
        val connection = java.net.URL(endpoint).openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseWeatherResponse(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeatherResponse(raw: String): Weather {
        val json = JSONObject(raw)
        val current = json.getJSONObject("current_weather")
        val temperature = current.getDouble("temperature")
        val weatherCode = current.optInt("weathercode", -1)
        val (description, condition) = mapWeatherCode(weatherCode)
        return Weather(
            temperatureC = temperature,
            description = description,
            condition = condition
        )
    }

    private fun mapWeatherCode(code: Int): Pair<String, String> = when (code) {
        0 -> "Cielo despejado" to "clear"
        1, 2 -> "Parcialmente nublado" to "clouds"
        3 -> "Nublado" to "clouds"
        in 45..48 -> "Neblina" to "clouds"
        in 51..57 -> "Llovizna" to "rain"
        in 61..67 -> "Lluvia" to "rain"
        in 71..77 -> "Nieve" to "snow"
        in 80..82 -> "Aguacero" to "rain"
        in 85..86 -> "Nieve intensa" to "snow"
        in 95..99 -> "Tormenta eléctrica" to "thunderstorm"
        else -> "Condición desconocida" to "clear"
    }
}

sealed interface HomeEvent {
    data class WeatherUpdated(val weather: Weather) : HomeEvent
    data class LocationErrorChanged(val hasError: Boolean) : HomeEvent
    data class StarColorsTransition(val previous: List<Color>, val current: List<Color>) : HomeEvent
    data class LightningChanged(val isActive: Boolean) : HomeEvent
}

interface HomeObserver {
    fun onHomeEvent(event: HomeEvent)
}

class HomeEventPublisher {
    private val observers = mutableSetOf<HomeObserver>()

    fun subscribe(observer: HomeObserver) {
        observers.add(observer)
    }

    fun unsubscribe(observer: HomeObserver) {
        observers.remove(observer)
    }

    fun notify(event: HomeEvent) {
        observers.forEach { it.onHomeEvent(event) }
    }
}

class HomeViewModel : ViewModel() {
    private val publisher = HomeEventPublisher()

    private var latestWeather: Weather? = null
    private var locationError = false
    private var previousColors = listOf(Color.White)
    private var currentColors = listOf(Color.White)
    private var isLightningActive = false

    fun registerObserver(observer: HomeObserver) {
        publisher.subscribe(observer)
        // Emit snapshot so UI starts with current state.
        latestWeather?.let { observer.onHomeEvent(HomeEvent.WeatherUpdated(it)) }
        observer.onHomeEvent(HomeEvent.LocationErrorChanged(locationError))
        observer.onHomeEvent(HomeEvent.StarColorsTransition(previousColors, currentColors))
        observer.onHomeEvent(HomeEvent.LightningChanged(isLightningActive))
    }

    fun unregisterObserver(observer: HomeObserver) {
        publisher.unsubscribe(observer)
    }

    fun loadWeather(context: Context) {
        viewModelScope.launch {
            try {
                val loc = getLastKnownLocation(context)
                val w = WeatherService.fetchWeather(
                    lat = loc?.latitude ?: 4.60971,   // Bogotá fallback
                    lon = loc?.longitude ?: -74.08175
                )
                latestWeather = w
                locationError = false
                publisher.notify(HomeEvent.WeatherUpdated(w))
                publisher.notify(HomeEvent.LocationErrorChanged(locationError))
                updateStarColors(w)
                if (w.condition.lowercase().contains("thunderstorm")) {
                    triggerLightningOnce()
                } else {
                    ensureLightningOff()
                }
            } catch (_: Exception) {
                locationError = true
                publisher.notify(HomeEvent.LocationErrorChanged(locationError))
            }
        }
    }

    private fun updateStarColors(w: Weather) {
        val newColors = when (w.condition.lowercase()) {
            "clear" -> listOf(Color(0xFFFFE082), Color(0xFFFFF8E1), Color(0xFFFFECB3))
            "clouds" -> listOf(Color(0xFFB0BEC5), Color(0xFFCFD8DC), Color(0xFF90A4AE))
            "rain" -> listOf(Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFFBBDEFB))
            "thunderstorm" -> listOf(Color(0xFFB3E5FC), Color(0xFFE1F5FE), Color(0xFF81D4FA))
            else -> listOf(Color.White)
        }
        previousColors = currentColors
        currentColors = newColors
        publisher.notify(HomeEvent.StarColorsTransition(previousColors, currentColors))
    }

    private suspend fun triggerLightningOnce() {
        delay(250)
        setLightningState(true)
        delay(900)
        setLightningState(false)
    }

    private fun ensureLightningOff() {
        if (isLightningActive) {
            setLightningState(false)
        }
    }

    private fun setLightningState(active: Boolean) {
        if (isLightningActive == active) return
        isLightningActive = active
        publisher.notify(HomeEvent.LightningChanged(isLightningActive))
    }
}

// ───────────────────────────── Permisos (simple) ─────────────────────────────

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

// ───────────────────────────── HomeScreen (UI) ─────────────────────────────

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    user: AuthUser,
    onNavigateToStellarEmotions: () -> Unit,
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val vm = remember { HomeViewModel() }
    val context = LocalContext.current

    val infinite = rememberInfiniteTransition()
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val weatherState = remember { mutableStateOf<Weather?>(null) }
    val locationErrorState = remember { mutableStateOf(false) }
    val previousColorsState = remember { mutableStateOf(listOf(Color.White)) }
    val currentColorsState = remember { mutableStateOf(listOf(Color.White)) }
    val lightningState = remember { mutableStateOf(false) }

    var colorPhaseTarget by remember { mutableStateOf(1f) }
    val colorPhase by animateFloatAsState(
        targetValue = colorPhaseTarget,
        animationSpec = tween(2000, easing = LinearEasing),
        label = "colorPhase"
    )

    val observer = remember {
        object : HomeObserver {
            override fun onHomeEvent(event: HomeEvent) {
                when (event) {
                    is HomeEvent.WeatherUpdated -> weatherState.value = event.weather
                    is HomeEvent.LocationErrorChanged -> locationErrorState.value = event.hasError
                    is HomeEvent.StarColorsTransition -> {
                        previousColorsState.value = event.previous
                        currentColorsState.value = event.current
                        colorPhaseTarget = 0f
                    }
                    is HomeEvent.LightningChanged -> lightningState.value = event.isActive
                }
            }
        }
    }

    DisposableEffect(vm) {
        vm.registerObserver(observer)
        onDispose { vm.unregisterObserver(observer) }
    }

    LaunchedEffect(Unit) {
        vm.loadWeather(context)
    }
    LaunchedEffect(colorPhase) {
        if (colorPhase == 0f) colorPhaseTarget = 1f
    }

    RememberLocationPermissionRequester { vm.loadWeather(context) }

    val lightningProgress by animateFloatAsState(
        targetValue = if (lightningState.value) 1f else 0f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "lightning"
    )

    val screenH = LocalConfiguration.current.screenHeightDp
    val astronautHeight = (screenH * 0.50f).dp  // ≈50% alto pantalla → más grande

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
    ) {
        // Estrellas
        val starColors = rememberInterpolatedColors(previousColorsState.value, currentColorsState.value, colorPhase)
        StarField(
            modifier = Modifier.fillMaxSize(),
            globalTime = time * (2f * PI.toFloat()),
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

            if (locationErrorState.value) {
                Text(
                    text = "Ubicación no disponible: usando condiciones estelares por defecto.",
                    color = Color(0xFFFFCDD2),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            weatherState.value?.let { weather ->
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
                    floatYOffsetDp = (sin(time * (2f * PI.toFloat())) * 10f).dp,
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
                    ShortcutSpec("Captain's Log", R.drawable.captain_log),
                    ShortcutSpec("Crew members", R.drawable.crew_members),
                    ShortcutSpec("Command profile", R.drawable.command_profile)
                ),
                onShortcutClick = { shortcut ->
                    if (shortcut.label == "Stellar Emotions") {
                        onNavigateToStellarEmotions()
                    } else if (shortcut.label == "Star Archive") {
                        onNavigateToLibrary()
                    } else if (shortcut.label == "Command profile") {
                        onNavigateToProfile()
                    }
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── UI pieces ─────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrbitNavbar(
    username: String,
    title: String,
    subtitle: String? = null,
    profilePainter: Painter? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val dark = Color(0xFF010B19)
    val borderColor = Color(0xFFB4B1B8)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val outerShape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
        Box(
            modifier = Modifier
                .offset(x = 28.dp, y = 14.dp)
                .size(width = 325.dp, height = 86.dp)
                .clip(outerShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), outerShape)
        )

        val centerShape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp, topEnd = 5.dp)
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 36.dp)
                .size(width = 225.dp, height = 57.dp)
                .clip(centerShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), centerShape)
                .padding(start = 32.dp, end = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = subtitle ?: "Hello, $username",
                color = Color(0xFFE9E8EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }

        val topShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 1.dp)
                .size(width = 300.dp, height = 30.dp)
                .clip(topShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), topShape)
        )

        val titleShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .offset(x = 38.dp, y = 4.5.dp)
                .size(width = 123.dp, height = 22.dp)
                .clip(titleShape)
                .border(BorderStroke(2.dp, borderColor), titleShape)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = Color(0xFFE9E8EE),
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }

        val buttonShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .offset(x = 1.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape)
                .clickable { onNavigateToHome() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = Color(0xFFE9E8EE),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 252.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFFE9E8EE)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 300.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape)
                .clickable { onNavigateToProfile() },
            contentAlignment = Alignment.Center
        ) {
            if (profilePainter != null) {
                Image(
                    painter = profilePainter,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile placeholder",
                    tint = Color(0xFFE9E8EE)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = 167.dp, y = 4.5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Headset,
                contentDescription = "Headphones",
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 192.dp, y = 4.5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = "Music",
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 245.dp, y = 7.dp)
                .size(17.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape)
        )

        Box(
            modifier = Modifier
                .offset(x = 265.dp, y = 18.dp)
                .size(width = 14.dp, height = 2.dp)
                .background(borderColor)
        )

        Canvas(
            modifier = Modifier
                .offset(x = 280.dp, y = 10.dp)
                .size(14.dp)
        ) {
            val paintColor = borderColor
            drawLine(
                color = paintColor,
                strokeWidth = 2f,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            drawLine(
                color = paintColor,
                strokeWidth = 2f,
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height)
            )
        }
    }
}

@Composable
private fun Astronaut(floatYOffsetDp: Dp, heightDp: Dp) {
    val p = painterByNameOrNull("astronaut_home")
    Box(modifier = Modifier.height(heightDp)) {
        if (p != null) {
            Image(
                painter = p,
                contentDescription = "Astronaut",
                modifier = Modifier
                    .offset(y = floatYOffsetDp)
                    .fillMaxWidth()
                    .height(heightDp)
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(y = floatYOffsetDp)
                    .fillMaxWidth()
                    .height(heightDp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0B1A2F)),
                contentAlignment = Alignment.Center
            ) { Text("astronaut_home.png", color = Color(0xFF90CAF9)) }
        }
    }
}

@Composable
private fun PlayerPill(
    albumName: String,
    // REQUIRED_IMAGE: <albumName>.jpg | .png
    songTitle: String,
    artist: String,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1A2F)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val albumPainter = painterByNameOrNull(albumName)
            if (albumPainter != null) {
                Image(
                    painter = albumPainter,
                    contentDescription = "Album",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10223B)),
                    contentAlignment = Alignment.Center
                ) { Text("♫", color = Color(0xFF64B5F6)) }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    songTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10223B))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.28f)
                            .background(Color(0xFF64B5F6))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Filled.SkipPrevious, null, tint = Color(0xFFB0BEC5)) }
                IconButton(onClick = onPlayPause) { Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFB0BEC5)) }
                IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, null, tint = Color(0xFFB0BEC5)) }
            }
        }
    }
}

data class ShortcutSpec(val label: String, @DrawableRes val iconRes: Int)

@Composable
private fun ShortcutsFive(
    items: List<ShortcutSpec>,
    onShortcutClick: (ShortcutSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.take(5).forEach { spec ->
            ShortcutTile(spec, onShortcutClick = { onShortcutClick(spec) })
        }
    }
}

@Composable
private fun RowScope.ShortcutTile(
    spec: ShortcutSpec,    onShortcutClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.2f)
                ),
                onClick = onShortcutClick
            )
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = spec.iconRes),
            contentDescription = spec.label,
            modifier = Modifier
                .heightIn(min = 110.dp)
                .fillMaxWidth(0.88f),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = spec.label,
            color = Color(0xFFE0E0E0),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ───────────────────────────── Star field ─────────────────────────────

@Composable
private fun StarField(
    modifier: Modifier,
    globalTime: Float,
    starColors: List<Color>
) {
    data class Star(val pos: Offset, val size: Float, val phase: Float, val speed: Float, val colorIndex: Int)

    val stars = remember {
        val rnd = Random(42)
        List(140) {
            Star(
                pos = Offset(rnd.nextFloat(), rnd.nextFloat()),
                size = (1f + rnd.nextFloat() * 2f),
                phase = rnd.nextFloat() * (2f * PI.toFloat()),
                speed = 0.5f + rnd.nextFloat() * 1.5f,
                colorIndex = rnd.nextInt(0, 3)
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val colorCount = starColors.size.coerceAtLeast(1)
        stars.forEach { s ->
            val t = (sin(globalTime * s.speed + s.phase) + 1f) * 0.5f // 0..1
            val radius = s.size * (0.5f + t * 0.5f)
            val c = starColors.getOrElse(s.colorIndex % colorCount) { Color.White }
            drawCircle(
                color = c.copy(alpha = 0.4f + 0.6f * t),
                radius = radius,
                center = Offset(s.pos.x * width, s.pos.y * height)
            )
        }
    }
}

@Composable
private fun rememberInterpolatedColors(
    prev: List<Color>,
    curr: List<Color>,
    phase: Float // 1 -> prev; 0 -> curr
): List<Color> {
    val count = maxOf(prev.size, curr.size, 1)
    return List(count) { i ->
        val a = prev.getOrElse(i) { prev.lastOrNull() ?: Color.White }
        val b = curr.getOrElse(i) { curr.lastOrNull() ?: Color.White }
        lerpColor(b, a, phase) // cuando phase va a 0, se ve curr
    }
}

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    fun lerp(a: Float, b: Float, tt: Float) = a + (b - a) * tt
    return Color(
        red = lerp(start.red, end.red, clamped),
        green = lerp(start.green, end.green, clamped),
        blue = lerp(start.blue, end.blue, clamped),
        alpha = lerp(start.alpha, end.alpha, clamped)
    )
}

// ───────────────────────────── Lightning ─────────────────────────────

@Composable
private fun LightningLayer(modifier: Modifier, progress: Float) {
    Canvas(modifier = modifier) { drawLightning(progress) }
}

private fun DrawScope.drawLightning(progress: Float) {
    if (progress < 0.2f || progress > 0.8f) return

    val alpha = 1f - (progress - 0.2f)
    val width = size.width
    val height = size.height
    val rnd = Random((progress * 10_000).toInt())

    var x = rnd.nextFloat() * width
    var y = 0f
    val path = Path().apply { moveTo(x, y) }

    while (y < height) {
        val xOffset = (rnd.nextFloat() - 0.5f) * 50f
        y += 30f + rnd.nextFloat() * 20f
        path.lineTo(x + xOffset, y)
        x += xOffset
    }

    drawIntoCanvas {
        drawPath(
            path = path,
            color = Color(0xFF448AFF).copy(alpha = alpha.coerceIn(0f, 1f)),
            style = Stroke(width = 2f)
        )
    }
}

// ───────────────────────────── Utils ─────────────────────────────

@Composable
private fun painterByNameOrNull(name: String): Painter? {
    val context = LocalContext.current
    val resId = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    return if (resId != 0) painterResource(id = resId) else null
}

@Suppress("MissingPermission")
private fun getLastKnownLocation(context: Context): Location? {
    return null
}
