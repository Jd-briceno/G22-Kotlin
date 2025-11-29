package com.g22.orbitsoundkotlin.data.repositories

import android.util.Log
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Repository para procesar y almacenar logs de actividad de sesiones.
 * Implementa SWR pattern (Stale-While-Revalidate) para eventual connectivity.
 * 
 * Procesa datos desde múltiples fuentes locales:
 * - LoginTelemetryEntity: Logins del usuario
 * - OutboxEntity: Acciones realizadas
 * - SearchHistoryEntity: Búsquedas realizadas
 * 
 * Agrupa eventos por sesión (ventana de 30 minutos de inactividad)
 * y calcula métricas agregadas.
 */
class SessionActivityRepository(
    private val db: AppDatabase
) {
    private val sessionLogDao = db.sessionActivityLogDao()
    private val telemetryDao = db.telemetryDao()
    private val outboxDao = db.outboxDao()
    private val libraryCacheDao = db.libraryCacheDao()
    
    private val gson = Gson()
    private val TAG = "SessionActivityRepo"
    
    /**
     * Obtiene logs de sesiones con SWR pattern.
     * 
     * 1. Intenta obtener cache válido (no expirado) → retorna inmediatamente
     * 2. Si no hay cache válido, procesa datos desde fuentes locales
     * 3. Guarda resultado en cache para próximas consultas
     * 4. Funciona 100% offline usando datos locales
     * 
     * @param userId ID del usuario (para filtrar SearchHistoryEntity y OutboxEntity)
     * @param userEmail Email del usuario (para filtrar LoginTelemetryEntity)
     * @param periodStartTimestamp Timestamp de inicio del período (ej. hace 2 semanas)
     * @return Lista de logs de sesiones procesados
     */
    suspend fun getSessionLogs(
        userId: String,
        userEmail: String? = null,
        periodStartTimestamp: Long
    ): List<SessionActivityLogEntity> = withContext(Dispatchers.Default) {
        
        // SWR Step 1: Intentar obtener cache válido
        val cachedLogs = sessionLogDao.getValidCache(userId, limit = 100)
        if (cachedLogs != null && cachedLogs.isNotEmpty()) {
            // Filtrar por período si es necesario
            val filteredLogs = cachedLogs.filter { 
                it.sessionStartTimestamp >= periodStartTimestamp 
            }
            if (filteredLogs.isNotEmpty()) {
                Log.d(TAG, "✅ Cache HIT: Serving ${filteredLogs.size} cached session logs")
                return@withContext filteredLogs
            }
        }
        
        // SWR Step 2: No hay cache válido, procesar desde datos fuente
        Log.d(TAG, "❌ Cache MISS: Processing session logs from local data sources")
        val processedLogs = processSessionLogsFromSources(userId, userEmail, periodStartTimestamp)
        
        // SWR Step 3: Guardar en cache para próximas consultas
        if (processedLogs.isNotEmpty()) {
            sessionLogDao.insertLogs(processedLogs)
            Log.d(TAG, "💾 Cache UPDATED: Saved ${processedLogs.size} session logs")
        }
        
        processedLogs
    }
    
    /**
     * Procesa logs de sesiones desde fuentes de datos locales.
     * Usa procesamiento paralelo para consultar múltiples fuentes simultáneamente.
     * 
     * @param userId ID del usuario
     * @param userEmail Email del usuario (para filtrar LoginTelemetryEntity)
     * @param periodStartTimestamp Timestamp de inicio del período
     * @return Lista de logs de sesiones procesados
     */
    private suspend fun processSessionLogsFromSources(
        userId: String,
        userEmail: String?,
        periodStartTimestamp: Long
    ): List<SessionActivityLogEntity> = withContext(Dispatchers.Default) {
        
        // Procesamiento paralelo de múltiples fuentes de datos
        val loginDataDeferred = async {
            // Obtener todos los logins exitosos del usuario en el período
            getAllLoginTelemetry(userEmail, periodStartTimestamp)
        }
        
        val outboxDataDeferred = async {
            // Obtener todas las operaciones del usuario en el período
            getAllOutboxOperations(userId, periodStartTimestamp)
        }
        
        val searchDataDeferred = async {
            // Obtener todas las búsquedas del usuario en el período
            getAllSearchHistory(userId, periodStartTimestamp)
        }
        
        // Esperar a que todas las consultas terminen (await individual para mantener tipos)
        val loginTelemetry = loginDataDeferred.await()
        val outboxOperations = outboxDataDeferred.await()
        val searchHistory = searchDataDeferred.await()
        
        Log.d(TAG, "📊 Data sources loaded: ${loginTelemetry.size} logins, " +
                "${outboxOperations.size} operations, ${searchHistory.size} searches")
        
        // Agrupar eventos por sesión y calcular métricas
        groupEventsBySession(
            userId = userId,
            loginTelemetry = loginTelemetry,
            outboxOperations = outboxOperations,
            searchHistory = searchHistory
        )
    }
    
    /**
     * Obtiene todos los logins exitosos del usuario en el período.
     * Filtra por email si se proporciona, sino obtiene todos los logins exitosos.
     * 
     * IMPORTANTE: Obtiene TODOS los logins (sincronizados y no sincronizados)
     * porque necesitamos procesar el historial completo de sesiones.
     */
    private suspend fun getAllLoginTelemetry(
        userEmail: String?,
        periodStartTimestamp: Long
    ): List<LoginTelemetryEntity> = withContext(Dispatchers.IO) {
        // Obtener TODOS los logins en el rango de tiempo (sincronizados y no sincronizados)
        val endTime = System.currentTimeMillis()
        val allTelemetry = telemetryDao.getTelemetryInTimeRange(periodStartTimestamp, endTime)
        
        val filtered = allTelemetry.filter { 
            it.success && 
            (userEmail == null || it.email.equals(userEmail, ignoreCase = true))
        }
        
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        Log.d(TAG, "📥 Login telemetry: ${allTelemetry.size} total in range, ${filtered.size} filtered " +
                "(email=$userEmail, periodStart=${dateFormat.format(java.util.Date(periodStartTimestamp))}, " +
                "periodEnd=${dateFormat.format(java.util.Date(endTime))})")
        
        filtered
    }
    
    /**
     * Obtiene todas las operaciones del usuario en el período.
     * Nota: OutboxEntity no tiene userId directo en el payload.
     * Por ahora, obtenemos todas las operaciones no sincronizadas en el período.
     */
    private suspend fun getAllOutboxOperations(
        userId: String,
        periodStartTimestamp: Long
    ): List<OutboxEntity> = withContext(Dispatchers.IO) {
        val allOperations = outboxDao.getUnsyncedOperations()
        val filtered = allOperations.filter { it.createdAt >= periodStartTimestamp }
        
        Log.d(TAG, "📥 Outbox operations: ${allOperations.size} total, ${filtered.size} filtered " +
                "(periodStart=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(periodStartTimestamp))})")
        
        filtered
    }
    
    /**
     * Obtiene todas las búsquedas del usuario en el período.
     */
    private suspend fun getAllSearchHistory(
        userId: String,
        periodStartTimestamp: Long
    ): List<SearchHistoryEntity> = withContext(Dispatchers.IO) {
        val allSearches = libraryCacheDao.getRecentSearches(userId, limit = 1000)
        val filtered = allSearches.filter { it.timestamp >= periodStartTimestamp }
        
        Log.d(TAG, "📥 Search history: ${allSearches.size} total, ${filtered.size} filtered " +
                "(userId=$userId, periodStart=${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(periodStartTimestamp))})")
        
        filtered
    }
    
    /**
     * Agrupa eventos por sesión y calcula métricas.
     * Una sesión comienza con un login exitoso y termina después de 30 minutos de inactividad.
     */
    private fun groupEventsBySession(
        userId: String,
        loginTelemetry: List<LoginTelemetryEntity>,
        outboxOperations: List<OutboxEntity>,
        searchHistory: List<SearchHistoryEntity>
    ): List<SessionActivityLogEntity> {
        
        if (loginTelemetry.isEmpty()) {
            Log.d(TAG, "No login telemetry found, returning empty logs")
            return emptyList()
        }
        
        // Ordenar logins por timestamp
        val sortedLogins = loginTelemetry.sortedBy { it.timestamp }
        
        val sessionLogs = mutableListOf<SessionActivityLogEntity>()
        
        for (login in sortedLogins) {
            val sessionStart = login.timestamp
            val sessionEnd = sessionStart + SessionActivityLogEntity.SESSION_INACTIVITY_WINDOW_MS
            
            // Encontrar todos los eventos que pertenecen a esta sesión
            val sessionOutboxOps = outboxOperations.filter { op ->
                op.createdAt >= sessionStart && 
                op.createdAt <= sessionEnd
            }
            
            val sessionSearches = searchHistory.filter { search ->
                search.timestamp >= sessionStart && 
                search.timestamp <= sessionEnd
            }
            
            // Calcular métricas
            val totalActions = sessionOutboxOps.size
            val actionTypesMap = sessionOutboxOps
                .groupingBy { it.operationType.name }
                .eachCount()
            val actionTypesJson = gson.toJson(actionTypesMap)
            
            val totalSearches = sessionSearches.size
            val searchQueries = sessionSearches
                .sortedByDescending { it.timestamp }
                .take(10) // Limitar a 10 búsquedas más recientes
                .map { it.query }
            val searchQueriesJson = gson.toJson(searchQueries)
            
            // Calcular duración de sesión
            val lastEventTimestamp = listOfNotNull(
                sessionOutboxOps.maxOfOrNull { it.createdAt },
                sessionSearches.maxOfOrNull { it.timestamp }
            ).maxOrNull() ?: sessionStart
            
            val actualSessionEnd = lastEventTimestamp + SessionActivityLogEntity.SESSION_INACTIVITY_WINDOW_MS
            val durationMinutes = ((actualSessionEnd - sessionStart) / (60 * 1000)).toInt()
            
            // Crear log de sesión
            val sessionLog = SessionActivityLogEntity(
                userId = userId,
                sessionStartTimestamp = sessionStart,
                sessionEndTimestamp = actualSessionEnd,
                durationMinutes = durationMinutes,
                loginType = login.loginType.name,
                totalActions = totalActions,
                actionTypesJson = actionTypesJson,
                totalSearches = totalSearches,
                searchQueriesJson = searchQueriesJson,
                processedAt = System.currentTimeMillis(),
                cachedAt = System.currentTimeMillis(),
                expiresAt = SessionActivityLogEntity.calculateExpiresAt()
            )
            
            sessionLogs.add(sessionLog)
        }
        
        Log.d(TAG, "✅ Processed ${sessionLogs.size} session logs")
        return sessionLogs
    }
    
    /**
     * Limpia logs expirados para liberar espacio.
     * Debe llamarse periódicamente.
     */
    suspend fun cleanupExpiredLogs() {
        withContext(Dispatchers.IO) {
            sessionLogDao.deleteExpiredLogs()
            Log.d(TAG, "🧹 Cleaned up expired session logs")
        }
    }
    
    /**
     * Elimina todos los logs de un usuario.
     * Útil para limpieza de datos o logout.
     */
    suspend fun deleteAllLogsForUser(userId: String) {
        withContext(Dispatchers.IO) {
            sessionLogDao.deleteAllLogsForUser(userId)
            Log.d(TAG, "🗑️ Deleted all logs for user: $userId")
        }
    }
}

