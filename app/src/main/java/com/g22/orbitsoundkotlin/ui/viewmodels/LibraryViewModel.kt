package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.MusicRecommendationEngine
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.services.SpotifyService
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class LibraryViewModel(
    private val spotifyService: SpotifyService = SpotifyService.Companion.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        Log.d("LibraryViewModel", "Initializing LibraryViewModel")
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

    fun searchTracks(query: String) {
        if (query.isEmpty()) return
        
        Log.d("LibraryViewModel", "Searching tracks: $query")
        viewModelScope.launch {
            _uiState.update { it.copy(searchLoading = true) }
            try {
                val tracks = spotifyService.searchTracks(query)
                Log.d("LibraryViewModel", "Tracks found: ${tracks.size}")
                
                // 📊 Analytics: Track search
                MusicAnalytics.trackSearch(query, tracks.size)
                
                _uiState.update { it.copy(
                    searchResults = tracks,
                    searchLoading = false,
                    lastSearchQuery = query
                )}
            } catch (e: Exception) {
                Log.e("LibraryViewModel", "Error searching tracks", e)
                _uiState.update { it.copy(
                    searchLoading = false,
                    error = e.message
                )}
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
     */
    private fun loadPersonalizedPlaylists(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        Log.d("LibraryViewModel", "Loading personalized playlists...")
        Log.d("LibraryViewModel", "Constellations: ${userConstellations.size}, Emotions: ${recentEmotions.size}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(playlistsLoading = true) }
            try {
                // Generate personalized sections
                val sections = MusicRecommendationEngine.generatePlaylistSections(
                    userConstellations = userConstellations,
                    recentEmotions = recentEmotions
                )
                
                Log.d("LibraryViewModel", "Sections generated: ${sections.joinToString { it.title }}")
                Log.d("LibraryViewModel", "Starting parallel searches")
                
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
                
                // Load tracks for each section in parallel
                val section1Deferred = async { 
                    spotifyService.searchTracks(sections[0].query)
                }
                val section2Deferred = async { 
                    spotifyService.searchTracks(sections[1].query)
                }
                val section3Deferred = async { 
                    spotifyService.searchTracks(sections[2].query)
                }
                val section4Deferred = async { 
                    spotifyService.searchTracks(sections[3].query)
                }
                
                val section1Songs = section1Deferred.await()
                val section2Songs = section2Deferred.await()
                val section3Songs = section3Deferred.await()
                val section4Songs = section4Deferred.await()
                
                Log.d("LibraryViewModel", "Playlists loaded: ${section1Songs.size}, ${section2Songs.size}, ${section3Songs.size}, ${section4Songs.size}")
                
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
                Log.e("LibraryViewModel", "Error loading personalized playlists", e)
                _uiState.update { it.copy(
                    playlistsLoading = false,
                    error = e.message
                )}
            }
        }
    }
<<<<<<< HEAD:app/src/main/java/com/g22/orbitsoundkotlin/ui/screens/library/LibraryViewModel.kt
    
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
=======

    private fun getFallbackTracks() = listOf(
        Track("Lofi Study", "Chill Beats", "3:45", 225000, ""),
        Track("Peaceful Morning", "Ambient Sounds", "4:12", 252000, ""),
        Track("Coffee Shop Vibes", "Relaxing Music", "3:30", 210000, "")
    )

>>>>>>> origin/main:app/src/main/java/com/g22/orbitsoundkotlin/ui/viewmodels/LibraryViewModel.kt
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
<<<<<<< HEAD:app/src/main/java/com/g22/orbitsoundkotlin/ui/screens/library/LibraryViewModel.kt
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

=======
    )
}
>>>>>>> origin/main:app/src/main/java/com/g22/orbitsoundkotlin/ui/viewmodels/LibraryViewModel.kt
