package com.g22.orbitsoundkotlin.ui.screens

import android.app.Activity
import android.content.Context
import android.location.Location
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ------------------------------- MODELOS -------------------------------

data class GenreSpec(
    val name: String,
    val drawableName: String,
    val gradientColors: List<Color>,
    val description: String,
    val subgenres: List<String>,
    val fontFamilyName: String
)

object Genres {
    const val punk = "Punk"
    const val kpop = "K-Pop"
    const val jrock = "J-Rock"
    const val pop = "Pop"
    const val jazz = "Jazz"
    const val rock = "Rock"
    const val classical = "Classical"
    const val metal = "Heavy Metal"
    const val edm = "EDM"
    const val rap = "Rap"
    const val medieval = "Medieval"
    const val anisong = "Anisong"
}

private val GENRES: List<GenreSpec> = listOf(
    GenreSpec(
        name = Genres.medieval,
        drawableName = "medieval", // REQUIRED_IMAGE: medieval.png
        gradientColors = listOf(Color(189, 0, 27, 204), Color(211, 164, 46, 77)),
        description = "Echoes of ancient times — Medieval music brings the sounds of castles, battles, and legends to life.",
        subgenres = listOf("Celtic", "Medieval", "Fantasy"),
        fontFamilyName = "Medieval"
    ),
    GenreSpec(
        name = Genres.punk,
        drawableName = "punk", // REQUIRED_IMAGE: punk.png
        gradientColors = listOf(Color(30, 30, 30, 204), Color(251, 11, 11, 77)),
        description = "Raw energy and DIY attitude since the 70s.",
        subgenres = listOf("Punk Rock", "Hardcore", "Post-Punk"),
        fontFamilyName = "Punk"
    ),
    GenreSpec(
        name = Genres.kpop,
        drawableName = "kpop", // REQUIRED_IMAGE: kpop.png
        gradientColors = listOf(Color(163, 140, 205, 204), Color(0, 0, 0, 77)),
        description = "Colorful, polished, choreographed hits.",
        subgenres = listOf("Idol Pop", "K-R&B", "K-Hip Hop", "Girl/Boy Groups"),
        fontFamilyName = "Kpop"
    ),
    GenreSpec(
        name = Genres.jrock,
        drawableName = "jrock", // REQUIRED_IMAGE: jrock.png
        gradientColors = listOf(Color(94, 2, 0, 204), Color(222, 5, 0, 77)),
        description = "Powerful riffs with melodic vocals and flair.",
        subgenres = listOf("Visual Kei", "Alternative", "Anime Rock", "Post-Rock"),
        fontFamilyName = "JRock"
    ),
    GenreSpec(
        name = Genres.pop,
        drawableName = "pop", // REQUIRED_IMAGE: pop.png
        gradientColors = listOf(Color(214, 15, 132, 204), Color(255, 212, 0, 77)),
        description = "Catchy and ever-evolving chart toppers.",
        subgenres = listOf("Dance Pop", "Synth Pop", "Electro Pop", "Indie Pop"),
        fontFamilyName = "Pop"
    ),
    GenreSpec(
        name = Genres.rap,
        drawableName = "rap", // REQUIRED_IMAGE: rap.png
        gradientColors = listOf(Color(0, 0, 0, 204), Color(212, 175, 55, 153)),
        description = "Stories of struggle and triumph through rhythm and rhyme.",
        subgenres = listOf("Trap", "Boom Bap", "Gangsta Rap", "Conscious Rap"),
        fontFamilyName = "Rap"
    ),
    GenreSpec(
        name = Genres.jazz,
        drawableName = "jazz", // REQUIRED_IMAGE: jazz.png
        gradientColors = listOf(Color(23, 77, 38, 204), Color(77, 134, 87, 77)),
        description = "Improvisation and soulful grooves.",
        subgenres = listOf("Bebop", "Swing", "Cool Jazz", "Fusion"),
        fontFamilyName = "Jazz"
    ),
    GenreSpec(
        name = Genres.rock,
        drawableName = "rock", // REQUIRED_IMAGE: rock.png
        gradientColors = listOf(Color(52, 42, 29, 204), Color(255, 69, 0, 77)),
        description = "Guitars, drums and rebellion.",
        subgenres = listOf("Classic Rock", "Alternative", "Indie Rock", "Progressive"),
        fontFamilyName = "Rock"
    ),
    GenreSpec(
        name = Genres.classical,
        drawableName = "classical", // REQUIRED_IMAGE: classical.png
        gradientColors = listOf(Color(255, 201, 0, 204), Color(234, 234, 234, 77)),
        description = "Timeless orchestras and elegant compositions.",
        subgenres = listOf("Baroque", "Romantic", "Opera", "Symphony"),
        fontFamilyName = "Classical"
    ),
    GenreSpec(
        name = Genres.metal,
        drawableName = "metal", // REQUIRED_IMAGE: metal.png
        gradientColors = listOf(Color(20, 20, 20, 204), Color(169, 169, 169, 77)),
        description = "Loud, powerful and intense.",
        subgenres = listOf("Thrash", "Death", "Power Metal", "Black Metal"),
        fontFamilyName = "Metal"
    ),
    GenreSpec(
        name = Genres.anisong,
        drawableName = "anisong", // REQUIRED_IMAGE: anisong.png
        gradientColors = listOf(Color(0, 191, 198, 204), Color(255, 191, 0, 102)),
        description = "The spirit of anime worlds in powerful melodies.",
        subgenres = listOf("Anime Openings", "Anime Endings", "Character Songs"),
        fontFamilyName = "Anime"
    ),
    GenreSpec(
        name = Genres.edm,
        drawableName = "edm", // REQUIRED_IMAGE: edm.png
        gradientColors = listOf(Color(4, 217, 255, 204), Color(138, 0, 196, 77)),
        description = "High-energy beats for the dancefloor.",
        subgenres = listOf("House", "Techno", "Dubstep"),
        fontFamilyName = "EDM"
    ),
)

// ------------------------------- SCREEN -------------------------------

@Composable
fun GenreSelectorScreen(
    onOpenHome: () -> Unit = {},
    onExploreGenre: (GenreSpec) -> Unit = {},
) {
    val context = LocalContext.current
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = GENRES.size * 1000)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // índice del ítem “centrado” (aprox) sin usar getters composables en mal contexto
    val selectedIndex by remember {
        derivedStateOf {
            val itemPx = with(density) { 480.dp.toPx() } // altura estimada de cada tarjeta
            val approx = listState.firstVisibleItemIndex +
                    (listState.firstVisibleItemScrollOffset / itemPx)
            approx.roundToInt()
        }
    }

    var popup: GenreSpec? by remember { mutableStateOf(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
    ) {
        val repeated = List(GENRES.size * 400) { GENRES[it % GENRES.size] }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 48.dp)
        ) {
            itemsIndexed(repeated) { idx, spec ->
                val isSelected = idx == selectedIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.6f,
                    animationSpec = tween(220, easing = LinearEasing),
                    label = "scale"
                )

                PlanetItem(
                    spec = spec,
                    isSelected = isSelected,
                    scale = scale,
                    onTap = {
                        if (isSelected) {
                            popup = spec
                        } else {
                            scope.launch { listState.animateScrollToItem(idx) }
                        }
                    }
                )
            }
        }

        // Top bar: Home (izq) + Black hole (der)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = onOpenHome) {
                Icon(Icons.Filled.Home, contentDescription = "Home")
            }
            // REQUIRED_IMAGE: black_hole.png
            val blackHole = painterByNameOrNull("black_hole")
            if (blackHole != null) {
                IconButton(onClick = { showExitDialog = true }) {
                    Image(painter = blackHole, contentDescription = "Exit", modifier = Modifier.size(44.dp))
                }
            } else {
                FilledTonalIconButton(onClick = { showExitDialog = true }) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit")
                }
            }
        }

        // Popup
        popup?.let { spec ->
            GenrePopup(
                spec = spec,
                onDismiss = { popup = null },
                onExplore = {
                    popup = null
                    onExploreGenre(spec)
                },
                onPreview = { /* TODO */ }
            )
        }

        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("¿Salir?", color = Color.White) },
                text = { Text("¿Seguro que quieres cerrar la aplicación?", color = Color(0xCCFFFFFF)) },
                confirmButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }) { Text("Salir", color = Color(0xFFFF6B6B)) }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) { Text("Cancelar", color = Color.White) }
                },
                containerColor = Color(0xFF0B1A2F)
            )
        }
    }
}

// ------------------------------- ÍTEM -------------------------------

@Composable
private fun PlanetItem(
    spec: GenreSpec,
    isSelected: Boolean,
    scale: Float,
    onTap: () -> Unit
) {
    val painter = painterByNameOrNull(spec.drawableName)
    // REQUIRED_IMAGE: <spec.drawableName>.png

    Card(
        onClick = onTap,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .height(480.dp)
            .padding(vertical = 12.dp)
            .scale(scale)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (painter != null) {
                Image(
                    painter = painter,
                    contentDescription = spec.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.82f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF10223B)),
                    contentAlignment = Alignment.Center
                ) { Text("${spec.drawableName}.png", color = Color(0xFF90CAF9)) }
            }

            Text(
                text = spec.name,
                color = Color(0xFFE9E8EE),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ------------------------------- POPUP -------------------------------

@Composable
private fun GenrePopup(
    spec: GenreSpec,
    onDismiss: () -> Unit,
    onExplore: () -> Unit,
    onPreview: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(spec.gradientColors), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    spec.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    spec.description,
                    color = Color(0xDDFFFFFF),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                FlowRowWrap(spec.subgenres)

                Spacer(Modifier.height(16.dp))
                val painter = painterByNameOrNull(spec.drawableName)
                // REQUIRED_IMAGE: <spec.drawableName>.png
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = spec.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF10223B)),
                        contentAlignment = Alignment.Center
                    ) { Text("${spec.drawableName}.png", color = Color(0xFF90CAF9)) }
                }

                Spacer(Modifier.height(16.dp))

                Column {
                    GradientButton("Explore Songs", spec.gradientColors, onExplore)
                    Spacer(Modifier.height(10.dp))
                    GradientButton("Listen to Preview", spec.gradientColors.reversed(), onPreview)
                }
            }
        },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun FlowRowWrap(items: List<String>) {
    // layout de chips simple y robusto
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var line = mutableListOf<String>()
        var used = 0
        val max = 32 * 10 // ancho aproximado en “caracteres”
        items.forEach { s ->
            val w = s.length + 6
            if (used + w > max && line.isNotEmpty()) {
                ChipRow(line)
                Spacer(Modifier.height(8.dp))
                line = mutableListOf()
                used = 0
            }
            line.add(s); used += w + 2
        }
        if (line.isNotEmpty()) ChipRow(line)
    }
}

@Composable
private fun ChipRow(row: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { sub ->
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Color.White)
            ) {
                Text(
                    sub,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun GradientButton(text: String, colors: List<Color>, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(colors))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

// ------------------------------- UTILS -------------------------------

@Composable
private fun painterByNameOrNull(name: String): Painter? {
    val context = LocalContext.current
    val id = remember(name) { context.resources.getIdentifier(name, "drawable", context.packageName) }
    return if (id != 0) painterResource(id = id) else null
}

// (no usamos localización aquí; mantenido por paridad con tu proyecto)
@Suppress("MissingPermission")
private fun getLastKnownLocation(context: Context): Location? = null
