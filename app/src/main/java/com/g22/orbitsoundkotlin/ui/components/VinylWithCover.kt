package com.g22.orbitsoundkotlin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.g22.orbitsoundkotlin.R


@Composable
fun VinylWithCover(
    albumArt: String,
    vinylArt: String = "",
    isSpinning: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 165.dp, height = 120.dp)
    ) {
        // 🎶 Vinilo detrás, sobresaliendo horizontalmente
        Box(
            modifier = Modifier
                .offset(x = 42.dp)
                .size(120.dp)
                .background(Color(0xFF0D0D0D), CircleShape)
                .border(1.dp, Color(0xFFE9E8EE).copy(alpha = 0.8f), CircleShape)
        ) {
            // Surcos del vinilo
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = this.center // Use the center from the DrawScope
                val radius = size.width / 2

                // --- START: MODIFIED SECTION ---

                // Surcos concéntricos
                val grooveColor = Color(0xFFB4B1B8).copy(alpha = 0.35f)
                val inner = radius * 0.36f
                var rGroove = inner
                while (rGroove <= radius * 0.96f) {
                    drawCircle(
                        color = grooveColor,
                        radius = rGroove,
                        center = center,
                        style = Stroke(width = 0.6f) // Pass Stroke style directly
                    )
                    rGroove += 1.6f
                }

                // Aros más marcados
                val boldGrooveColor = Color(0xFFE9E8EE).copy(alpha = 0.5f)
                var rBold = inner + 8f
                while (rBold <= radius * 0.96f) {
                    drawCircle(
                        color = boldGrooveColor,
                        radius = rBold,
                        center = center,
                        style = Stroke(width = 1.0f) // Pass Stroke style directly
                    )
                    rBold += 8f
                }

                // --- END: MODIFIED SECTION ---
            }

            // Etiqueta del centro
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.Center)
                    .background(Color(0xFF222222), CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
            ) {
                val imageUrl = if (vinylArt.isNotEmpty()) vinylArt else albumArt
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Vinyl Label",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // 📀 Funda con la portada del álbum
        Box(
            modifier = Modifier
                .size(120.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(0.dp),
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .background(Color(0xFF010B19), RoundedCornerShape(0.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(0.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(albumArt)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // 📏 Línea gris clarita superior del mismo ancho que la funda
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 1.5.dp)
                .background(Color.White.copy(alpha = 0.24f))
        )
    }
}
