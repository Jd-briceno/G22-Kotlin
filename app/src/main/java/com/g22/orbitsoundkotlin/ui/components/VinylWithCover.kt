package com.g22.orbitsoundkotlin.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.g22.orbitsoundkotlin.R

/**
 * VinylWithCover - Componente que muestra una funda de vinilo con el vinilo sobresaliendo
 * Basado en el componente Flutter del mismo nombre
 */
@Composable
fun VinylWithCover(
    albumArt: String,
    vinylArt: String = "",
    isSpinning: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Imagen cuadrada simple
    Box(
        modifier = modifier
            .size(120.dp)
            .background(Color(0xFF2C2C2C))
            .border(1.dp, Color(0xFFB4B1B8), RoundedCornerShape(8.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(albumArt)
                .crossfade(true)
                .build(),
            contentDescription = "Album Cover",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            error = {
                // Placeholder si no se encuentra la imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎵",
                        fontSize = 24.sp,
                        color = Color(0xFFB4B1B8)
                    )
                }
            }
        )
    }
}