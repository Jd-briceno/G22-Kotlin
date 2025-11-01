package com.g22.orbitsoundkotlin.ui.screens.emotions

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.ui.screens.home.StarField
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import com.g22.orbitsoundkotlin.ui.viewmodels.ConstellationsViewModel

@Composable
@Suppress("UNUSED_PARAMETER")
fun ConstellationsScreen(
    username: String,
    onNavigateToHome: () -> Unit = {},
    viewModel: ConstellationsViewModel = viewModel()
) {

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
        Color(0xFFE3F2FD),
        Color(0xFFFFFDE7),
        Color(0xFFF3E5F5)
    )

    val containerBorderColor = Color.White.copy(alpha = 0.5f)

    val constellations = listOf(
        Constellation("fenix", "Phoenix", "From my own ashes, I carry the fire that will guide me home.", "Renewal, courage after loss, and embracing change.", R.drawable.fenix),
        Constellation("draco", "Draco", "My power is ancient, a force of nature that commands respect.", "Wisdom, authority, and untamed strength.", R.drawable.draco),
        Constellation("pegasus", "Pegasus", "With wings of starlight, I soar towards my highest aspirations.", "Inspiration, creativity, and boundless ambition.", R.drawable.pegasus),
        Constellation("cisne", "Cygnus", "In stillness, I find my grace and the quiet strength to glide on.", "Peace, elegance, and profound serenity.", R.drawable.cisne),
        Constellation("ursa_mayor", "Ursa Major", "I am the steadfast guardian, a shield against the darkness.", "Strength, family, and unwavering protection.", R.drawable.ursa_mayor),
        Constellation("cruz", "Crux", "My light is a beacon, a fixed point in the ever-changing cosmos.", "Faith, direction, and unerring guidance.", R.drawable.cruz)
    )

    // Collect state from ViewModel
    val selectedConstellation by viewModel.selectedConstellation.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val context = LocalContext.current

    // Auto-select constellation based on AI suggestion
    LaunchedEffect(suggestion) {
        if (suggestion != null && selectedConstellation == null) {
            val autoSelected = viewModel.autoSelectFromSuggestion(constellations)
            if (autoSelected != null) {
                Toast.makeText(
                    context,
                    "${autoSelected.title} has been autoselected, but feel free to change it",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010B19))
    ) {

        StarField(
            modifier = Modifier.fillMaxSize(),
            globalTime = globalTime,
            starColors = starColors
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 8.dp)
        ) {

            // OrbitSoundHeader(
            //     title = "Constellations",
            //     username = username,
            //     subtitle = "Explore the celestial tapestry"
            // )

            // Removed the navigation header for this screen; keep a small spacer for layout balance
            Spacer(modifier = Modifier.height(8.dp))

            // Show most selected emotion label under the header
            MostSelectedEmotionLabel(viewModel = viewModel)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ConstellationButton(drawableId = R.drawable.fenix, label = "Renewal", isSelected = selectedConstellation?.id == constellations[0].id, onClick = { viewModel.selectConstellation(constellations[0]) })
                    ConstellationButton(drawableId = R.drawable.draco, label = "Power", isSelected = selectedConstellation?.id == constellations[1].id, onClick = { viewModel.selectConstellation(constellations[1]) })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    ConstellationButton(drawableId = R.drawable.pegasus, label = "Ambition", isSelected = selectedConstellation?.id == constellations[2].id, onClick = { viewModel.selectConstellation(constellations[2]) })
                }



                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    if (selectedConstellation == null) {
                        Image(
                            painter = painterResource(id = R.drawable.constellations_default),
                            contentDescription = "Default Constellation Symbol",
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "How do you want to feel today?",
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        selectedConstellation?.let {
                            ConstellationInfo(
                                title = it.title,
                                subtitle = it.subtitle,
                                description = it.description
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    ConstellationButton(drawableId = R.drawable.cisne, label = "Serenity", isSelected = selectedConstellation?.id == constellations[3].id, onClick = { viewModel.selectConstellation(constellations[3]) })
                    ConstellationButton(drawableId = R.drawable.ursa_mayor, label = "Protection", isSelected = selectedConstellation?.id == constellations[4].id, onClick = { viewModel.selectConstellation(constellations[4]) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    ConstellationButton(drawableId = R.drawable.cruz, label = "Guidance", isSelected = selectedConstellation?.id == constellations[5].id, onClick = { viewModel.selectConstellation(constellations[5]) })
                }
            }


            Button(
                onClick = {
                    if (selectedConstellation != null) {
                        onNavigateToHome()
                    } else {
                        Toast.makeText(context, "Select your desired emotion before continuing your journey", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .border(1.dp, containerBorderColor, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(text = "Choose the colors of your sound", color = Color.White)
            }
        }
    }
}

@Composable
fun ConstellationInfo(title: String, subtitle: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = Color(0xFFFF8C00),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xFFFF8C00).copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun ConstellationButton(drawableId: Int, label: String, isSelected: Boolean = false, onClick: () -> Unit) {
    // Visual styles for selected vs normal
    val borderColor = if (isSelected) Color(0xFFFF8C00) else Color.Transparent
    val bgColor = if (isSelected) Color(0xFFFF8C00).copy(alpha = 0.12f) else Color.Transparent
    val imageSize = if (isSelected) 92.dp else 80.dp
    val labelColor = if (isSelected) Color(0xFFFF8C00) else Color.White
    val labelWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(imageSize)
                .background(bgColor, shape = RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = label,
                modifier = Modifier.size(imageSize - 16.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 14.sp,
            fontWeight = labelWeight
        )
    }
}

@Composable
fun MostSelectedEmotionLabel(viewModel: ConstellationsViewModel) {
    val mostSelected by viewModel.mostSelectedEmotion.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val suggestionLoading by viewModel.suggestionLoading.collectAsState()

    // Visual container centered horizontally
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .background(Color(0xFF071125).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (loading) {
                Text(text = "Loading most selected emotion...", color = Color.White, fontSize = 14.sp)
            } else {
                val label = mostSelected ?: "None"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your most selected emotion in this week: $label",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    when {
                        suggestionLoading -> Text(text = "Thinking...", color = Color(0xFFB4C6FF), fontSize = 12.sp)
                        suggestion != null -> Text(text = "Suggested emotion: ${suggestion!!}", color = Color(0xFFB4C6FF), fontSize = 12.sp)
                        else -> Text(text = "No suggestion available", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun ConstellationsScreenPreview() {
    OrbitSoundKotlinTheme {
        ConstellationsScreen(username = "Captain")
    }
}
