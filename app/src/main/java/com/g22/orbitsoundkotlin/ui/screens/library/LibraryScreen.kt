package com.g22.orbitsoundkotlin.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.ui.components.VinylWithCover
import com.g22.orbitsoundkotlin.ui.screens.home.OrbitNavbar
import com.g22.orbitsoundkotlin.ui.viewmodels.LibraryViewModel

@Composable
fun LibraryScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // UI Principal
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }

        item {
            OrbitNavbar(
                username = "Jay Walker",
                title = "Ninja",
                subtitle = "Star Archive",
                profilePainter = null,
                onNavigateToHome = onNavigateToHome,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        item {
            Spacer(modifier = Modifier.height(2.dp))
        }

        item {
            SearchBarComposable(
                onSearch = viewModel::searchTracks
            )
        }

        item {
            Spacer(modifier = Modifier.height(5.dp))
        }

        // Mostrar mensaje de error si existe
        uiState.error?.let { errorMessage ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0xFFFF5252).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ Error: $errorMessage",
                        color = Color(0xFFFFCDD2),
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (uiState.searchLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE9E8EE)
                    )
                }
            }
        }

        if (uiState.searchResults.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.height(180.dp)
                ) {
                    itemsIndexed(uiState.searchResults) { index, song ->
                        SongResultCard(
                            song = song,
                            onClick = {
                                // 📊 Analytics: Track with position and query
                                viewModel.selectTrackFromSearch(
                                    track = song,
                                    position = index,
                                    query = uiState.lastSearchQuery
                                )
                            }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Library",
                color = Color(0xFFE9E8EE),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
        if (uiState.playlistsLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFE9E8EE)
                    )
                }
            }
        } else {
            // Secciones dinámicas de playlists
            uiState.section1?.let { section ->
                item {
                    SongSection(
                        title = section.section.title,
                        subtitle = section.section.subtitle,
                        songs = section.tracks,
                        onSongClick = { track ->
                            // 📊 Analytics: Track with section
                            viewModel.selectTrackFromSection(track, sectionPosition = 1)
                        }
                    )
                }
            }

            uiState.section2?.let { section ->
                item {
                    SongSection(
                        title = section.section.title,
                        subtitle = section.section.subtitle,
                        songs = section.tracks,
                        onSongClick = { track ->
                            // 📊 Analytics: Track with section
                            viewModel.selectTrackFromSection(track, sectionPosition = 2)
                        }
                    )
                }
            }

            uiState.section3?.let { section ->
                item {
                    SongSection(
                        title = section.section.title,
                        subtitle = section.section.subtitle,
                        songs = section.tracks,
                        onSongClick = { track ->
                            // 📊 Analytics: Track with section
                            viewModel.selectTrackFromSection(track, sectionPosition = 3)
                        }
                    )
                }
            }

            uiState.section4?.let { section ->
                item {
                    SongSection(
                        title = section.section.title,
                        subtitle = section.section.subtitle,
                        songs = section.tracks,
                        onSongClick = { track ->
                            // 📊 Analytics: Track with section
                            viewModel.selectTrackFromSection(track, sectionPosition = 4)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    uiState.selectedTrack?.let { track ->
        SongDetailModal(
            track = track,
            onDismiss = viewModel::dismissTrackDetail
        )
    }
}

@Composable
fun SongDetailModal(
    track: Track,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(
                    Color(0xFF010B19),
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFB4B1B8),
                    RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
                .clickable { /* Prevent dismiss when clicking modal content */ }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                VinylWithCover(
                    albumArt = track.albumArt,
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = track.title,
                    color = Color(0xFFE9E8EE),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.artist,
                    color = Color(0xFFE9E8EE),
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "KPop Demon Hunters (Soundtrack from Netflix Film)",
                    color = Color(0xFFB4B1B8),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = track.duration,
                    color = Color(0xFFB4B1B8),
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { /* Play action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Add to playlist action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Playlist",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { 
                            // TODO: Implement like functionality with achievement
                            // val context = LocalContext.current
                            // viewModel.likeTrack(track, context)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = { /* Share action */ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF010B19),
                                CircleShape
                            )
                            .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFE9E8EE),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarComposable(
    onSearch: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(y = 15.dp)
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    Color(0xFF010B19),
                    RoundedCornerShape(25.dp)
                )
                .border(
                    1.dp,
                    Color(0xFFB4B1B8),
                    RoundedCornerShape(25.dp)
                )
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            placeholder = {
                Text(
                    "Find your rhythm...",
                    color = Color(0xFFA1BBD1),
                    fontSize = 15.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            },
            modifier = Modifier
                .offset(y = 15.dp)
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFFE9E8EE),
                unfocusedTextColor = Color(0xFFE9E8EE),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 15.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        )

        Box(
            modifier = Modifier
                .offset(x = 16.dp, y = 0.dp)
                .align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF010B19), CircleShape)
                    .border(1.dp, Color(0xFFE9E8EE), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Transparent, CircleShape)
                    .border(1.dp, Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFB4B1B8), CircleShape)
                    .align(Alignment.Center)
            )

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color(0xFF010B19),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SongResultCard(
    song: Track,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(165.dp)
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VinylWithCover(
            albumArt = song.albumArt,
            isSpinning = false,
            modifier = Modifier.clickable { onClick() }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${song.title}\n${song.artist}",
            color = Color(0xFFE9E8EE),
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun SongSection(
    title: String,
    songs: List<Track>,
    onSongClick: (Track) -> Unit,
    subtitle: String? = null
) {
    Column {
        Text(
            text = title,
            color = Color(0xFFE9E8EE),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )
        
        subtitle?.let {
            Text(
                text = it,
                color = Color(0xFFB4B1B8),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        LazyRow(
            modifier = Modifier.height(180.dp)
        ) {
            items(songs) { song ->
                SongResultCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}
