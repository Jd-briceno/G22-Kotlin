package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.models.Playlist
import com.g22.orbitsoundkotlin.services.SpotifyService
import com.g22.orbitsoundkotlin.ui.components.VinylWithCover
import com.g22.orbitsoundkotlin.ui.screens.OrbitNavbar
import kotlinx.coroutines.*
import kotlinx.coroutines.async

@Composable
fun LibraryScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val spotifyService = remember { SpotifyService() }
    var songs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    var showModal by remember { mutableStateOf(false) }
    
    var starlightSongs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var djNovaSongs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var eternalHitsSongs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var orbitCrewSongs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var playlistsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            playlistsLoading = true
            
            val starlightDeferred = async { spotifyService.searchTracks("lofi music") }
            val djNovaDeferred = async { spotifyService.searchTracks("electronic dance music") }
            val eternalHitsDeferred = async { spotifyService.searchTracks("rock music") }
            val orbitCrewDeferred = async { spotifyService.searchTracks("pop hits") }
            
            starlightSongs = starlightDeferred.await()
            djNovaSongs = djNovaDeferred.await()
            eternalHitsSongs = eternalHitsDeferred.await()
            orbitCrewSongs = orbitCrewDeferred.await()
            
        } catch (e: Exception) {
            starlightSongs = listOf(
                Track("Lofi Study", "Chill Beats", "3:45", 225000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef123"),
                Track("Peaceful Morning", "Ambient Sounds", "4:12", 252000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef124"),
                Track("Coffee Shop Vibes", "Relaxing Music", "3:30", 210000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef125")
            )
            djNovaSongs = listOf(
                Track("Electronic Dreams", "DJ Nova", "4:20", 260000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef126"),
                Track("Dance Floor", "EDM Master", "3:55", 235000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef127"),
                Track("Neon Lights", "Synth Wave", "4:08", 248000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef128")
            )
            eternalHitsSongs = listOf(
                Track("Classic Rock Anthem", "Rock Legends", "5:15", 315000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef129"),
                Track("Timeless Melody", "Eternal Artists", "4:30", 270000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef130")
            )
            orbitCrewSongs = listOf(
                Track("Space Journey", "Orbit Crew", "6:45", 405000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef131"),
                Track("Indie Vibes", "Alternative Sound", "3:42", 222000, "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef132")
            )
        } finally {
            playlistsLoading = false
        }
    }

    fun searchSongs(query: String) {
        if (query.isEmpty()) return

        loading = true
        searchQuery = query

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    spotifyService.searchTracks(query)
                }
                songs = results
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                loading = false
            }
        }
    }

    // UI Principal
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            OrbitNavbar(
                username = "Jay Walker",
                title = "Ninja",
                subtitle = "Star Archive",
                profilePainter = null,
                onNavigateToHome = onNavigateToHome,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            SearchBarComposable(
                onSearch = { query -> searchSongs(query) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(5.dp))
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE9E8EE)
                    )
                }
            }
        }

        if (songs.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.height(180.dp)
                ) {
                    items(songs) { song ->
                        SongResultCard(
                            song = song,
                            onClick = {
                                selectedTrack = song
                                showModal = true
                            }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Library",
                color = Color(0xFFE9E8EE),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
        if (playlistsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE9E8EE)
                    )
                }
            }
        } else {
            item {
                SongSection(
                    title = "✨ Starlight Suggestions",
                    songs = starlightSongs,
                    onSongClick = { song ->
                        selectedTrack = song
                        showModal = true
                    }
                )
            }

            item {
                SongSection(
                    title = "🎧 DJ Nova's Set",
                    songs = djNovaSongs,
                    onSongClick = { song ->
                        selectedTrack = song
                        showModal = true
                    }
                )
            }

            item {
                SongSection(
                    title = "💖 Eternal Hits",
                    songs = eternalHitsSongs,
                    onSongClick = { song ->
                        selectedTrack = song
                        showModal = true
                    }
                )
            }

            item {
                SongSection(
                    title = "🎧 Orbit Crew Playlist",
                    songs = orbitCrewSongs,
                    onSongClick = { song ->
                        selectedTrack = song
                        showModal = true
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showModal && selectedTrack != null) {
        SongDetailModal(
            track = selectedTrack!!,
            onDismiss = { 
                showModal = false
                selectedTrack = null
            }
        )
    }
}

@Composable
fun SongDetailModal(
    track: Track,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(
                    Color(0xFF010B19),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFB4B1B8),
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
                .clickable { /* Prevent dismiss when clicking modal content */ }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                VinylWithCover(
                    albumArt = track.albumArt,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = track.title,
                    color = Color(0xFFE9E8EE),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.artist,
                    color = Color(0xFFE9E8EE),
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "KPop Demon Hunters (Soundtrack from Netflix Film)",
                    color = Color(0xFFB4B1B8),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.duration,
                    color = Color(0xFFB4B1B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { /* Play action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Add to playlist action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Playlist",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Like action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Share action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavbarComposable(
    username: String,
    title: String,
    subtitle: String,
    onProfileClick: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color(0xFF010B19))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ninja",
                color = Color(0xFFE9E8EE),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Row {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFB4B1B8), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFB4B1B8), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "o_x",
                    color = Color(0xFFB4B1B8),
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = "Star Archive",
            color = Color(0xFFE9E8EE),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateToHome,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color(0xFF010B19),
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color(0xFFE9E8EE),
                    modifier = Modifier.size(20.dp)
                )
            }

            Row {
                IconButton(
                    onClick = { /* Notifications */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF010B19),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFFE9E8EE),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF2196F3),
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarComposable(
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(y = 15.dp)
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Color(0xFF010B19),
                    RoundedCornerShape(25.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFB4B1B8),
                    RoundedCornerShape(25.dp)
                )
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = {
                Text(
                    "Find your rhythm...",
                    color = Color(0xFFA1BBD1),
                    fontSize = 15.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            },
            modifier = Modifier
                .offset(y = 15.dp)
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFFE9E8EE),
                unfocusedTextColor = Color(0xFFE9E8EE),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )

        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 0.dp)
                .align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF010B19), CircleShape)
                    .border(1.dp, Color(0xFFE9E8EE), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF010B19),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SongResultCard(
    song: Track,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VinylWithCover(
            albumArt = song.albumArt,
            isSpinning = false,
            modifier = Modifier.clickable { onClick() }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${song.title}\n${song.artist}",
            color = Color(0xFFE9E8EE),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SongSection(
    title: String,
    songs: List<Track>,
    onSongClick: (Track) -> Unit
) {
    Column {
        Text(
            text = title,
            color = Color(0xFFE9E8EE),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.height(180.dp)
        ) {
            items(songs) { song ->
                SongResultCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistCard(playlist: Playlist) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VinylWithCover(
            albumArt = playlist.cover,
            isSpinning = false
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = playlist.title,
            color = Color(0xFFE9E8EE),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
