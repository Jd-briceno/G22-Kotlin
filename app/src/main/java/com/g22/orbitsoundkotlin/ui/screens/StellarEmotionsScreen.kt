package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.g22.orbitsoundkotlin.ui.screens.shared.OrbitSoundHeader
import com.g22.orbitsoundkotlin.ui.screens.shared.StarField
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme

@Composable
fun StellarEmotionsScreen(
    username: String
) {
    // Animation for the star field background
    val infiniteTransition = rememberInfiniteTransition(label = "star_field_transition")
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 180_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "global_time_animation"
    )

    val starColors = listOf(
        Color(0xFFE3F2FD), // Light Blue
        Color(0xFFFFFDE7), // Light Yellow
        Color(0xFFF3E5F5)  // Light Purple
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
    ) {
        // Reused StarField background
        StarField(
            modifier = Modifier.fillMaxSize(),
            globalTime = globalTime,
            starColors = starColors
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp) // <-- Add this line
        ) {
            // Reused Header component
            OrbitSoundHeader(
                title = "StellarEmotions",
                username = username,
                subtitle = "How are you feeling?",
            )

            // --- Your screen-specific content goes here ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Content for Stellar Emotions Screen",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun StellarEmotionsScreenPreview() {
    OrbitSoundKotlinTheme {
        StellarEmotionsScreen(username = "User")
    }
}

