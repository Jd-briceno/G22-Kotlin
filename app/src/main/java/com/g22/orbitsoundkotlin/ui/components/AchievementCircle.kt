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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g22.orbitsoundkotlin.models.Achievement

@Composable
fun AchievementCircle(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(
                2.dp,
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4CAF50), // Verde
                        Color(0xFF9C27B0), // Púrpura
                        Color(0xFFE91E63), // Rosa
                        Color(0xFF2196F3)  // Azul
                    ),
                    radius = 30f
                ),
                CircleShape
            )
    ) {
        // Fondo oscuro con símbolo blanco
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color(0xFF2C2C2C)),
            contentAlignment = Alignment.Center
        ) {
            // Símbolos según el tipo de logro
            val symbol = when (achievement.id) {
                "1" -> "💀" // Skull
                "2" -> "🔥" // Flame
                "3" -> "👽" // Alien
                else -> "🏆"
            }
            Text(
                text = symbol,
                fontSize = 20.sp,
                color = Color.White
            )
        }
    }
}
