package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.models.Track

@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit = {}
) {
    val currentTrack = remember {
        Track(
            title = "Vengeance",
            artist = "Coldrain",
            duration = "3:45",
            durationMs = 225000,
            albumArt = "assets/images/Coldrain.jpg"
        )
    }

    var isPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 20.dp),
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

        Spacer(modifier = Modifier.height(0.dp))

        // Backstage Card
        BackstageCard()

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
                .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(10.dp))
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
                    AchievementCircle(image = "assets/images/medal.jpg")
                    AchievementCircle(image = "assets/images/medal2.jpg")
                    AchievementCircle(image = "assets/images/medal3.jpg")
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
                    FriendCircle(image = "assets/images/X.jpg", status = FriendStatus.ONLINE)
                    FriendCircle(image = "assets/images/Lin ling.jpg", status = FriendStatus.AWAY)
                    FriendCircle(image = "assets/images/E-soul.jpg", status = FriendStatus.OFFLINE)
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

@Composable
fun BackstageCard() {
    Box(
        modifier = Modifier
            .width(320.dp)
            .height(380.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF9C27B0),
                        Color(0xFF2196F3),
                        Color(0xFF4CAF50),
                        Color(0xFFFF9800)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(12.dp))
            .padding(16.dp)
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
                    tint = Color(0xFF010B19),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Picture
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(Color(0xFF2196F3), CircleShape)
                    .border(2.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "VR",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Username
            Text(
                text = "Higan",
                color = Color(0xFF010B19),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Title Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFF010B19), RoundedCornerShape(50.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Hero X",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Bio
            Text(
                text = "From calm seas to wild storms — I have a track for it 🌊⚡",
                color = Color(0xFF010B19),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            // QR Code placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF010B19), RoundedCornerShape(12.dp))
                    .padding(8.dp),
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
fun DottedLine() {
    Canvas(
        modifier = Modifier
            .width(320.dp)
            .height(2.dp)
    ) {
        val color = Color(0xFFB4B1B8)
        val strokeWidth = 2f
        val dashWidth = 6f
        val dashSpace = 4f
        var startX = 0f
        
        while (startX < size.width) {
            drawLine(
                color = color,
                start = Offset(startX, 0f),
                end = Offset(startX + dashWidth, 0f),
                strokeWidth = strokeWidth
            )
            startX += dashWidth + dashSpace
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.54f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.54f))
        )
    }
}

@Composable
fun AchievementCircle(image: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.Transparent, CircleShape)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🏆",
            fontSize = 20.sp
        )
    }
}

@Composable
fun FriendCircle(image: String, status: FriendStatus) {
    val borderColor = when (status) {
        FriendStatus.ONLINE -> Color(0xFF4CAF50)
        FriendStatus.AWAY -> Color(0xFFFFC107)
        FriendStatus.OFFLINE -> Color(0xFFF44336)
    }
    
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.Transparent, CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "👤",
            fontSize = 20.sp
        )
    }
}

@Composable
fun PlusIcon() {
    Box(
        modifier = Modifier
            .size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(60.dp)
        )
    }
}

enum class FriendStatus { ONLINE, AWAY, OFFLINE }

@Composable
fun MiniSongReproductor(
    track: Track,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(400.dp)
            .height(62.dp)
            .background(
                Color(0xFF010B19),
                RoundedCornerShape(30.5.dp)
            )
            .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(30.5.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0xFF2C2C2C), CircleShape)
                .border(1.dp, Color(0xFFB4B1B8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎵",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Track Info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color(0xFFE9E8EE),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFFE9E8EE),
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color(0xFFE9E8EE),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(6.dp))
    }
}

