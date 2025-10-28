package com.g22.orbitsoundkotlin.ui.screens.home

import android.content.Context
import android.location.Location
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val DefaultStarColors = listOf(Color.White)

data class HomeUiState(
    val isLoadingWeather: Boolean = false,
    val locationError: Boolean = false,
    val weather: Weather? = null,
    val starPrevColors: List<Color> = DefaultStarColors,
    val starCurrColors: List<Color> = DefaultStarColors,
    val lightningActive: Boolean = false,
    val error: String? = null
)

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

class HomeViewModel(
    private val publisher: HomeEventPublisher = HomeEventPublisher(),
    private val weatherService: WeatherService = WeatherService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var isObserverRegistered = false
    private var latestWeather: Weather? = null
    private var latestLocationError: Boolean = false
    private var previousColors: List<Color> = DefaultStarColors
    private var currentColors: List<Color> = DefaultStarColors
    private var isLightningActive: Boolean = false

    private val internalObserver = object : HomeObserver {
        override fun onHomeEvent(event: HomeEvent) {
            when (event) {
                is HomeEvent.WeatherUpdated -> {
                    latestWeather = event.weather
                    _uiState.update {
                        it.copy(
                            weather = event.weather,
                            isLoadingWeather = false,
                            error = null
                        )
                    }
                }

                is HomeEvent.LocationErrorChanged -> {
                    latestLocationError = event.hasError
                    _uiState.update {
                        it.copy(
                            locationError = event.hasError,
                            isLoadingWeather = false
                        )
                    }
                }

                is HomeEvent.StarColorsTransition -> {
                    previousColors = event.previous
                    currentColors = event.current
                    _uiState.update {
                        it.copy(
                            starPrevColors = event.previous,
                            starCurrColors = event.current
                        )
                    }
                }

                is HomeEvent.LightningChanged -> {
                    isLightningActive = event.isActive
                    _uiState.update { it.copy(lightningActive = event.isActive) }
                }
            }
        }
    }

    fun onAppear() {
        if (!isObserverRegistered) {
            publisher.subscribe(internalObserver)
            isObserverRegistered = true
            latestWeather?.let { internalObserver.onHomeEvent(HomeEvent.WeatherUpdated(it)) }
            internalObserver.onHomeEvent(HomeEvent.LocationErrorChanged(latestLocationError))
            internalObserver.onHomeEvent(HomeEvent.StarColorsTransition(previousColors, currentColors))
            internalObserver.onHomeEvent(HomeEvent.LightningChanged(isLightningActive))
        }
    }

    fun onDisappear() {
        if (isObserverRegistered) {
            publisher.unsubscribe(internalObserver)
            isObserverRegistered = false
        }
    }

    fun loadWeather(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWeather = true, error = null) }
            try {
                val loc = getLastKnownLocation(context)
                val weather = weatherService.fetchWeather(
                    lat = loc?.latitude ?: 4.60971,
                    lon = loc?.longitude ?: -74.08175
                )
                latestWeather = weather
                latestLocationError = false
                publisher.notify(HomeEvent.WeatherUpdated(weather))
                publisher.notify(HomeEvent.LocationErrorChanged(false))
                updateStarColorsByCondition(weather.condition)
                if (weather.condition.lowercase().contains("thunderstorm")) {
                    triggerLightningPulse()
                } else {
                    ensureLightningOff()
                }
            } catch (ex: Exception) {
                latestLocationError = true
                publisher.notify(HomeEvent.LocationErrorChanged(true))
                _uiState.update {
                    it.copy(
                        isLoadingWeather = false,
                        error = ex.message
                    )
                }
            }
        }
    }

    private fun updateStarColorsByCondition(condition: String) {
        val newColors = when (condition.lowercase()) {
            "clear" -> listOf(Color(0xFFFFE082), Color(0xFFFFF8E1), Color(0xFFFFECB3))
            "clouds" -> listOf(Color(0xFFB0BEC5), Color(0xFFCFD8DC), Color(0xFF90A4AE))
            "rain" -> listOf(Color(0xFF90CAF9), Color(0xFF64B5F6), Color(0xFFBBDEFB))
            "thunderstorm" -> listOf(Color(0xFFB3E5FC), Color(0xFFE1F5FE), Color(0xFF81D4FA))
            else -> DefaultStarColors
        }
        val previous = currentColors
        previousColors = previous
        currentColors = newColors
        publisher.notify(HomeEvent.StarColorsTransition(previous, newColors))
    }

    private fun triggerLightningPulse() {
        viewModelScope.launch {
            delay(250)
            setLightningState(true)
            delay(900)
            ensureLightningOff()
        }
    }

    private fun setLightningState(active: Boolean) {
        if (isLightningActive == active) return
        isLightningActive = active
        publisher.notify(HomeEvent.LightningChanged(active))
    }

    private fun ensureLightningOff() {
        setLightningState(false)
    }
}

data class Weather(
    val temperatureC: Double,
    val description: String,
    val condition: String
)

object WeatherService {
    suspend fun fetchWeather(lat: Double, lon: Double): Weather {
        return runCatching { fetchFromApi(lat, lon) }
            .getOrElse {
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

@Suppress("MissingPermission")
private fun getLastKnownLocation(context: Context): Location? {
    return null
}
