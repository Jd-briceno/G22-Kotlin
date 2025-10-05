package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.models.Track

@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit = {}
) {
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
            .background(Color(0xFF010B19))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Navbar
        OrbitNavbar(
            username = "Jay Walker",
            title = "Ninja",
            subtitle = "Profile",
            profilePainter = null,
            onNavigateToHome = onNavigateToHome
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Profile Card
        ProfileCard()

        Spacer(modifier = Modifier.height(20.dp))

        // Achievements Section
        SectionWithTitle("Achievements") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementBadge(icon = "💀", color = Color(0xFF9C27B0))
                AchievementBadge(icon = "🔥", color = Color(0xFFFF5722))
                AchievementBadge(icon = "👽", color = Color(0xFF4CAF50))
                PlusBadge()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Friends Section
        SectionWithTitle("Friends") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FriendAvatar(name = "Green Ninja", color = Color(0xFF4CAF50))
                FriendAvatar(name = "Snow Warrior", color = Color(0xFFF44336))
                FriendAvatar(name = "Dark Knight", color = Color(0xFF2196F3))
                PlusBadge()
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Now Listening Section
        SectionWithTitle("Now listening") {
            MusicPlayerCard(track = currentTrack, isPlaying = isPlaying, onPlayPause = { isPlaying = !isPlaying })
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ProfileCard() {
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(280.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9C27B0),
                        Color(0xFF2196F3),
                        Color(0xFF4CAF50),
                        Color(0xFFFF9800)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Edit icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profile Picture
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF2196F3), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VR",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                text = "Higan",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF010B19), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Ninja",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bio
            Text(
                text = "From calm seas to wild storms — I have a track for it 🌊⚡",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code placeholder
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "QR",
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionWithTitle(title: String, content: @Composable () -> Unit) {
    Column {
        // Title with lines
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(1.dp)
                    .background(Color(0xFFB4B1B8))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = Color(0xFFE9E8EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(1.dp)
                    .background(Color(0xFFB4B1B8))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        content()
    }
}

@Composable
fun AchievementBadge(icon: String, color: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(color, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
    }
}

@Composable
fun FriendAvatar(name: String, color: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(color, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PlusBadge() {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(Color.Transparent, CircleShape)
            .border(2.dp, Color(0xFFB4B1B8), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            tint = Color(0xFFB4B1B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MusicPlayerCard(track: Track, isPlaying: Boolean, onPlayPause: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3))
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2C2C2C), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎵",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { /* Previous */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = { /* Next */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

