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
    onNavigateToHome: () -> Unit = {}
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

        // Navbar usando el mismo de HomeScreen
        OrbitNavbar(
            username = "Jay Walker",
            title = "Ninja",
            subtitle = "Profile",
            profilePainter = null,
            onNavigateToHome = onNavigateToHome
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

