package com.g22.orbitsoundkotlin.data.repositories

import android.util.Log
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.local.entities.*
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatsPeriod
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityItem
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityStatChip
import com.g22.orbitsoundkotlin.ui.viewmodels.JournalEntry
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository para Activity Stats.
 * 
 * Responsabilidades:
 * - Calcular summary desde entidades existentes (LoginTelemetry, Outbox, SearchHistory)
 * - Gestionar entradas del journal
 * - Implementar caching en memoria con TTL
 * - Proporcionar datos para recent activity
 * 
 * CONCURRENCIA: Todas las operaciones de Room corren en Dispatchers.IO
 * CACHING: Cache en memoria con TTL de 5 minutos para summaries
 * SEGURIDAD: Todas las queries filtran por userId del usuario autenticado
 */
class ActivityStatsRepository(
    private val db: AppDatabase
) {
    private val summaryDao = db.userDailyActivitySummaryDao()
    private val journalDao = db.journalEntryDao()
    private val telemetryDao = db.telemetryDao()
    private val outboxDao = db.outboxDao()
    private val libraryCacheDao = db.libraryCacheDao()
    
    private val TAG = "ActivityStatsRepo"
    
    init {
        Log.d(TAG, "🏗️ ActivityStatsRepository initialized")
    }
    
    // Cache en memoria para summaries por período
    // TTL: 5 minutos
    private data class CachedSummary(
        val data: ActivitySummary,
        val timestamp: Long
    )
    
    private val summaryCache = mutableMapOf<String, CachedSummary>()
    private val CACHE_TTL_MS = 30 * 1000L // 30 segundos (para permitir actualizaciones más frecuentes)
    
    /**
     * Obtiene el summary de actividad para un período específico.
     * 
     * Estrategia:
     * 1. Verificar cache en memoria (si no expirado, devolver)
     * 2. Calcular summary desde entidades existentes
     * 3. Actualizar/crear UserDailyActivitySummaryEntity
     * 4. Actualizar cache
     * 
     * CONCURRENCIA: Ejecuta en Dispatchers.IO
     * CACHING: Cache en memoria con TTL de 5 minutos
     */
    suspend fun getSummary(
        userId: String,
        userEmail: String?,
        period: ActivityStatsPeriod
    ): ActivitySummary = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 getSummary called - userId: $userId, period: ${period.displayName}")
        val cacheKey = "${userId}_${period.name}"
        val cached = summaryCache[cacheKey]
        
        // Verificar cache
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            Log.d(TAG, "✅ Cache HIT for period: ${period.displayName} - sessions: ${cached.data.sessionsCount}, time: ${cached.data.totalTimeMinutes}min, action: '${cached.data.mostCommonAction}'")
            // Invalidar cache si los datos están vacíos (para forzar recálculo)
            // También invalidar si el cache es muy antiguo (más de 1 minuto) para permitir actualizaciones
            val cacheAge = System.currentTimeMillis() - cached.timestamp
            if ((cached.data.sessionsCount == 0 && cached.data.totalTimeMinutes == 0 && cached.data.mostCommonAction.isEmpty()) || cacheAge > 60000) {
                Log.d(TAG, "⚠️ Cache invalidated: empty=${cached.data.sessionsCount == 0}, age=${cacheAge}ms")
                summaryCache.remove(cacheKey)
            } else {
                Log.d(TAG, "✅ Using cached data")
                return@withContext cached.data
            }
        }
        
        Log.d(TAG, "❌ Cache MISS for period: ${period.displayName}, calculating...")
        
        // Calcular rango de fechas para el período
        val (startDate, endDate) = calculateDateRange(period)
        val (startTimestamp, endTimestamp) = calculateTimestampRange(period)
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Log.d(TAG, "📅 Period range: ${dateFormat.format(Date(startTimestamp))} to ${dateFormat.format(Date(endTimestamp))}")
        
        // Obtener datos de fuentes existentes
        val allTelemetry = telemetryDao.getTelemetryInTimeRange(startTimestamp, endTimestamp)
        Log.d(TAG, "📥 Total telemetry in range: ${allTelemetry.size}")
        
        val loginTelemetry = allTelemetry.filter { it.success && (userEmail == null || it.email.equals(userEmail, ignoreCase = true)) }
        Log.d(TAG, "📥 Filtered successful logins: ${loginTelemetry.size} (userEmail: $userEmail)")
        
        // Obtener todas las operaciones (sincronizadas y no sincronizadas) para el período
        // NOTA: Para Activity Stats, todas las operaciones en OutboxEntity son del usuario actual
        // No necesitamos filtrar por userId del payload porque el contexto ya es del usuario autenticado
        val allOutboxOps = outboxDao.getOperationsInTimeRange(startTimestamp, endTimestamp)
        Log.d(TAG, "📥 Total outbox operations in range: ${allOutboxOps.size}")
        
        // Agrupar por tipo para debugging
        val opsByType = allOutboxOps.groupBy { it.operationType }
        opsByType.forEach { (type, ops) ->
            Log.d(TAG, "  - ${type.name}: ${ops.size} operations")
        }
        
        val outboxOperations = allOutboxOps
        
        val searchHistory = libraryCacheDao.getRecentSearches(userId, limit = 1000)
        Log.d(TAG, "📥 Total searches for user: ${searchHistory.size}")
        
        val filteredSearches = searchHistory.filter { it.timestamp >= startTimestamp }
        Log.d(TAG, "📥 Filtered searches in period: ${filteredSearches.size}")
        
        Log.d(TAG, "📊 Final data: ${loginTelemetry.size} logins, ${outboxOperations.size} operations, ${filteredSearches.size} searches")
        
        // Calcular métricas
        val sessionsCount = calculateSessionsCount(loginTelemetry, outboxOperations, filteredSearches, startTimestamp)
        Log.d(TAG, "📊 Calculated sessions: $sessionsCount")
        
        val totalTimeMinutes = calculateTotalTimeMinutes(
            loginTelemetry, 
            outboxOperations, 
            filteredSearches, 
            sessionsCount
        )
        Log.d(TAG, "📊 Calculated total time: ${totalTimeMinutes} minutes")
        
        val mostCommonAction = calculateMostCommonAction(outboxOperations, filteredSearches)
        Log.d(TAG, "📊 Most common action: $mostCommonAction")
        
        // Crear summary
        val summary = ActivitySummary(
            sessionsCount = sessionsCount,
            totalTimeMinutes = totalTimeMinutes,
            mostCommonAction = mostCommonAction
        )
        
        // Guardar summary diario para hoy (si aplica)
        val todayDate = formatDate(System.currentTimeMillis())
        if (isDateInRange(todayDate, startDate, endDate)) {
            val dailySummary = UserDailyActivitySummaryEntity(
                userId = userId,
                date = todayDate,
                sessionsCount = sessionsCount,
                totalTimeMinutes = totalTimeMinutes,
                mostCommonAction = mostCommonAction,
                lastUpdatedAt = System.currentTimeMillis(),
                isSynced = false // Marcar como no sincronizado
            )
            summaryDao.upsertSummary(dailySummary)
            Log.d(TAG, "💾 Daily summary saved for date: $todayDate")
        }
        
        // Actualizar cache solo si hay datos reales
        if (sessionsCount > 0 || totalTimeMinutes > 0 || mostCommonAction.isNotEmpty()) {
            summaryCache[cacheKey] = CachedSummary(summary, System.currentTimeMillis())
            Log.d(TAG, "✅ Summary calculated and cached: sessions=$sessionsCount, time=${totalTimeMinutes}min, action=$mostCommonAction")
        } else {
            Log.w(TAG, "⚠️ Summary is empty - not caching. Data: logins=${loginTelemetry.size}, ops=${outboxOperations.size}, searches=${filteredSearches.size}")
        }
        
        summary
    }
    
    /**
     * Obtiene la lista de actividad reciente para un período.
     * 
     * Construye items amigables para la UI desde:
     * - LoginTelemetryEntity (sesiones)
     * - OutboxEntity (acciones: likes, updates, etc.)
     * - SearchHistoryEntity (búsquedas)
     */
    suspend fun getRecentActivity(
        userId: String,
        userEmail: String?,
        period: ActivityStatsPeriod
    ): List<ActivityItem> = withContext(Dispatchers.IO) {
        val (startTimestamp, endTimestamp) = calculateTimestampRange(period)
        
        // Obtener datos
        val loginTelemetry = telemetryDao.getTelemetryInTimeRange(startTimestamp, endTimestamp)
            .filter { it.success && (userEmail == null || it.email.equals(userEmail, ignoreCase = true)) }
        
        // Obtener todas las operaciones (sincronizadas y no sincronizadas) para el período
        // NOTA: Para Activity Stats, todas las operaciones en OutboxEntity son del usuario actual
        // No necesitamos filtrar por userId del payload porque el contexto ya es del usuario autenticado
        val outboxOperations = outboxDao.getOperationsInTimeRange(startTimestamp, endTimestamp)
        
        val searchHistory = libraryCacheDao.getRecentSearches(userId, limit = 1000)
            .filter { it.timestamp >= startTimestamp }
        
        Log.d(TAG, "📊 Recent activity data: ${loginTelemetry.size} logins, ${outboxOperations.size} operations, ${searchHistory.size} searches")
        
        // Agrupar por día y construir items
        val activityItems = mutableListOf<ActivityItem>()
        
        // Agrupar logins por día
        val loginsByDay = loginTelemetry.groupBy { 
            formatDate(it.timestamp)
        }
        
        // Agrupar búsquedas por día
        val searchesByDay = searchHistory.groupBy { 
            formatDate(it.timestamp)
        }
        
        // Agrupar operaciones por día
        val operationsByDay = outboxOperations.groupBy { 
            formatDate(it.createdAt)
        }
        
        // Combinar y crear items
        val allDays = (loginsByDay.keys + searchesByDay.keys + operationsByDay.keys).sortedDescending()
        
        allDays.take(10).forEach { date ->
            val dayLogins = loginsByDay[date] ?: emptyList()
            val daySearches = searchesByDay[date] ?: emptyList()
            val dayOperations = operationsByDay[date] ?: emptyList()
            
            if (dayLogins.isNotEmpty() || daySearches.isNotEmpty() || dayOperations.isNotEmpty()) {
                val latestTimestamp = listOfNotNull(
                    dayLogins.maxOfOrNull { it.timestamp },
                    daySearches.maxOfOrNull { it.timestamp },
                    dayOperations.maxOfOrNull { it.createdAt }
                ).maxOrNull() ?: System.currentTimeMillis()
                
                val summary = buildActivitySummary(daySearches, dayOperations)
                val stats = buildActivityStats(daySearches, dayOperations)
                
                activityItems.add(
                    ActivityItem(
                        id = "activity_${date}_${latestTimestamp}",
                        dateTime = formatDateTime(latestTimestamp),
                        summary = summary,
                        stats = stats
                    )
                )
            }
        }
        
        activityItems
    }
    
    // ========== JOURNAL METHODS ==========
    
    /**
     * Resumen de un día en la timeline del journal.
     */
    data class JournalDaySummary(
        val date: String, // Formato "YYYY-MM-DD"
        val dateTimestamp: Long, // Timestamp del inicio del día
        val displayLabel: String, // "Today", "Yesterday", "Mon 24", etc.
        val entryCount: Int, // Número de entradas en ese día
        val hasEntries: Boolean // true si entryCount > 0
    )
    
    /**
     * Obtiene la timeline de los últimos N días con conteo de entradas.
     * Incluye TODOS los días del rango, incluso si no tienen entradas.
     * 
     * CONCURRENCIA: Ejecuta en Dispatchers.IO
     * SEGURIDAD: Manejo robusto de errores para evitar crashes
     */
    suspend fun getJournalTimeline(
        userId: String,
        daysBack: Int = 30
    ): List<JournalDaySummary> = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            val today = calendar.clone() as Calendar
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            
            val fromDate = calendar.clone() as Calendar
            fromDate.add(Calendar.DAY_OF_YEAR, -daysBack)
            fromDate.set(Calendar.HOUR_OF_DAY, 0)
            fromDate.set(Calendar.MINUTE, 0)
            fromDate.set(Calendar.SECOND, 0)
            fromDate.set(Calendar.MILLISECOND, 0)
            
            val fromDateStr = formatDate(fromDate.timeInMillis)
            val toDateStr = formatDate(today.timeInMillis)
            
            // Obtener todas las entradas en el rango de forma segura
            val entries = try {
                journalDao.getEntriesInDateRange(userId, fromDateStr, toDateStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting entries in date range", e)
                emptyList() // Retornar lista vacía en caso de error
            }
            
            // Agrupar por fecha y contar
            val countMap = entries.groupBy { it.date }.mapValues { it.value.size }
            
            // Construir lista de todos los días del rango
            val timeline = mutableListOf<JournalDaySummary>()
            val currentDay = today.clone() as Calendar
            
            repeat(daysBack + 1) {
                val dateStr = formatDate(currentDay.timeInMillis)
                val entryCount = countMap[dateStr] ?: 0
                val displayLabel = formatDayLabel(currentDay, today)
                
                timeline.add(
                    JournalDaySummary(
                        date = dateStr,
                        dateTimestamp = currentDay.timeInMillis,
                        displayLabel = displayLabel,
                        entryCount = entryCount,
                        hasEntries = entryCount > 0
                    )
                )
                
                currentDay.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            timeline.reversed() // Más antiguo primero, más reciente al final
        } catch (e: Exception) {
            Log.e(TAG, "Error building journal timeline", e)
            emptyList() // Retornar lista vacía en caso de error
        }
    }
    
    /**
     * Formatea la etiqueta de un día para la timeline.
     * Retorna "Today", "Yesterday", o "Mon 24" según corresponda.
     */
    private fun formatDayLabel(day: Calendar, today: Calendar): String {
        val yesterday = today.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return when {
            isSameDay(day, today) -> "Today"
            isSameDay(day, yesterday) -> "Yesterday"
            else -> {
                val dayOfWeek = day.get(Calendar.DAY_OF_WEEK)
                val month = day.get(Calendar.MONTH)
                val dayOfMonth = day.get(Calendar.DAY_OF_MONTH)
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                         "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${dayNames[dayOfWeek - 1]} ${monthNames[month]} $dayOfMonth"
            }
        }
    }
    
    /**
     * Obtiene entradas del journal para una fecha específica.
     */
    suspend fun getJournalEntriesForDate(
        userId: String,
        dateTimestamp: Long
    ): List<JournalEntry> = withContext(Dispatchers.IO) {
        val date = formatDate(dateTimestamp)
        val entities = journalDao.getEntriesForDate(userId, date)
        
        entities.map { entity ->
            JournalEntry(
                id = entity.id.toString(),
                date = formatDateDisplay(entity.date),
                text = entity.text,
                timestamp = entity.createdAt,
                time = formatTime(entity.createdAt)
            )
        }
    }
    
    /**
     * Obtiene todas las fechas que tienen entradas para navegación.
     */
    suspend fun getAvailableJournalDates(userId: String): List<Long> = withContext(Dispatchers.IO) {
        val dateStrings = journalDao.getDatesWithEntries(userId)
        dateStrings.mapNotNull { dateStr ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                sdf.parse(dateStr)?.time
            } catch (e: Exception) {
                null
            }
        }.sortedDescending()
    }
    
    /**
     * Agrega una nueva entrada al journal.
     */
    suspend fun addJournalEntry(
        userId: String,
        dateTimestamp: Long,
        text: String
    ): JournalEntry = withContext(Dispatchers.IO) {
        val date = formatDate(dateTimestamp)
        val now = System.currentTimeMillis()
        
        val entity = JournalEntryEntity(
            userId = userId,
            date = date,
            text = text.trim(),
            createdAt = now,
            updatedAt = now
        )
        
        val id = journalDao.insertEntry(entity)
        
        JournalEntry(
            id = id.toString(),
            date = formatDateDisplay(date),
            text = entity.text,
            timestamp = entity.createdAt,
            time = formatTime(entity.createdAt)
        )
    }
    
    /**
     * Actualiza una entrada del journal.
     */
    suspend fun updateJournalEntry(
        userId: String,
        entryId: String,
        newText: String
    ): Boolean = withContext(Dispatchers.IO) {
        val id = entryId.toLongOrNull() ?: return@withContext false
        val existing = journalDao.getEntryById(userId, id) ?: return@withContext false
        
        val updated = existing.copy(
            text = newText.trim(),
            updatedAt = System.currentTimeMillis()
        )
        
        journalDao.updateEntry(updated)
        true
    }
    
    /**
     * Elimina una entrada del journal.
     */
    suspend fun deleteJournalEntry(
        userId: String,
        entryId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val id = entryId.toLongOrNull() ?: return@withContext false
        val existing = journalDao.getEntryById(userId, id) ?: return@withContext false
        
        journalDao.deleteEntry(existing)
        true
    }
    
    /**
     * Obtiene summaries no sincronizados para el Worker.
     */
    suspend fun getUnsyncedSummaries(userId: String): List<UserDailyActivitySummaryEntity> = 
        withContext(Dispatchers.IO) {
            summaryDao.getUnsyncedSummaries(userId)
        }
    
    /**
     * Marca summaries como sincronizados.
     */
    suspend fun markSummariesAsSynced(ids: List<Long>) = withContext(Dispatchers.IO) {
        summaryDao.markAsSynced(ids)
    }
    
    /**
     * Limpia el cache en memoria.
     */
    fun clearCache() {
        summaryCache.clear()
        Log.d(TAG, "🗑️ Summary cache cleared")
    }
    
    // ========== PRIVATE HELPER METHODS ==========
    
    private fun calculateDateRange(period: ActivityStatsPeriod): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = formatDate(calendar.timeInMillis)
        
        when (period) {
            ActivityStatsPeriod.LAST_24H -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            ActivityStatsPeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            ActivityStatsPeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
            }
        }
        
        val startDate = formatDate(calendar.timeInMillis)
        return Pair(startDate, endDate)
    }
    
    private fun calculateTimestampRange(period: ActivityStatsPeriod): Pair<Long, Long> {
        val endTimestamp = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endTimestamp
        
        when (period) {
            ActivityStatsPeriod.LAST_24H -> {
                calendar.add(Calendar.HOUR_OF_DAY, -24)
            }
            ActivityStatsPeriod.LAST_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            ActivityStatsPeriod.LAST_30_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
            }
        }
        
        val startTimestamp = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Log.d(TAG, "⏰ Time range for ${period.displayName}: ${dateFormat.format(Date(startTimestamp))} to ${dateFormat.format(Date(endTimestamp))}")
        return Pair(startTimestamp, endTimestamp)
    }
    
    /**
     * Calcula el número de sesiones en el período.
     * Una sesión comienza con un login exitoso y termina después de 30 minutos de inactividad.
     * 
     * Si no hay logins, pero hay operaciones o búsquedas, cuenta 1 sesión (usuario ya estaba logueado).
     * 
     * CONCURRENCIA: Ejecuta en el mismo contexto (Dispatchers.IO ya aplicado)
     */
    private fun calculateSessionsCount(
        logins: List<LoginTelemetryEntity>,
        operations: List<OutboxEntity>,
        searches: List<SearchHistoryEntity>,
        startTimestamp: Long
    ): Int {
        // Si hay operaciones o búsquedas, significa que el usuario está activo
        // Incluso si no hay logins nuevos en el período (ya estaba logueado)
        val hasActivity = operations.isNotEmpty() || searches.isNotEmpty()
        
        if (logins.isEmpty()) {
            if (hasActivity) {
                // Usuario está activo pero no hay logins nuevos = 1 sesión continua
                Log.d(TAG, "⚠️ No logins found in period, but has activity - assuming 1 active session")
                return 1
            } else {
                // No hay actividad en absoluto
                Log.d(TAG, "⚠️ No logins and no activity in period")
                return 0
            }
        }
        
        // Ordenar logins por timestamp
        val sortedLogins = logins.sortedBy { it.timestamp }
        
        // Agrupar logins en sesiones usando ventana de 30 minutos
        val SESSION_INACTIVITY_WINDOW_MS = 30 * 60 * 1000L // 30 minutos
        var sessionCount = 0
        var lastSessionEnd: Long? = null
        
        for (login in sortedLogins) {
            if (lastSessionEnd == null || login.timestamp > lastSessionEnd) {
                // Nuevo login está fuera de la ventana de la sesión anterior
                sessionCount++
                lastSessionEnd = login.timestamp + SESSION_INACTIVITY_WINDOW_MS
            } else {
                // Login está dentro de la ventana de la sesión actual
                // Extender la ventana de la sesión
                lastSessionEnd = login.timestamp + SESSION_INACTIVITY_WINDOW_MS
            }
        }
        
        Log.d(TAG, "📊 Sessions calculated from ${sortedLogins.size} logins: $sessionCount sessions")
        return sessionCount
    }
    
    /**
     * Calcula el tiempo total en minutos basado en eventos reales.
     * 
     * Estrategia:
     * 1. Agrupar eventos (logins, operaciones, búsquedas) por sesión
     * 2. Para cada sesión, calcular duración desde primer evento hasta último evento
     * 3. Sumar todas las duraciones
     * 
     * Si no hay eventos suficientes, usa estimación conservadora.
     * 
     * CONCURRENCIA: Ejecuta en el mismo contexto (Dispatchers.IO ya aplicado)
     */
    private fun calculateTotalTimeMinutes(
        logins: List<LoginTelemetryEntity>,
        operations: List<OutboxEntity>,
        searches: List<SearchHistoryEntity>,
        sessionsCount: Int
    ): Int {
        if (sessionsCount == 0) return 0
        
        val SESSION_INACTIVITY_WINDOW_MS = 30 * 60 * 1000L // 30 minutos
        
        // Obtener todos los eventos con sus timestamps
        val allEvents = mutableListOf<Pair<Long, String>>()
        
        // Agregar logins
        logins.forEach { login ->
            allEvents.add(Pair(login.timestamp, "login"))
        }
        
        // Agregar operaciones
        operations.forEach { op ->
            allEvents.add(Pair(op.createdAt, "operation"))
        }
        
        // Agregar búsquedas
        searches.forEach { search ->
            allEvents.add(Pair(search.timestamp, "search"))
        }
        
        if (allEvents.isEmpty()) {
            // Si no hay eventos, usar estimación conservadora basada en sesiones
            val estimated = sessionsCount * 10 // 10 minutos por sesión como mínimo
            Log.d(TAG, "⚠️ No events found, using estimated time: $estimated minutes")
            return estimated
        }
        
        // Ordenar eventos por timestamp
        val sortedEvents = allEvents.sortedBy { it.first }
        
        // Agrupar eventos por sesión y calcular duración de cada sesión
        var totalMinutes = 0L
        var currentSessionStart: Long? = null
        var currentSessionLastEvent: Long? = null
        
        for ((timestamp, _) in sortedEvents) {
            if (currentSessionStart == null) {
                // Iniciar nueva sesión
                currentSessionStart = timestamp
                currentSessionLastEvent = timestamp
            } else if (timestamp <= (currentSessionLastEvent!! + SESSION_INACTIVITY_WINDOW_MS)) {
                // Evento está dentro de la ventana de la sesión actual
                currentSessionLastEvent = timestamp
            } else {
                // Cerrar sesión anterior y calcular duración
                val sessionDuration = currentSessionLastEvent!! - currentSessionStart!!
                totalMinutes += sessionDuration
                
                // Iniciar nueva sesión
                currentSessionStart = timestamp
                currentSessionLastEvent = timestamp
            }
        }
        
        // Cerrar última sesión
        if (currentSessionStart != null && currentSessionLastEvent != null) {
            val sessionDuration = currentSessionLastEvent!! - currentSessionStart!!
            totalMinutes += sessionDuration
        }
        
        // Convertir a minutos (redondear hacia arriba)
        val totalMinutesInt = ((totalMinutes + 59999) / 60000).toInt()
        
        // Asegurar mínimo razonable: al menos 5 minutos por sesión si hay actividad
        // Si hay operaciones o búsquedas, asumimos que el usuario estuvo activo
        val minTime = if (operations.isNotEmpty() || searches.isNotEmpty()) {
            // Si hay actividad pero el tiempo calculado es muy bajo, usar estimación
            if (totalMinutesInt < 5) {
                // Estimar basado en número de acciones (mínimo 2 minutos por acción)
                val estimatedFromActions = (operations.size + searches.size) * 2
                maxOf(sessionsCount * 5, estimatedFromActions, 5)
            } else {
                maxOf(sessionsCount * 5, 5)
            }
        } else {
            sessionsCount // Mínimo 1 minuto por sesión
        }
        
        val finalTime = maxOf(totalMinutesInt, minTime)
        Log.d(TAG, "📊 Calculated time: $totalMinutesInt minutes (from events), minTime: $minTime, final: $finalTime minutes")
        return finalTime
    }
    
    private fun calculateMostCommonAction(
        operations: List<OutboxEntity>,
        searches: List<SearchHistoryEntity>
    ): String {
        val actionCounts = mutableMapOf<String, Int>()
        
        // Contar operaciones de Outbox
        operations.forEach { op ->
            when (op.operationType) {
                OutboxOperationType.QUICK_ACTION_LIKE -> {
                    actionCounts["like"] = actionCounts.getOrDefault("like", 0) + 1
                }
                OutboxOperationType.UPDATE_MOOD -> {
                    actionCounts["mood"] = actionCounts.getOrDefault("mood", 0) + 1
                }
                OutboxOperationType.UPSERT_INTERESTS -> {
                    actionCounts["interests"] = actionCounts.getOrDefault("interests", 0) + 1
                }
                else -> {}
            }
        }
        
        // Contar búsquedas
        if (searches.isNotEmpty()) {
            actionCounts["search"] = searches.size
        }
        
        return actionCounts.maxByOrNull { it.value }?.key?.let {
            when (it) {
                "search" -> "Searching music"
                "like" -> "Liking tracks"
                "mood" -> "Updating mood"
                "interests" -> "Updating interests"
                else -> "Using the app"
            }
        } ?: "Using the app"
    }
    
    /**
     * Construye un resumen de actividad amigable para la UI.
     * 
     * Prioriza:
     * 1. Búsquedas (más específicas)
     * 2. Likes (acciones comunes)
     * 3. Mood updates (menos frecuentes)
     * 4. Otras operaciones
     */
    private fun buildActivitySummary(
        searches: List<SearchHistoryEntity>,
        operations: List<OutboxEntity>
    ): String {
        val parts = mutableListOf<String>()
        
        // Búsquedas: mostrar la más reciente o un conteo si hay muchas
        if (searches.isNotEmpty()) {
            val uniqueQueries = searches.map { it.query }.distinct()
            when {
                uniqueQueries.size == 1 -> {
                    parts.add("searched for '${uniqueQueries.first()}'")
                }
                uniqueQueries.size <= 3 -> {
                    parts.add("searched for ${uniqueQueries.size} different things")
                }
                else -> {
                    parts.add("searched ${searches.size} times")
                }
            }
        }
        
        // Likes
        val likes = operations.count { it.operationType == OutboxOperationType.QUICK_ACTION_LIKE }
        if (likes > 0) {
            parts.add("liked $likes track${if (likes > 1) "s" else ""}")
        }
        
        // Mood updates
        val moodUpdates = operations.count { it.operationType == OutboxOperationType.UPDATE_MOOD }
        if (moodUpdates > 0) {
            parts.add("updated mood ${moodUpdates} time${if (moodUpdates > 1) "s" else ""}")
        }
        
        // Otras operaciones
        val otherOps = operations.count { 
            it.operationType != OutboxOperationType.QUICK_ACTION_LIKE && 
            it.operationType != OutboxOperationType.UPDATE_MOOD 
        }
        if (otherOps > 0 && parts.isEmpty()) {
            parts.add("performed $otherOps action${if (otherOps > 1) "s" else ""}")
        }
        
        return if (parts.isEmpty()) {
            "Used the app"
        } else {
            "You ${parts.joinToString(", ")}"
        }
    }
    
    /**
     * Construye la lista de chips de estadísticas para un item de actividad.
     * 
     * Muestra:
     * - Número de búsquedas
     * - Número de likes
     * - Número de mood updates
     * - Otras operaciones (si hay)
     */
    private fun buildActivityStats(
        searches: List<SearchHistoryEntity>,
        operations: List<OutboxEntity>
    ): List<ActivityStatChip> {
        val stats = mutableListOf<ActivityStatChip>()
        
        // Búsquedas
        if (searches.isNotEmpty()) {
            stats.add(ActivityStatChip("searches", searches.size))
        }
        
        // Likes
        val likes = operations.count { it.operationType == OutboxOperationType.QUICK_ACTION_LIKE }
        if (likes > 0) {
            stats.add(ActivityStatChip("likes", likes))
        }
        
        // Mood updates
        val moodUpdates = operations.count { it.operationType == OutboxOperationType.UPDATE_MOOD }
        if (moodUpdates > 0) {
            stats.add(ActivityStatChip("mood updates", moodUpdates))
        }
        
        // Otras operaciones (solo si no hay otras stats)
        if (stats.isEmpty()) {
            val otherOps = operations.size
            if (otherOps > 0) {
                stats.add(ActivityStatChip("actions", otherOps))
            }
        }
        
        return stats
    }
    
    /**
     * Extrae el userId del payload de OutboxEntity.
     * 
     * Intenta diferentes campos posibles:
     * - "userId" (más común)
     * - "uid" (alternativa)
     * - "user_id" (alternativa)
     * 
     * SEGURIDAD: Si no se puede extraer userId, retorna null para filtrar la operación.
     */
    private fun extractUserIdFromPayload(payload: JsonObject): String? {
        return try {
            // Intentar diferentes campos posibles
            payload.get("userId")?.asString
                ?: payload.get("uid")?.asString
                ?: payload.get("user_id")?.asString
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting userId from payload: ${e.message}")
            null
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestamp))
    }
    
    private fun formatDateDisplay(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return dateStr
        
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        calendar.time = date
        
        return when {
            isSameDay(calendar, today) -> "Today"
            isSameDay(calendar, yesterday) -> "Yesterday"
            else -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${dayNames[dayOfWeek - 1]} · ${monthNames[month]} $day"
            }
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val time = formatTime(timestamp)
        return "${dayNames[dayOfWeek - 1]} · $time"
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun isDateInRange(date: String, startDate: String, endDate: String): Boolean {
        return date >= startDate && date <= endDate
    }
}

/**
 * Summary de actividad para un período.
 */
data class ActivitySummary(
    val sessionsCount: Int,
    val totalTimeMinutes: Int,
    val mostCommonAction: String
)

