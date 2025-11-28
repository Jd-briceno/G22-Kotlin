package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.data.local.entities.SessionActivityLogEntity
import com.g22.orbitsoundkotlin.data.repositories.SessionActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para Captain's Log - Session Activity Journal.
 * 
 * Procesa y muestra logs de actividad de sesiones del usuario.
 * Implementa cache en memoria y procesamiento paralelo para múltiples períodos.
 */
data class CaptainsLogUiState(
    val isLoading: Boolean = false,
    val sessionLogs: List<SessionActivityLogEntity> = emptyList(),
    val error: String? = null,
    val selectedPeriod: ActivityPeriod = ActivityPeriod.WEEK
)

/**
 * Períodos de actividad disponibles para filtrar logs.
 */
enum class ActivityPeriod(val days: Int, val displayName: String) {
    DAY(1, "Día"),
    WEEK(7, "Semana"),
    MONTH(30, "Mes")
}

class CaptainsLogViewModel(
    private val sessionActivityRepository: SessionActivityRepository,
    private val userId: String,
    private val userEmail: String? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CaptainsLogUiState())
    val uiState: StateFlow<CaptainsLogUiState> = _uiState.asStateFlow()
    
    // Cache en memoria para acceso rápido
    private val memoryCache = mutableMapOf<String, List<SessionActivityLogEntity>>()
    private val TAG = "CaptainsLogViewModel"
    
    init {
        // Cargar logs al inicializar
        loadSessionLogs(ActivityPeriod.WEEK)
    }
    
    /**
     * Carga logs de sesiones para un período específico.
     * Usa cache en memoria si está disponible, sino procesa en background.
     */
    fun loadSessionLogs(period: ActivityPeriod) {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedPeriod = period) }
            
            try {
                // Paso 1: Verificar cache en memoria (acceso ultra-rápido)
                val cacheKey = "${userId}_${period.name}"
                val cachedInMemory = memoryCache[cacheKey]
                if (cachedInMemory != null) {
                    Log.d(TAG, "✅ Memory cache HIT for period: ${period.displayName}")
                    _uiState.update {
                        it.copy(
                            sessionLogs = cachedInMemory,
                            isLoading = false
                        )
                    }
                    // Actualizar en background sin bloquear UI
                    refreshLogsInBackground(period)
                    return@launch
                }
                
                // Paso 2: Obtener desde repositorio (cache Room + procesamiento)
                val periodStartTimestamp = System.currentTimeMillis() - (period.days * 24 * 60 * 60 * 1000L)
                val logs = withContext(Dispatchers.Default) {
                    // Procesamiento en background thread para no bloquear UI
                    sessionActivityRepository.getSessionLogs(userId, userEmail, periodStartTimestamp)
                }
                
                // Paso 3: Actualizar cache en memoria y UI
                memoryCache[cacheKey] = logs
                _uiState.update {
                    it.copy(
                        sessionLogs = logs,
                        isLoading = false
                    )
                }
                
                Log.d(TAG, "✅ Session logs loaded for ${period.displayName}: ${logs.size} sessions")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading session logs", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading session logs"
                    )
                }
            }
        }
    }
    
    /**
     * Carga logs para múltiples períodos en paralelo.
     * Útil para mostrar comparaciones o estadísticas agregadas.
     */
    fun loadAllPeriodsLogs() {
        if (userId.isBlank()) {
            _uiState.update { it.copy(error = "User ID is required") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Procesamiento paralelo de múltiples períodos
                val dayDeferred = async(Dispatchers.Default) {
                    val periodStart = System.currentTimeMillis() - (ActivityPeriod.DAY.days * 24 * 60 * 60 * 1000L)
                    sessionActivityRepository.getSessionLogs(userId, userEmail, periodStart)
                }
                
                val weekDeferred = async(Dispatchers.Default) {
                    val periodStart = System.currentTimeMillis() - (ActivityPeriod.WEEK.days * 24 * 60 * 60 * 1000L)
                    sessionActivityRepository.getSessionLogs(userId, userEmail, periodStart)
                }
                
                val monthDeferred = async(Dispatchers.Default) {
                    val periodStart = System.currentTimeMillis() - (ActivityPeriod.MONTH.days * 24 * 60 * 60 * 1000L)
                    sessionActivityRepository.getSessionLogs(userId, userEmail, periodStart)
                }
                
                // Esperar a que todos los períodos se procesen (await individual para mantener tipos)
                val dayLogs = dayDeferred.await()
                val weekLogs = weekDeferred.await()
                val monthLogs = monthDeferred.await()
                
                // Actualizar cache en memoria
                memoryCache["${userId}_${ActivityPeriod.DAY.name}"] = dayLogs
                memoryCache["${userId}_${ActivityPeriod.WEEK.name}"] = weekLogs
                memoryCache["${userId}_${ActivityPeriod.MONTH.name}"] = monthLogs
                
                // Mostrar logs de la semana por defecto
                _uiState.update {
                    it.copy(
                        sessionLogs = weekLogs,
                        isLoading = false,
                        selectedPeriod = ActivityPeriod.WEEK
                    )
                }
                
                Log.d(TAG, "✅ All periods loaded in parallel: " +
                        "Day=${dayLogs.size}, Week=${weekLogs.size}, Month=${monthLogs.size}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading all periods logs", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error loading session logs"
                    )
                }
            }
        }
    }
    
    /**
     * Actualiza logs en background sin bloquear UI.
     * Útil para refrescar datos después de servir cache.
     */
    private fun refreshLogsInBackground(period: ActivityPeriod) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val periodStartTimestamp = System.currentTimeMillis() - (period.days * 24 * 60 * 60 * 1000L)
                val logs = sessionActivityRepository.getSessionLogs(userId, userEmail, periodStartTimestamp)
                
                // Actualizar cache en memoria
                val cacheKey = "${userId}_${period.name}"
                memoryCache[cacheKey] = logs
                
                // Actualizar UI si el período sigue siendo el seleccionado
                _uiState.update { currentState ->
                    if (currentState.selectedPeriod == period) {
                        currentState.copy(sessionLogs = logs)
                    } else {
                        currentState
                    }
                }
                
                Log.d(TAG, "🔄 Background refresh completed for ${period.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error refreshing logs in background", e)
            }
        }
    }
    
    /**
     * Limpia el cache en memoria.
     * Útil cuando el usuario cambia o se necesita forzar recarga.
     */
    fun clearMemoryCache() {
        memoryCache.clear()
        Log.d(TAG, "🗑️ Memory cache cleared")
    }
    
    /**
     * Limpia todos los logs del usuario (cache y datos).
     * Útil para logout o limpieza de datos.
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            try {
                sessionActivityRepository.deleteAllLogsForUser(userId)
                memoryCache.clear()
                _uiState.update {
                    it.copy(sessionLogs = emptyList())
                }
                Log.d(TAG, "🗑️ All logs cleared for user")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing logs", e)
                _uiState.update {
                    it.copy(error = e.message ?: "Error clearing logs")
                }
            }
        }
    }
}

