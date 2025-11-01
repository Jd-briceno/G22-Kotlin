package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.data.FirestoreEmotionRepository
import com.g22.orbitsoundkotlin.data.MusicRecommendationEngine
import com.g22.orbitsoundkotlin.data.repositories.LibraryCacheRepository
import androidx.compose.ui.graphics.Color
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
    private val spotifyService: SpotifyService = SpotifyService.Companion.getInstance(),
    private val libraryCacheRepo: LibraryCacheRepository? = null,
    private val userId: String = ""
) : ViewModel() {
    
    companion object {
        private const val TAG = "LibraryViewModel"
    }

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "Initializing LibraryViewModel")
        MusicAnalytics.trackLibraryScreenView()
        loadPersonalizedPlaylists()
    }
    
    /**
     * Updates playlist recommendations based on user's constellations and emotions.
     * 
     * This method can be called when the user updates their preferences or when
     * fresh recommendations are needed based on new data.
     *
     * @param userConstellations List of user's selected constellations for personalization
     * @param recentEmotions List of user's recent emotions for mood-based recommendations
     */
    fun refreshRecommendations(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        loadPersonalizedPlaylists(userConstellations, recentEmotions)
    }
    
    /**
     * Loads user emotions from Firestore and refreshes playlist recommendations.
     * 
     * This method performs a sequential operation where user's recent emotions
     * are first fetched from Firestore, then used to generate personalized
     * playlist recommendations from Spotify. All I/O operations are executed
     * on the IO dispatcher to avoid blocking the main thread.
     *
     * @param userId The unique identifier of the user whose data should be loaded
     */
    fun loadUserEmotionsAndRefresh(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting emotion-based playlist refresh")
            
            try {
                val emotionsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Fetching emotions on: ${Thread.currentThread().name}")
                    getRecentEmotionsFromFirestore(userId, limit = 5)
                }
                
                val emotions = emotionsDeferred.await()
                Log.d(TAG, "Emotions loaded: ${emotions.size} emotions")
                
                val playlistsDeferred = async(Dispatchers.IO) {
                    Log.d(TAG, "Loading playlists on: ${Thread.currentThread().name}")
                    loadPersonalizedPlaylistsWithEmotions(emotions)
                }
                
                playlistsDeferred.await()
                Log.d(TAG, "All nested operations completed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in nested operations", e)
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
    
    /**
     * Fetches recent emotions from Firestore and converts them to EmotionModel list.
     *
     * @param userId The user identifier for fetching emotion logs
     * @param limit Maximum number of emotions to retrieve
     * @return List of EmotionModel objects representing user's recent emotions
     */
    private suspend fun getRecentEmotionsFromFirestore(userId: String, limit: Int): List<EmotionModel> {
        return withContext(Dispatchers.IO) {
            try {
                val emotionRepo = FirestoreEmotionRepository()
                val logs = emotionRepo.getEmotionLogs(userId).first()
                
                val allEmotions = logs.flatMap { log -> 
                    log.emotions.map { entry -> 
                        EmotionModel(
                            id = entry.id,
                            name = entry.name,
                            description = "",
                            color = Color.Gray,
                            iconRes = 0,
                            source = entry.source
                        )
                    }
                }
                
                allEmotions.take(limit)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching emotions from Firestore", e)
                emptyList()
            }
        }
    }
    
    /**
     * Wrapper method to load personalized playlists using only emotion data.
     *
     * @param emotions List of emotions to use for generating recommendations
     */
    private suspend fun loadPersonalizedPlaylistsWithEmotions(emotions: List<EmotionModel>) {
        loadPersonalizedPlaylists(
            userConstellations = emptyList(),
            recentEmotions = emotions
        )
    }

    /**
     * Searches for music tracks on Spotify based on user query.
     * 
     * This method performs the search operation on an IO dispatcher for network calls,
     * then switches to the Main dispatcher to update the UI state. This ensures
     * that network operations don't block the main thread while UI updates
     * happen on the appropriate thread.
     *
     * @param query The search query string entered by the user
     */
    fun searchTracks(query: String) {
        if (query.isEmpty()) return
        
        Log.d(TAG, "Starting search with dispatcher switch: $query")
        viewModelScope.launch {
            _uiState.update { it.copy(searchLoading = true) }
            
            // Save to search history
            if (libraryCacheRepo != null && userId.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        libraryCacheRepo.saveSearch(userId, query)
                        Log.d(TAG, "Search saved to history: $query")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving search history", e)
                    }
                }
            }
            
            try {
                val tracks = withContext(Dispatchers.IO) {
                    Log.d(TAG, "Searching on: ${Thread.currentThread().name}")
                    spotifyService.searchTracks(query)
                }
                
                Log.d(TAG, "Fetched ${tracks.size} tracks")
                
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "Updating UI on: ${Thread.currentThread().name}")
                    
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
                Log.e(TAG, "Error in search", e)
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
     * Selects a track from a recommendation section and tracks the interaction.
     * 
     * Records analytics data about which section the track was selected from,
     * including section position, title, and type for recommendation analysis.
     *
     * @param track The track that was selected
     * @param sectionPosition The position of the section (1-4) where the track was located
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
            val sectionType = determineSectionType(data.section.title)
            
            MusicAnalytics.trackSectionClick(
                sectionTitle = data.section.title,
                sectionType = sectionType,
                trackTitle = track.title,
                trackArtist = track.artist,
                sectionPosition = sectionPosition
            )
            
            MusicAnalytics.trackTrackDetailView(track.title, track.artist)
        }
        
        _uiState.update { it.copy(selectedTrack = track) }
    }
    
    /**
     * Selects a track from search results and records the interaction.
     * 
     * Tracks analytics data about the search result selection, including
     * the query that led to the result and the position in the results list.
     *
     * @param track The track that was selected from search results
     * @param position The position of the track in the search results list
     * @param query The search query that produced these results
     */
    fun selectTrackFromSearch(track: Track, position: Int, query: String) {
        MusicAnalytics.trackSearchResultClick(
            query = query,
            trackTitle = track.title,
            trackArtist = track.artist,
            resultPosition = position
        )
        
        MusicAnalytics.trackTrackDetailView(track.title, track.artist)
        
        _uiState.update { it.copy(selectedTrack = track) }
    }
    
    /**
     * Determines the type category of a playlist section based on its title.
     * 
     * Categories include: time-based, constellation-based, emotion-based, or default.
     *
     * @param title The title of the playlist section
     * @return The section type as a string identifier
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
            
            title.contains("Cisne", ignoreCase = true) ||
            title.contains("Pegasus", ignoreCase = true) ||
            title.contains("Draco", ignoreCase = true) ||
            title.contains("Ursa", ignoreCase = true) ||
            title.contains("Cross", ignoreCase = true) ||
            title.contains("Phoenix", ignoreCase = true) -> "constellation"
            
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
    
    /**
     * Dismisses the currently displayed track detail view.
     * 
     * Clears the selected track from the UI state, closing any detail
     * view that may be showing.
     */
    fun dismissTrackDetail() {
        _uiState.update { it.copy(selectedTrack = null) }
    }
    
    /**
     * Loads sections from local cache (instant load).
     * Part of SWR (Stale-While-Revalidate) pattern - shows cached data immediately
     * while fresh data loads in background.
     */
    private suspend fun loadFromCache() {
        if (libraryCacheRepo == null || userId.isEmpty()) return
        
        try {
            val cachedSections = libraryCacheRepo.getAllSections(userId)
            
            if (cachedSections.isNotEmpty()) {
                Log.d(TAG, "Loading ${cachedSections.size} sections from cache")
                
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        var updatedState = state
                        
                        cachedSections[1]?.let { (title, tracks) ->
                            val section = MusicRecommendationEngine.PlaylistSection(title, "", "", "🎵")
                            updatedState = updatedState.copy(
                                section1 = PlaylistSectionData(section, tracks)
                            )
                        }
                        
                        cachedSections[2]?.let { (title, tracks) ->
                            val section = MusicRecommendationEngine.PlaylistSection(title, "", "", "🎵")
                            updatedState = updatedState.copy(
                                section2 = PlaylistSectionData(section, tracks)
                            )
                        }
                        
                        cachedSections[3]?.let { (title, tracks) ->
                            val section = MusicRecommendationEngine.PlaylistSection(title, "", "", "🎵")
                            updatedState = updatedState.copy(
                                section3 = PlaylistSectionData(section, tracks)
                            )
                        }
                        
                        cachedSections[4]?.let { (title, tracks) ->
                            val section = MusicRecommendationEngine.PlaylistSection(title, "", "", "🎵")
                            updatedState = updatedState.copy(
                                section4 = PlaylistSectionData(section, tracks),
                                playlistsLoading = false // Cache loaded, remove spinner
                            )
                        }
                        
                        updatedState
                    }
                }
                
                Log.d(TAG, "Cache loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading cache", e)
            // Don't fail - let Spotify refresh handle it
        }
    }
    
    /**
     * Saves the 4 playlist sections to local cache for next load.
     * Enables instant loading on next app launch.
     */
    private suspend fun cacheSections(
        sections: List<MusicRecommendationEngine.PlaylistSection>,
        section1Tracks: List<Track>,
        section2Tracks: List<Track>,
        section3Tracks: List<Track>,
        section4Tracks: List<Track>
    ) {
        if (libraryCacheRepo == null || userId.isEmpty()) return
        
        try {
            withContext(Dispatchers.IO) {
                val sectionsData = mapOf(
                    1 to Pair(sections[0].title, section1Tracks),
                    2 to Pair(sections[1].title, section2Tracks),
                    3 to Pair(sections[2].title, section3Tracks),
                    4 to Pair(sections[3].title, section4Tracks)
                )
                
                libraryCacheRepo.cacheAllSections(userId, sectionsData)
                Log.d(TAG, "Cached all 4 sections successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching sections", e)
            // Non-critical - app continues working
        }
    }
    
    /**
     * Loads personalized playlists using the recommendation engine.
     * 
     * Generates four playlist sections based on user preferences (constellations
     * and recent emotions) and fetches tracks for each section from Spotify.
     * All Spotify API calls are executed in parallel on the IO dispatcher
     * for optimal performance.
     *
     * @param userConstellations List of user's selected constellations
     * @param recentEmotions List of user's recent emotions
     */
    private fun loadPersonalizedPlaylists(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ) {
        Log.d(TAG, "Loading personalized playlists...")
        Log.d(TAG, "Constellations: ${userConstellations.size}, Emotions: ${recentEmotions.size}")
        
        viewModelScope.launch {
            _uiState.update { it.copy(playlistsLoading = true) }
            
            // SWR PATTERN: Load from cache first (instant load)
            loadFromCache()
            
            try {
                val sections = MusicRecommendationEngine.generatePlaylistSections(
                    userConstellations = userConstellations,
                    recentEmotions = recentEmotions
                )
                
                Log.d(TAG, "Sections generated: ${sections.joinToString { it.title }}")
                Log.d(TAG, "Starting parallel searches")
                
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
                
                // SWR PATTERN: Cache fresh data in background (for next load)
                cacheSections(sections, section1Songs, section2Songs, section3Songs, section4Songs)
                
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
     * Data class that combines playlist section metadata with its associated tracks.
     *
     * @property section The playlist section information (title, query, etc.)
     * @property tracks List of tracks belonging to this section
     */
    data class PlaylistSectionData(
        val section: MusicRecommendationEngine.PlaylistSection,
        val tracks: List<Track>
    )
    
    /**
     * Represents the complete UI state for the Library screen.
     *
     * @property searchResults List of tracks from the current search query
     * @property searchLoading Indicates if a search operation is in progress
     * @property lastSearchQuery The most recent search query for analytics tracking
     * @property section1 First personalized playlist section with tracks
     * @property section2 Second personalized playlist section with tracks
     * @property section3 Third personalized playlist section with tracks
     * @property section4 Fourth personalized playlist section with tracks
     * @property playlistsLoading Indicates if playlist sections are being loaded
     * @property selectedTrack Currently selected track for detail view
     * @property error Error message if any operation fails
     */
    data class LibraryUiState(
        val searchResults: List<Track> = emptyList(),
        val searchLoading: Boolean = false,
        val lastSearchQuery: String = "",
        val section1: PlaylistSectionData? = null,
        val section2: PlaylistSectionData? = null,
        val section3: PlaylistSectionData? = null,
        val section4: PlaylistSectionData? = null,
        val playlistsLoading: Boolean = false,
        val selectedTrack: Track? = null,
        val error: String? = null
    ) {
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

