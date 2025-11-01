package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.FirestoreEmotionRepository
import com.g22.orbitsoundkotlin.data.MusicRecommendationEngine
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.SpotifyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class LibraryViewModel(
    private val spotifyService: SpotifyService = SpotifyService.Companion.getInstance()
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryViewModel"
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "Initializing LibraryViewModel")
        // 📊 Analytics: User entered LibraryScreen
        MusicAnalytics.trackLibraryScreenView()
        loadPersonalizedPlaylists()
    }
    
    /**
     * Updates recommendations based on user's constellations and emotions.
     * Can be called when the user updates their preferences.
     */
    fun refreshRecommendations(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        loadPersonalizedPlaylists(userConstellations, recentEmotions)
    }
    
    /**
     * 🔄 CASE B: Nested Coroutines with Multiple I/O Operations
     * 
     * Loads user's emotions from Firestore, then uses them to generate
     * personalized playlist recommendations from Spotify.
     * 
     * Demonstrates: Sequential I/O dependency chain with nested coroutines.
     */
    fun loadUserEmotionsAndRefresh(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "🔄 [CASE B] Starting nested I/O operations")
            
            try {
                // 1st I/O: Get user's recent emotions from Firestore
                val emotionsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "🔄 [CASE B - Level 1] Fetching emotions on: ${Thread.currentThread().name}")
                    getRecentEmotionsFromFirestore(userId, limit = 5)
                }
                
                val emotions = emotionsDeferred.await()
                Log.d(TAG, "✅ [CASE B - Level 1] Emotions loaded: ${emotions.size} emotions")
                
                // 2nd I/O (nested): Load personalized playlists using emotions
                val playlistsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "🔄 [CASE B - Level 2] Loading playlists on: ${Thread.currentThread().name}")
                    // This internally makes 4 parallel Spotify API calls
                    loadPersonalizedPlaylistsWithEmotions(emotions)
                }
                
                playlistsDeferred.await()
                Log.d(TAG, "✅ [CASE B - Level 2] All nested operations completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CASE B] Error in nested operations", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    /**
     * Helper: Fetches recent emotions from Firestore and converts to EmotionModel list.
     */
    private suspend fun getRecentEmotionsFromFirestore(userId: String, limit: Int): List<EmotionModel> {
        return withContext(Dispatchers.IO) {
            try {
                val emotionRepo = FirestoreEmotionRepository()
                // getEmotionLogs returns Flow, we take first emission
                val logs = emotionRepo.getEmotionLogs(userId).first()
                
                // Convert EmotionLog.EmotionEntry to EmotionModel
                val allEmotions = logs.flatMap { log -> 
                    log.emotions.map { entry -> 
                        EmotionModel(id = entry.id, name = entry.name)
                    }
                }
                
                // Return most recent N emotions
                allEmotions.take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching emotions from Firestore", e)
                emptyList()
            }
        }
    }
    
    /**
     * Helper: Wrapper to call loadPersonalizedPlaylists with emotions.
     */
    private suspend fun loadPersonalizedPlaylistsWithEmotions(emotions: List<EmotionModel>) {
        // Call existing loadPersonalizedPlaylists with emotions (no constellations for now)
        loadPersonalizedPlaylists(
            userConstellations = emptyList(),
            recentEmotions = emotions
        )
    }

    /**
     * 🎨 CASE C: Explicit Dispatcher Switch (I/O → Main)
     * 
     * Searches for tracks on Spotify with explicit dispatcher switching.
     * Network call uses IO dispatcher, UI update uses Main dispatcher.
     * 
     * Demonstrates: Explicit context switching between I/O and Main threads.
     */
    fun searchTracks(query: String) {
        if (query.isEmpty()) return
        
        Log.d(TAG, "🔍 [CASE C] Starting search with dispatcher switch: $query")
        viewModelScope.launch {
            _uiState.update { it.copy(searchLoading = true) }
            
            try {
                // Explicit I/O dispatcher for network call
                val tracks = withContext(Dispatchers.IO) {
                    Log.d(TAG, "🔄 [CASE C - IO] Searching on: ${Thread.currentThread().name}")
                    spotifyService.searchTracks(query)
                }
                
                Log.d(TAG, "✅ [CASE C] Fetched ${tracks.size} tracks")
                
                // Explicit Main dispatcher for UI update (academic - StateFlow is thread-safe)
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "🎨 [CASE C - Main] Updating UI on: ${Thread.currentThread().name}")
                    
                    // 📊 Analytics: Track search
                    MusicAnalytics.trackSearch(query, tracks.size)
                    
                    _uiState.update {
                        it.copy(
                            searchResults = tracks,
                            searchLoading = false,
                            lastSearchQuery = query
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [CASE C] Error in search", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            searchLoading = false,
                            error = e.message
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Selects a track from a recommendation section.
     * 📊 Tracks in Analytics which section the click came from.
     */
    fun selectTrackFromSection(
        track: Track,
        sectionPosition: Int  // 1-4
    ) {
        val sectionData = when (sectionPosition) {
            1 -> _uiState.value.section1
            2 -> _uiState.value.section2
            3 -> _uiState.value.section3
            4 -> _uiState.value.section4
            else -> null
        }
        
        sectionData?.let { data ->
            // Determine section type
            val sectionType = determineSectionType(data.section.title)
            
            // 📊 Analytics: Track section click
            MusicAnalytics.trackSectionClick(
                sectionTitle = data.section.title,
                sectionType = sectionType,
                trackTitle = track.title,
                trackArtist = track.artist,
                sectionPosition = sectionPosition
            )
            
            // 📊 Analytics: Track detail view
            MusicAnalytics.trackTrackDetailView(track.title, track.artist)
        }
        
        _uiState.update { it.copy(selectedTrack = track) }
    }
    
    /**
     * Selects a track from search results.
     * 📊 Tracks in Analytics the click on search result.
     */
    fun selectTrackFromSearch(track: Track, position: Int, query: String) {
        // 📊 Analytics: Track search result click
        MusicAnalytics.trackSearchResultClick(
            query = query,
            trackTitle = track.title,
            trackArtist = track.artist,
            resultPosition = position
        )
        
        // 📊 Analytics: Track detail view
        MusicAnalytics.trackTrackDetailView(track.title, track.artist)
        
        _uiState.update { it.copy(selectedTrack = track) }
    }
    
    /**
     * Determines the section type based on its title.
     */
    private fun determineSectionType(title: String): String {
        return when {
            title.contains("Morning", ignoreCase = true) ||
            title.contains("Afternoon", ignoreCase = true) ||
            title.contains("Twilight", ignoreCase = true) ||
            title.contains("Midnight", ignoreCase = true) -> "time"
            
            title.contains("Orbit Crew", ignoreCase = true) ||
            title.contains("Cosmic Chill", ignoreCase = true) ||
            title.contains("Saturn", ignoreCase = true) ||
            title.contains("Starlight", ignoreCase = true) -> "default"
            
            // Constellations
            title.contains("Cisne", ignoreCase = true) ||
            title.contains("Pegasus", ignoreCase = true) ||
            title.contains("Draco", ignoreCase = true) ||
            title.contains("Ursa", ignoreCase = true) ||
            title.contains("Cross", ignoreCase = true) ||
            title.contains("Phoenix", ignoreCase = true) -> "constellation"
            
            // Emotions
            title.contains("Joyful", ignoreCase = true) ||
            title.contains("Melancholy", ignoreCase = true) ||
            title.contains("Volcanic", ignoreCase = true) ||
            title.contains("Romantic", ignoreCase = true) ||
            title.contains("Calm", ignoreCase = true) ||
            title.contains("Brave", ignoreCase = true) ||
            title.contains("Discovery", ignoreCase = true) ||
            title.contains("Raw", ignoreCase = true) ||
            title.contains("Ambition", ignoreCase = true) ||
            title.contains("Comfort", ignoreCase = true) -> "emotion"
            
            else -> "unknown"
        }
    }
    
    fun dismissTrackDetail() {
        _uiState.update { it.copy(selectedTrack = null) }
    }
    
    /**
     * Loads personalized playlists using the recommendation engine.
     * ✨ Bonus: Fixed parallel calls to use Dispatchers.IO
     */
    private fun loadPersonalizedPlaylists(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        Log.d(TAG, "Loading personalized playlists...")
        Log.d(TAG, "Constellations: ${userConstellations.size}, Emotions: ${recentEmotions.size}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(playlistsLoading = true) }
            try {
                // Generate personalized sections
                val sections = MusicRecommendationEngine.generatePlaylistSections(
                    userConstellations = userConstellations,
                    recentEmotions = recentEmotions
                )
                
                Log.d(TAG, "Sections generated: ${sections.joinToString { it.title }}")
                Log.d(TAG, "Starting parallel searches")
                
                // 📊 Analytics: Track recommendation context
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val timeOfDay = when (hour) {
                    in 5..11 -> "morning"
                    in 12..17 -> "afternoon"
                    in 18..22 -> "evening"
                    else -> "night"
                }
                
                MusicAnalytics.trackRecommendationContext(
                    hasConstellations = userConstellations.isNotEmpty(),
                    hasEmotions = recentEmotions.isNotEmpty(),
                    constellationCount = userConstellations.size,
                    emotionCount = recentEmotions.size,
                    timeOfDay = timeOfDay
                )
                
                // ✨ Load tracks for each section in parallel with Dispatchers.IO
                val section1Deferred = async(Dispatchers.IO) { 
                    spotifyService.searchTracks(sections[0].query)
                }
                val section2Deferred = async(Dispatchers.IO) { 
                    spotifyService.searchTracks(sections[1].query)
                }
                val section3Deferred = async(Dispatchers.IO) { 
                    spotifyService.searchTracks(sections[2].query)
                }
                val section4Deferred = async(Dispatchers.IO) { 
                    spotifyService.searchTracks(sections[3].query)
                }
                
                val section1Songs = section1Deferred.await()
                val section2Songs = section2Deferred.await()
                val section3Songs = section3Deferred.await()
                val section4Songs = section4Deferred.await()
                
                Log.d(TAG, "Playlists loaded: ${section1Songs.size}, ${section2Songs.size}, ${section3Songs.size}, ${section4Songs.size}")
                
                // 📊 Analytics: Track each loaded section
                listOf(
                    Triple(sections[0], section1Songs, 1),
                    Triple(sections[1], section2Songs, 2),
                    Triple(sections[2], section3Songs, 3),
                    Triple(sections[3], section4Songs, 4)
                ).forEach { (section, songs, _) ->
                    val sectionType = determineSectionType(section.title)
                    MusicAnalytics.trackSectionLoaded(
                        sectionTitle = section.title,
                        sectionType = sectionType,
                        trackCount = songs.size,
                        query = section.query
                    )
                }
                
                _uiState.update { it.copy(
                    section1 = PlaylistSectionData(sections[0], section1Songs),
                    section2 = PlaylistSectionData(sections[1], section2Songs),
                    section3 = PlaylistSectionData(sections[2], section3Songs),
                    section4 = PlaylistSectionData(sections[3], section4Songs),
                    playlistsLoading = false
                )}
            } catch (e: Exception) {
                Log.e(TAG, "Error loading personalized playlists", e)
                _uiState.update { it.copy(
                    playlistsLoading = false,
                    error = e.message
                )}
            }
        }
    }
    
    /**
     * Data class that combines section information with its tracks.
     */
    data class PlaylistSectionData(
        val section: MusicRecommendationEngine.PlaylistSection,
        val tracks: List<Track>
    )
    
    /**
     * UI state for LibraryScreen.
     */
    data class LibraryUiState(
        // Search
        val searchResults: List<Track> = emptyList(),
        val searchLoading: Boolean = false,
        val lastSearchQuery: String = "", // For analytics
        
        // Personalized playlist sections
        val section1: PlaylistSectionData? = null,
        val section2: PlaylistSectionData? = null,
        val section3: PlaylistSectionData? = null,
        val section4: PlaylistSectionData? = null,
        
        // General state
        val playlistsLoading: Boolean = false,
        val selectedTrack: Track? = null,
        val error: String? = null
    ) {
        // Legacy compatibility properties (LibraryScreen)
        @Deprecated("Use section1.tracks instead", ReplaceWith("section1?.tracks ?: emptyList()"))
        val starlightSongs: List<Track> get() = section1?.tracks ?: emptyList()
        
        @Deprecated("Use section2.tracks instead", ReplaceWith("section2?.tracks ?: emptyList()"))
        val djNovaSongs: List<Track> get() = section2?.tracks ?: emptyList()
        
        @Deprecated("Use section3.tracks instead", ReplaceWith("section3?.tracks ?: emptyList()"))
        val eternalHitsSongs: List<Track> get() = section3?.tracks ?: emptyList()
        
        @Deprecated("Use section4.tracks instead", ReplaceWith("section4?.tracks ?: emptyList()"))
        val orbitCrewSongs: List<Track> get() = section4?.tracks ?: emptyList()
    }
}

