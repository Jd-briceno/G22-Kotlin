package com.g22.orbitsoundkotlin.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Centralized service for music and user behavior analytics.
 * 
 * IMPORTANT: Enable in Firebase Console:
 * - Analytics → Events → View all custom events
 * - Analytics → DebugView → View events in real-time during development
 */
object MusicAnalytics {
    
    private var analyticsInstance: FirebaseAnalytics? = null
    
    /**
     * Initializes the analytics service.
     * Must be called from MainActivity or Application.
     */
    fun initialize(context: Context) {
        if (analyticsInstance == null) {
            analyticsInstance = FirebaseAnalytics.getInstance(context)
        }
    }
    
    private val analytics: FirebaseAnalytics
        get() = analyticsInstance ?: throw IllegalStateException(
            "MusicAnalytics has not been initialized. Call MusicAnalytics.initialize(context) first."
        )
    
    // ═══════════════════════════════════════════════════════════
    // LIBRARY SECTIONS - Engagement by Recommendation
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tracks when a user clicks on a track from a section.
     * 
     * @param sectionTitle Section title (e.g., "Morning Orbit", "Pegasus Flight")
     * @param sectionType Recommendation type ("time", "constellation", "emotion", "default")
     * @param trackTitle Track title
     * @param trackArtist Track artist
     * @param sectionPosition Section position (1-4)
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
     * Tracks when a section loads successfully with tracks.
     * 
     * @param sectionTitle Section title
     * @param sectionType Recommendation type
     * @param trackCount Number of tracks obtained
     * @param query Query used to search in Spotify
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
     * Tracks when a section fails to load.
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
    // SEARCH - Usage Patterns
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tracks when a user performs a search.
     * 
     * @param query Search text
     * @param resultCount Number of results obtained
     */
    fun trackSearch(query: String, resultCount: Int) {
        val params = Bundle().apply {
            putString("search_query", query)
            putInt("result_count", resultCount)
        }
        analytics.logEvent("music_search", params)
    }
    
    /**
     * Tracks when a user clicks on a search result.
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
    // RECOMMENDATIONS - Engine Effectiveness
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tracks what user context generated the recommendations.
     * 
     * @param hasConstellations Whether the user has saved constellations
     * @param hasEmotions Whether the user has recent emotions
     * @param constellationCount Number of constellations
     * @param emotionCount Number of recent emotions
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
    // NAVIGATION - User Flow
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tracks when the user enters LibraryScreen.
     */
    fun trackLibraryScreenView() {
        analytics.logEvent("library_screen_view", null)
    }
    
    /**
     * Tracks when the user opens the track detail modal.
     */
    fun trackTrackDetailView(trackTitle: String, trackArtist: String) {
        val params = Bundle().apply {
            putString("track_title", trackTitle)
            putString("track_artist", trackArtist)
        }
        analytics.logEvent("track_detail_view", params)
    }
    
    // ═══════════════════════════════════════════════════════════
    // ARES MODE - AI Emotional Recommendations
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Tracks when the user enters Ares: Modo Emocional screen.
     */
    fun trackAresScreenView() {
        analytics.logEvent("ares_screen_view", null)
    }
    
    /**
     * Tracks when the user generates a playlist with AI.
     * 
     * @param userInput The emotional input text from the user
     * @param queriesGenerated Number of queries generated by Gemini
     * @param tracksFound Number of tracks found from Spotify
     * @param success Whether the generation was successful
     */
    fun trackAresGeneration(
        userInput: String,
        queriesGenerated: Int,
        tracksFound: Int,
        success: Boolean
    ) {
        val params = Bundle().apply {
            putString("user_input", userInput)
            putInt("input_length", userInput.length)
            putInt("queries_count", queriesGenerated)
            putInt("tracks_count", tracksFound)
            putBoolean("success", success)
        }
        analytics.logEvent("ares_playlist_generated", params)
    }
}

