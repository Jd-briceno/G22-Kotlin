package com.g22.orbitsoundkotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.g22.orbitsoundkotlin.models.UserProfile

@Composable
fun BackstageCard(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(320.dp)
            .height(380.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Fondo degradado iridiscente como en la primera imagen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // Verde
                            Color(0xFF9C27B0), // Púrpura
                            Color(0xFFE91E63), // Rosa
                            Color(0xFF2196F3)  // Azul
                        ),
                        radius = 400f
                    )
                )
        )

        // Botón de editar en la esquina superior derecha
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            IconButton(
                onClick = { /* Edit profile */ },
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .border(
                        2.5.dp,
                        Color.White.copy(alpha = 0.25f),
                        CircleShape
                    )
            ) {
                AsyncImage(
                    model = profile.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nombre de usuario
            Text(
                text = profile.username,
                color = Color(0xFF2E7D32), // Verde oscuro como en la primera imagen
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badge con título
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFF010B19),
                        RoundedCornerShape(50.dp)
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = profile.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            // Descripción
            Text(
                text = profile.description,
                color = Color(0xFF4CAF50), // Verde claro como en la primera imagen
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // QR Code con icono central
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Color.White,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // Icono central del QR (simulando el ninja mask)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                Color(0xFF2196F3),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VR",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
