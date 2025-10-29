package com.g22.orbitsoundkotlin.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Servicio centralizado para analytics de música y comportamiento del usuario.
 * 
 * IMPORTANTE: Activar en Firebase Console:
 * - Analytics → Events → Ver todos los eventos personalizados
 * - Analytics → DebugView → Ver eventos en tiempo real durante desarrollo
 */
object MusicAnalytics {
    
    private val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }
    
    // ═══════════════════════════════════════════════════════════
    // SECCIONES DE LIBRARY - Engagement por Recomendación
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Registra cuando un usuario hace click en una canción de una sección.
     * 
     * @param sectionTitle Título de la sección (ej: "Morning Orbit", "Pegasus Flight")
     * @param sectionType Tipo de recomendación ("time", "constellation", "emotion", "default")
     * @param trackTitle Título de la canción
     * @param trackArtist Artista de la canción
     * @param sectionPosition Posición de la sección (1-4)
     */
    fun trackSectionClick(
        sectionTitle: String,
        sectionType: String,
        trackTitle: String,
        trackArtist: String,
        sectionPosition: Int
    ) {
        val params = Bundle().apply {
            putString("section_title", sectionTitle)
            putString("section_type", sectionType)
            putString("track_title", trackTitle)
            putString("track_artist", trackArtist)
            putInt("section_position", sectionPosition)
        }
        analytics.logEvent("section_track_click", params)
    }
    
    /**
     * Registra cuando una sección se carga exitosamente con canciones.
     * 
     * @param sectionTitle Título de la sección
     * @param sectionType Tipo de recomendación
     * @param trackCount Número de canciones obtenidas
     * @param query Query usado para buscar en Spotify
     */
    fun trackSectionLoaded(
        sectionTitle: String,
        sectionType: String,
        trackCount: Int,
        query: String
    ) {
        val params = Bundle().apply {
            putString("section_title", sectionTitle)
            putString("section_type", sectionType)
            putInt("track_count", trackCount)
            putString("spotify_query", query)
        }
        analytics.logEvent("section_loaded", params)
    }
    
    /**
     * Registra cuando una sección falla al cargar.
     */
    fun trackSectionError(
        sectionTitle: String,
        sectionType: String,
        errorMessage: String
    ) {
        val params = Bundle().apply {
            putString("section_title", sectionTitle)
            putString("section_type", sectionType)
            putString("error_message", errorMessage)
        }
        analytics.logEvent("section_load_error", params)
    }
    
    // ═══════════════════════════════════════════════════════════
    // BÚSQUEDA - Patrones de uso
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Registra cuando un usuario realiza una búsqueda.
     * 
     * @param query Texto buscado
     * @param resultCount Número de resultados obtenidos
     */
    fun trackSearch(query: String, resultCount: Int) {
        val params = Bundle().apply {
            putString("search_query", query)
            putInt("result_count", resultCount)
        }
        analytics.logEvent("music_search", params)
    }
    
    /**
     * Registra cuando un usuario hace click en un resultado de búsqueda.
     */
    fun trackSearchResultClick(
        query: String,
        trackTitle: String,
        trackArtist: String,
        resultPosition: Int
    ) {
        val params = Bundle().apply {
            putString("search_query", query)
            putString("track_title", trackTitle)
            putString("track_artist", trackArtist)
            putInt("result_position", resultPosition)
        }
        analytics.logEvent("search_result_click", params)
    }
    
    // ═══════════════════════════════════════════════════════════
    // RECOMENDACIONES - Efectividad del Motor
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Registra qué contexto del usuario generó las recomendaciones.
     * 
     * @param hasConstellations Si el usuario tiene constelaciones guardadas
     * @param hasEmotions Si el usuario tiene emociones recientes
     * @param constellationCount Número de constelaciones
     * @param emotionCount Número de emociones recientes
     * @param timeOfDay "morning", "afternoon", "evening", "night"
     */
    fun trackRecommendationContext(
        hasConstellations: Boolean,
        hasEmotions: Boolean,
        constellationCount: Int,
        emotionCount: Int,
        timeOfDay: String
    ) {
        val params = Bundle().apply {
            putBoolean("has_constellations", hasConstellations)
            putBoolean("has_emotions", hasEmotions)
            putInt("constellation_count", constellationCount)
            putInt("emotion_count", emotionCount)
            putString("time_of_day", timeOfDay)
        }
        analytics.logEvent("recommendation_context", params)
    }
    
    // ═══════════════════════════════════════════════════════════
    // NAVEGACIÓN - Flujo del usuario
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Registra cuando el usuario entra a LibraryScreen.
     */
    fun trackLibraryScreenView() {
        analytics.logEvent("library_screen_view", null)
    }
    
    /**
     * Registra cuando el usuario abre el modal de detalle de una canción.
     */
    fun trackTrackDetailView(trackTitle: String, trackArtist: String) {
        val params = Bundle().apply {
            putString("track_title", trackTitle)
            putString("track_artist", trackArtist)
        }
        analytics.logEvent("track_detail_view", params)
    }
}

