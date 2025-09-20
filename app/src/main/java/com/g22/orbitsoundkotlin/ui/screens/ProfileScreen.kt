package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.models.*
import com.g22.orbitsoundkotlin.ui.components.*

@Composable
fun ProfileScreen(
    onNavigateToLibrary: () -> Unit = {}
) {
    val userProfile = remember {
        UserProfile(
            id = "1",
            username = "Higan",
            title = "Ninja",
            description = "From calm seas to wild storms — I have a track for it 🌊⚡",
            avatarUrl = "assets/images/Jay.jpg",
            isPremium = false,
            qrData = "https://tuapp.com/user/higan",
            achievements = listOf(
                Achievement("1", "Skull Master", "https://example.com/skull.jpg", "Complete your first playlist"),
                Achievement("2", "Flame Warrior", "https://example.com/flame.jpg", "Listen to 100 songs"),
                Achievement("3", "Alien Hunter", "https://example.com/alien.jpg", "Share 10 playlists")
            ),
            friends = listOf(
                Friend("1", "Green Ninja", "https://example.com/green-ninja.jpg", FriendStatus.ONLINE),
                Friend("2", "Snow Warrior", "https://example.com/snow-warrior.jpg", FriendStatus.AWAY),
                Friend("3", "Dark Knight", "https://example.com/dark-knight.jpg", FriendStatus.OFFLINE)
            )
        )
    }

    val currentTrack = remember {
        Track(
            title = "JEOPARDY",
            artist = "Sawano Hiroyuki",
            duration = "3:45",
            durationMs = 225000,
            albumArt = "https://example.com/anime-album.jpg"
        )
    }

    var isPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Navbar con icono de configuración
        NavbarComposable(
            username = "Jay Walker",
            title = "Lightning Ninja",
            subtitle = null,
            showSettingsIcon = true,
            onHomeClick = onNavigateToLibrary
        )

        Spacer(modifier = Modifier.height(0.dp))

        // Backstage Card
        BackstageCard(profile = userProfile)

        // Línea punteada
        DottedLine()

        // Contenedor principal con secciones
        Box(
            modifier = Modifier
                .width(320.dp)
                .height(390.dp)
                .background(
                    Color(0xFF010B19),
                    RoundedCornerShape(10.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                // Sección Achievements
                SectionTitle(title = "Achievements")
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    userProfile.achievements.forEach { achievement ->
                        AchievementCircle(achievement = achievement)
                    }
                    PlusIcon()
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sección Friends
                SectionTitle(title = "Friends")
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    userProfile.friends.forEach { friend ->
                        FriendCircle(friend = friend)
                    }
                    PlusIcon()
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Sección Now Listening
                SectionTitle(title = "Now Listening")
                Spacer(modifier = Modifier.height(14.dp))

                MiniSongReproductor(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    onPlayPause = { isPlaying = !isPlaying },
                    onNext = { /* Lógica para siguiente canción */ },
                    onPrevious = { /* Lógica para canción anterior */ }
                )
            }
        }
    }
}

// Navbar específico para Profile con icono de configuración
@Composable
fun NavbarComposable(
    username: String,
    title: String,
    subtitle: String?,
    showSettingsIcon: Boolean = false,
    onHomeClick: () -> Unit = {}
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
            // Campo "Ninja" como en la imagen
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(24.dp)
                    .background(
                        Color(0xFF2C2C2C),
                        RoundedCornerShape(4.dp)
                    )
                    .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ninja",
                    color = Color(0xFFB4B1B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

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

        // Barra principal con navegación
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Home + "Profile"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón Home
                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF2C2C2C),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = Color(0xFFE9E8EE),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Texto "Profile"
                Text(
                    text = "Profile",
                    color = Color(0xFFE9E8EE),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Botones de la derecha
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón Notificaciones
                IconButton(
                    onClick = { /* Notifications */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF2C2C2C),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFFE9E8EE),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Botón Configuración
                IconButton(
                    onClick = { /* Settings */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF2C2C2C),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFE9E8EE),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
