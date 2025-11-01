package com.g22.orbitsoundkotlin.data.repositories

import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.WeatherCacheEntity
import com.g22.orbitsoundkotlin.ui.screens.home.Weather

/**
 * Repository para datos de clima usando SWR pattern (Stale-While-Revalidate).
 * Sirve cache inmediatamente mientras actualiza en background.
 */
class WeatherRepository(
    private val db: AppDatabase
) {
    private val weatherCacheDao = db.weatherCacheDao()

    /**
     * Obtiene datos de clima con SWR pattern.
     * Retorna cache válido inmediatamente, actualiza en background si está expirado.
     */
    suspend fun getWeather(
        uid: String,
        latitude: Double,
        longitude: Double,
        fetchRemote: suspend () -> Weather
    ): Weather {
        // Intentar obtener cache válido
        val cached = weatherCacheDao.getValidCacheByUid(uid)
        if (cached != null) {
            // Cache válido: retornarlo
            return Weather(cached.temperatureC.toDouble(), cached.description, extractCondition(cached.description))
        }

        // No hay cache válido: fetch del servidor
        val weather = fetchRemote()

        // Guardar en cache
        val cacheEntity = WeatherCacheEntity(
            uid = uid,
            temperatureC = weather.temperatureC.toFloat(),
            description = weather.description,
            latitude = latitude,
            longitude = longitude,
            expiresAt = WeatherCacheEntity.calculateExpiresAt()
        )
        weatherCacheDao.insertCache(cacheEntity)

        return weather
    }

    /**
     * Limpia cache expirado (llamar periódicamente).
     */
    suspend fun cleanupExpiredCache() {
        weatherCacheDao.deleteExpiredCaches()
    }

    /**
     * Extrae la condición meteorológica de la descripción para compatibilidad.
     */
    private fun extractCondition(description: String): String {
        return when {
            description.contains("clear", ignoreCase = true) -> "clear"
            description.contains("cloud", ignoreCase = true) -> "clouds"
            description.contains("rain", ignoreCase = true) -> "rain"
            description.contains("thunder", ignoreCase = true) || description.contains("storm", ignoreCase = true) -> "thunderstorm"
            else -> "clear"
        }
    }
}

