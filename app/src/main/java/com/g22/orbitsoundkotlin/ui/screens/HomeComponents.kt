package com.g22.orbitsoundkotlin.ui.screens.shared

import android.content.Context
import android.location.Location
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.random.Random

// NOTE: All these functions were moved from HomeScreen.kt and made `internal` to be shared within the module.

@Composable
internal fun OrbitSoundHeader(
    title: String,
    username: String,
    subtitle: String? = null,
    profilePainter: Painter? = null
) {
    val dark = Color(0xFF010B19)
    val borderColor = Color(0xFFB4B1B8)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val outerShape = RoundedCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
        Box(
            modifier = Modifier
                .offset(x = 28.dp, y = 14.dp)
                .size(width = 325.dp, height = 86.dp)
                .clip(outerShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), outerShape)
        )

        val centerShape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp, topEnd = 5.dp)
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 36.dp)
                .size(width = 225.dp, height = 57.dp)
                .clip(centerShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), centerShape)
                .padding(start = 32.dp, end = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = subtitle ?: "Hello, $username",
                color = Color(0xFFE9E8EE),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }

        val topShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
        Box(
            modifier = Modifier
                .offset(x = 21.dp, y = 1.dp)
                .size(width = 300.dp, height = 30.dp)
                .clip(topShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), topShape)
        )

        val titleShape = RoundedCornerShape(10.dp)
        Box(
            modifier = Modifier
                .offset(x = 38.dp, y = 4.5.dp)
                .size(width = 123.dp, height = 22.dp)
                .clip(titleShape)
                .border(BorderStroke(2.dp, borderColor), titleShape)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                color = Color(0xFFE9E8EE),
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        }

        val buttonShape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .offset(x = 1.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = "Home",
                tint = Color(0xFFE9E8EE),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 252.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFFE9E8EE)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 300.dp, y = 42.dp)
                .size(45.dp)
                .clip(buttonShape)
                .border(BorderStroke(2.dp, borderColor), buttonShape),
            contentAlignment = Alignment.Center
        ) {
            if (profilePainter != null) {
                Image(
                    painter = profilePainter,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile placeholder",
                    tint = Color(0xFFE9E8EE)
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = 167.dp, y = 4.5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Headset,
                contentDescription = "Headphones",
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 192.dp, y = 4.5.dp)
                .size(24.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = "Music",
                tint = borderColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 245.dp, y = 7.dp)
                .size(17.dp)
                .clip(CircleShape)
                .border(BorderStroke(2.dp, borderColor), CircleShape)
        )

        Box(
            modifier = Modifier
                .offset(x = 265.dp, y = 18.dp)
                .size(width = 14.dp, height = 2.dp)
                .background(borderColor)
        )

        Canvas(
            modifier = Modifier
                .offset(x = 280.dp, y = 10.dp)
                .size(14.dp)
        ) {
            val paintColor = borderColor
            drawLine(
                color = paintColor,
                strokeWidth = 2f,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            drawLine(
                color = paintColor,
                strokeWidth = 2f,
                start = Offset(size.width, 0f),
                end = Offset(0f, size.height)
            )
        }
    }
}

@Composable
internal fun StarField(
    modifier: Modifier,
    globalTime: Float,
    starColors: List<Color>
) {
    data class Star(val pos: Offset, val size: Float, val phase: Float, val speed: Float, val colorIndex: Int)

    val stars = remember {
        val rnd = Random(42)
        List(140) {
            Star(
                pos = Offset(rnd.nextFloat(), rnd.nextFloat()),
                size = (1f + rnd.nextFloat() * 2f),
                phase = rnd.nextFloat() * (2f * PI.toFloat()),
                speed = 0.5f + rnd.nextFloat() * 1.5f,
                colorIndex = rnd.nextInt(0, 3)
            )
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val colorCount = starColors.size.coerceAtLeast(1)
        stars.forEach { s ->
            val t = (kotlin.math.sin(globalTime * s.speed + s.phase) + 1f) * 0.5f // 0..1
            val radius = s.size * (0.5f + t * 0.5f)
            val c = starColors.getOrElse(s.colorIndex % colorCount) { Color.White }
            drawCircle(
                color = c.copy(alpha = 0.4f + 0.6f * t),
                radius = radius,
                center = Offset(s.pos.x * width, s.pos.y * height)
            )
        }
    }
}

// You may want to move other functions like painterByNameOrNull here as well
@Composable
internal fun painterByNameOrNull(name: String): Painter? {
    val context = LocalContext.current
    val resId = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    return if (resId != 0) painterResource(id = resId) else null
}

