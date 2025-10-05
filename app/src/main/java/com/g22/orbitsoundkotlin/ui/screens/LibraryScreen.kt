package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
    
    // 🎵 Playlists reales de Spotify
    var starlightPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var djNovaPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var eternalHitsPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var orbitCrewPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var playlistsLoading by remember { mutableStateOf(true) }

    // Cargar playlists al iniciar
    LaunchedEffect(Unit) {
        try {
            playlistsLoading = true
            
            // Cargar playlists en paralelo
            val starlightDeferred = async { spotifyService.getGenrePlaylists("lofi") }
            val djNovaDeferred = async { spotifyService.getFeaturedPlaylists() }
            val eternalHitsDeferred = async { spotifyService.getGenrePlaylists("rock") }
            val orbitCrewDeferred = async { spotifyService.getCategoryPlaylists("pop") }
            
            starlightPlaylists = starlightDeferred.await()
            djNovaPlaylists = djNovaDeferred.await()
            eternalHitsPlaylists = eternalHitsDeferred.await()
            orbitCrewPlaylists = orbitCrewDeferred.await()
            
            println("📊 Playlists loaded:")
            println("  - Starlight: ${starlightPlaylists.size}")
            println("  - DJ Nova: ${djNovaPlaylists.size}")
            println("  - Eternal Hits: ${eternalHitsPlaylists.size}")
            println("  - Orbit Crew: ${orbitCrewPlaylists.size}")
            
        } catch (e: Exception) {
            println("Error cargando playlists: ${e.message}")
            // Fallback a playlists locales si falla la API
            starlightPlaylists = listOf(
                Playlist("Roll a d20", "assets/images/Dungeons.jpg"),
                Playlist("Good Vibes", "assets/images/Good.jpg"),
                Playlist("Jazz Nights", "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef123")
            )
            djNovaPlaylists = listOf(
                Playlist("Lofi", "assets/images/Lofi.jpg"),
                Playlist("Study", "assets/images/Study.jpg"),
                Playlist("Jazz Nights", "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef123")
            )
            eternalHitsPlaylists = listOf(
                Playlist("Hunting soul", "assets/images/Hunting.jpg"),
                Playlist("Ruined King", "assets/images/Ruined.jpg")
            )
            orbitCrewPlaylists = listOf(
                Playlist("I Believe", "assets/images/UFO.jpg"),
                Playlist("Indie Dreams", "assets/images/Indie.jpg")
            )
        } finally {
            playlistsLoading = false
        }
    }

    // Función para buscar canciones
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
                // Manejar error
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

        // Navbar (usando el mismo de HomeScreen)
        item {
            NavbarComposable(
                username = "Jay Walker",
                title = "Ninja",
                subtitle = "Star Archive",
                onProfileClick = onNavigateToProfile,
                onNavigateToHome = onNavigateToHome
            )
        }

        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Search Bar
        item {
            SearchBarComposable(
                onSearch = { query -> searchSongs(query) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(5.dp))
        }

        // Loading indicator
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

        // Songs results
        if (songs.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.height(180.dp)
                ) {
                    items(songs) { song ->
                        SongResultCard(song = song)
                    }
                }
            }
        }

        // Library Header
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

        // Playlist Sections con datos reales de Spotify
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
                PlaylistSection(
                    title = "✨ Starlight Suggestions",
                    playlists = starlightPlaylists
                )
            }

            item {
                PlaylistSection(
                    title = "🎧 DJ Nova's Set",
                    playlists = djNovaPlaylists
                )
            }

            item {
                PlaylistSection(
                    title = "💖 Eternal Hits",
                    playlists = eternalHitsPlaylists
                )
            }

            item {
                PlaylistSection(
                    title = "🎧 Orbit Crew Playlist",
                    playlists = orbitCrewPlaylists
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}

// Composable para Navbar (diseño de la primera imagen)
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
        // Barra de título superior con "Ninja"
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
                // Círculos decorativos
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

        // Título principal "Star Archive"
        Text(
            text = "Star Archive",
            color = Color(0xFFE9E8EE),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 20.dp)
        )

        // Botones de navegación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Home
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

            // Botones de la derecha
            Row {
                // Botón Notificaciones
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

                // Avatar del perfil
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

// Composable para Search Bar (igual que en Flutter)
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
        // Fondo de la barra de búsqueda (rectángulo redondeado)
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

        // Campo de texto
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

        // Círculos concéntricos (como en Flutter)
        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 0.dp) // Ajustado para alinear con el input
                .align(Alignment.CenterEnd)
        ) {
            // Círculo exterior
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF010B19), CircleShape)
                    .border(1.dp, Color(0xFFE9E8EE), CircleShape)
            )

            // Círculo medio
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            // Círculo interior
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            // Círculo central
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            // Icono de lupa
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

// Composable para Song Result Card (igual que en Flutter)
@Composable
fun SongResultCard(song: Track) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VinylWithCover(
            albumArt = song.albumArt,
            isSpinning = false
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

// Composable para Playlist Section
@Composable
fun PlaylistSection(
    title: String,
    playlists: List<Playlist>
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
            items(playlists) { playlist ->
                PlaylistCard(playlist = playlist)
            }
        }
    }
}

// Composable para Playlist Card (igual que en Flutter)
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
