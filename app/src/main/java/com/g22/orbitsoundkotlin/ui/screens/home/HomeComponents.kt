package com.g22.orbitsoundkotlin.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OrbitSoundHeader(
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
fun OrbitNavbar(
    username: String,
    title: String,
    subtitle: String? = null,
    profilePainter: Painter? = null,
    onNavigateToHome: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
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
                .border(BorderStroke(2.dp, borderColor), buttonShape)
                .clickable { onNavigateToHome() },
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
                .background(dark)
                .border(BorderStroke(2.dp, borderColor), buttonShape)
                .clickable { onNavigateToProfile() },
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
fun Astronaut(floatYOffsetDp: Dp, heightDp: Dp) {
    val painter = painterByNameOrNull("astronaut_home")
    Box(modifier = Modifier.height(heightDp)) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = "Astronaut",
                modifier = Modifier
                    .offset(y = floatYOffsetDp)
                    .fillMaxWidth()
                    .height(heightDp)
            ) // REQUIRED_IMAGE: astronaut_home.png
        } else {
            Box(
                modifier = Modifier
                    .offset(y = floatYOffsetDp)
                    .fillMaxWidth()
                    .height(heightDp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0B1A2F)),
                contentAlignment = Alignment.Center
            ) {
                Text("astronaut_home.png", color = Color(0xFF90CAF9))
            }
        }
    }
}

@Composable
fun PlayerPill(
    albumName: String,
    songTitle: String,
    artist: String,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1A2F)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val albumPainter = painterByNameOrNull(albumName)
            if (albumPainter != null) {
                Image(
                    painter = albumPainter,
                    contentDescription = "Album",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                ) // REQUIRED_IMAGE: $albumName
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10223B)),
                    contentAlignment = Alignment.Center
                ) { Text("♫", color = Color(0xFF64B5F6)) }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    songTitle,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artist,
                    color = Color(0xFFB0BEC5),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF10223B))
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.28f)
                            .background(Color(0xFF64B5F6))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) { Icon(Icons.Filled.SkipPrevious, null, tint = Color(0xFFB0BEC5)) }
                IconButton(onClick = onPlayPause) { Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFFB0BEC5)) }
                IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, null, tint = Color(0xFFB0BEC5)) }
            }
        }
    }
}

data class ShortcutSpec(val label: String, @DrawableRes val iconRes: Int)

@Composable
fun ShortcutsFive(
    items: List<ShortcutSpec>,
    onShortcutClick: (ShortcutSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.take(5).forEach { spec ->
            ShortcutTile(
                spec = spec,
                onShortcutClick = { onShortcutClick(spec) }
            )
        }
    }
}

@Composable
private fun RowScope.ShortcutTile(
    spec: ShortcutSpec,
    onShortcutClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = Color.White.copy(alpha = 0.2f)
                ),
                onClick = onShortcutClick
            )
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = spec.iconRes),
            contentDescription = spec.label,
            modifier = Modifier
                .heightIn(min = 110.dp)
                .fillMaxWidth(0.88f),
            contentScale = ContentScale.Fit
        ) // REQUIRED_IMAGE: icon resource
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = spec.label,
            color = Color(0xFFE0E0E0),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StarField(
    modifier: Modifier,
    globalTime: Float,
    starColors: List<Color> // ya viene interpolado desde HomeScreen
) {
    // Generamos “tipos” de estrella con tamaños más contrastados
    data class Star(val pos: Offset, val base: Float, val phase: Float, val speed: Float, val colorIndex: Int)

    val stars = remember {
        val rnd = kotlin.random.Random(1337)
        List(180) {
            Star(
                pos = Offset(rnd.nextFloat(), rnd.nextFloat()),
                base = when (rnd.nextInt(0, 10)) {
                    in 0..5 -> 1.4f     // pequeñas
                    in 6..8 -> 2.3f     // medianas
                    else -> 3.4f        // grandes
                },
                phase = rnd.nextFloat() * (2f * kotlin.math.PI.toFloat()),
                speed = 0.6f + rnd.nextFloat() * 1.6f,
                colorIndex = rnd.nextInt(0, maxOf(1, starColors.size))
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val colors = if (starColors.isEmpty()) listOf(Color.White) else starColors

        stars.forEach { s ->
            // Twinkle (0..1)
            val t = (kotlin.math.sin(globalTime * s.speed + s.phase) + 1f) * 0.5f
            // Radio amplificado para que el cambio sea visible
            val r = (s.base + 1.2f * t)

            val c = colors[s.colorIndex % colors.size]
            val center = Offset(s.pos.x * w, s.pos.y * h)

            // Halo suave (más grande, menos alfa)
            drawCircle(
                color = c.copy(alpha = 0.22f + 0.38f * t),
                radius = r * 2.4f,
                center = center
            )
            // Núcleo
            drawCircle(
                color = c.copy(alpha = 0.65f + 0.35f * t),
                radius = r,
                center = center
            )
        }
    }
}


@Composable
fun rememberInterpolatedColors(
    prev: List<Color>,
    curr: List<Color>,
    phase: Float
): List<Color> {
    val count = maxOf(prev.size, curr.size, 1)
    return List(count) { index ->
        val previous = prev.getOrElse(index) { prev.lastOrNull() ?: Color.White }
        val current = curr.getOrElse(index) { curr.lastOrNull() ?: Color.White }
        lerpColor(current, previous, phase)
    }
}

private fun lerpColor(start: Color, end: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    fun lerp(a: Float, b: Float, ratio: Float) = a + (b - a) * ratio
    return Color(
        red = lerp(start.red, end.red, clamped),
        green = lerp(start.green, end.green, clamped),
        blue = lerp(start.blue, end.blue, clamped),
        alpha = lerp(start.alpha, end.alpha, clamped)
    )
}

@Composable
fun LightningLayer(modifier: Modifier, progress: Float) {
    Canvas(modifier = modifier) {
        drawLightning(progress)
    }
}

private fun DrawScope.drawLightning(progress: Float) {
    if (progress < 0.2f || progress > 0.8f) return

    val alpha = 1f - (progress - 0.2f)
    val width = size.width
    val height = size.height
    val rnd = Random((progress * 10_000).toInt())

    var x = rnd.nextFloat() * width
    var y = 0f
    val path = Path().apply { moveTo(x, y) }

    while (y < height) {
        val xOffset = (rnd.nextFloat() - 0.5f) * 50f
        y += 30f + rnd.nextFloat() * 20f
        path.lineTo(x + xOffset, y)
        x += xOffset
    }

    drawIntoCanvas {
        drawPath(
            path = path,
            color = Color(0xFF448AFF).copy(alpha = alpha.coerceIn(0f, 1f)),
            style = Stroke(width = 2f)
        )
    }
}

@Composable
fun painterByNameOrNull(name: String): Painter? {
    val context = LocalContext.current
    val resId = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    return if (resId != 0) painterResource(id = resId) else null
}
