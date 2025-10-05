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
import kotlinx.coroutines.*

@Composable
fun LibraryScreen(
    onNavigateToProfile: () -> Unit = {}
) {
    val spotifyService = remember { SpotifyService() }
    var songs by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // 🎨 Playlists simuladas (igual que en Flutter)
    val djRecommendations = listOf(
        Playlist("Electronic Sunset", "https://i.scdn.co/image/ab67616d0000b273e59f65e3c9131d123456aaaa"),
        Playlist("Deep House Mix", "https://i.scdn.co/image/ab67616d0000b2739f39e8b3dff67aa987654bbb")
    )

    val friendsPlaylists = listOf(
        Playlist("Rock Classics", "https://i.scdn.co/image/ab67616d0000b27311223344aabbccddeeff0011"),
        Playlist("Indie Dreams", "https://i.scdn.co/image/ab67616d0000b27399887766aabbccddeeff2233")
    )

    val myPlaylists = listOf(
        Playlist("Roll a d20", "assets/images/Dungeons.jpg"),
        Playlist("Workout", "https://i.scdn.co/image/ab67616d0000b273f62a7e2c97d11af987654321"),
        Playlist("Jazz Nights", "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef123")
    )

    val recommendedPlaylists = listOf(
        Playlist("Lofi", "assets/images/Kamui.jpg"),
        Playlist("Workout", "https://i.scdn.co/image/ab67616d0000b273f62a7e2c97d11af987654321"),
        Playlist("Jazz Nights", "https://i.scdn.co/image/ab67616d0000b27333a4c2bd3a4a5edcabcdef123")
    )

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
            OrbitNavbar(
                username = "Jay Walker",
                title = "Ninja",
                subtitle = "Star Archive",
                profilePainter = null
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    color = Color(0xFFE9E8EE),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = { /* Add playlist */ }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color(0xFFE9E8EE)
                        )
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = Color(0xFFE9E8EE)
                        )
                    }
                }
            }
        }

        // Playlist Sections (con títulos de la primera imagen)
        item {
            PlaylistSection(
                title = "✨ Starlight Suggestions",
                playlists = listOf(
                    Playlist("Hunting soul", "https://example.com/hunting-soul.jpg"),
                    Playlist("Your idol", "https://example.com/your-idol.jpg"),
                    Playlist("7 minutes in hell", "https://example.com/7-minutes.jpg"),
                    Playlist("Roll a d20", "assets/images/Dungeons.jpg")
                )
            )
        }

        item {
            PlaylistSection(
                title = "🎧 DJ Nova's Set",
                playlists = listOf(
                    Playlist("Knife to the Throat", "https://example.com/knife-throat.jpg"),
                    Playlist("Takedown", "https://example.com/takedown.jpg"),
                    Playlist("JOLT", "https://example.com/jolt.jpg"),
                    Playlist("Lofi", "assets/images/Kamui.jpg")
                )
            )
        }

        item {
            PlaylistSection(
                title = "💖 Eternal Hits",
                playlists = listOf(
                    Playlist("Vengeance", "https://example.com/vengeance.jpg"),
                    Playlist("Dark will fall", "https://example.com/dark-will-fall.jpg"),
                    Playlist("Vortex", "https://example.com/vortex.jpg"),
                    Playlist("Electronic Sunset", "https://i.scdn.co/image/ab67616d0000b273e59f65e3c9131d123456aaaa")
                )
            )
        }

        item {
            PlaylistSection(
                title = "🎧 Orbit Crew Playlist",
                playlists = listOf(
                    Playlist("Floating Points", "https://example.com/floating-points.jpg"),
                    Playlist("Ninja Kahui", "https://example.com/ninja-kahui.jpg"),
                    Playlist("Anime Character", "https://example.com/anime-character.jpg"),
                    Playlist("Rock Classics", "https://i.scdn.co/image/ab67616d0000b27311223344aabbccddeeff0011")
                )
            )
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
    onProfileClick: () -> Unit = {}
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
                onClick = { /* Home action */ },
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

// Composable para Search Bar (diseño de la primera imagen)
@Composable
fun SearchBarComposable(
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Campo de texto (sin el icono superpuesto)
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
                .weight(1f)
                .height(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFB4B1B8),
                unfocusedBorderColor = Color(0xFFB4B1B8),
                focusedTextColor = Color(0xFFE9E8EE),
                unfocusedTextColor = Color(0xFFE9E8EE),
                focusedContainerColor = Color(0xFF010B19),
                unfocusedContainerColor = Color(0xFF010B19)
            ),
            shape = RoundedCornerShape(25.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Icono de búsqueda separado (como en la primera imagen)
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    Color(0xFFB4B1B8),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF010B19),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Composable para Song Result Card
@Composable
fun SongResultCard(song: Track) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vinyl Cover placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Color(0xFF2C2C2C),
                    CircleShape
                )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = song.title,
            color = Color(0xFFE9E8EE),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = song.artist,
            color = Color(0xFFE9E8EE),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

// Composable para Playlist Card (diseño de la primera imagen)
@Composable
fun PlaylistCard(playlist: Playlist) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cover con bordes redondeados más pronunciados
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Color(0xFF2C2C2C),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFB4B1B8).copy(alpha = 0.3f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Placeholder para la imagen del álbum
            Text(
                text = "🎵",
                fontSize = 32.sp,
                color = Color(0xFFB4B1B8)
            )
        }

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
