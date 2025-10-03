package com.g22.orbitsoundkotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g22.orbitsoundkotlin.models.Friend
import com.g22.orbitsoundkotlin.models.FriendStatus

@Composable
fun FriendCircle(
    friend: Friend,
    modifier: Modifier = Modifier
) {
    // Colores vibrantes como en la primera imagen
    val borderColors = when (friend.status) {
        FriendStatus.ONLINE -> listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)) // Verde
        FriendStatus.AWAY -> listOf(Color(0xFFFFC107), Color(0xFFFF9800)) // Amarillo
        FriendStatus.OFFLINE -> listOf(Color(0xFFF44336), Color(0xFFD32F2F)) // Rojo
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(
                2.dp,
                Brush.radialGradient(
                    colors = borderColors,
                    radius = 30f
                ),
                CircleShape
            )
    ) {
        // Fondo con avatar estilizado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            // Avatares estilizados según el amigo
            val avatarSymbol = when (friend.id) {
                "1" -> "🟢" // Green Ninja
                "2" -> "❄️" // Snow Warrior
                "3" -> "🌙" // Dark Knight
                else -> "👤"
            }
            Text(
                text = avatarSymbol,
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}
