package com.g22.orbitsoundkotlin.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.ui.screens.shared.OrbitSoundHeader
import com.g22.orbitsoundkotlin.ui.screens.shared.StarField
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import kotlin.math.cos
import kotlin.math.sin
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.EmotionControlState
import com.g22.orbitsoundkotlin.models.SliderEmotionControlState
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.clickable // New Import
import androidx.compose.ui.res.painterResource
import kotlin.math.roundToInt // New Import
import com.g22.orbitsoundkotlin.R

// Define custom colors for better reusability
private val containerBorderColor = Color.White.copy(alpha = 0.4f)
private val containerShape = RoundedCornerShape(16.dp)
private val accentRed = Color(0xFFFF4747)
private val accentYellow = Color(0xFFFFD700)
private val knobGray = Color(0xFFC4C4C4)


val anxietyKnobEmotions = mapOf(
    "Anxiety" to EmotionModel(
        id = "anxiety",
        name = "Anxiety",
        description = "A knot of dread in the stomach",
        color = Color(0xFF5D48D3),
        iconRes = R.drawable.anxiety,
        source = "knob1" // Assign a source
    ),
    "Anger" to EmotionModel(
        id = "anger",
        name = "Anger",
        description = "Burning silently under my skin",
        color = Color(0xFFFF595D),
        iconRes = R.drawable.anger,
        source = "knob1"
    ),
    "Disgust" to EmotionModel(
        id = "disgust",
        name = "Disgust",
        description = "A bitter taste in the air",
        color = Color(0xFF91C836),
        iconRes = R.drawable.disgust,
        source = "knob1"
    ),
    "Envy" to EmotionModel(
        id = "envy",
        name = "Envy",
        description = "Yearning for what is not mine",
        color = Color(0xFF30CA84),
        iconRes = R.drawable.envy,
        source = "knob1"
    ),
    "Embarrassment" to EmotionModel(
        id = "embarrassment",
        name = "Embarrassment",
        description = "Heat rising to my cheeks",
        color = Color(0xFFB53F5F),
        iconRes = R.drawable.embarrassment,
        source = "knob2" // Assign a different source for the second knob
    ),
    "Love" to EmotionModel(
        id = "love",
        name = "Love",
        description = "A warmth that softens the edges",
        color = Color(0xFFB53E8E),
        iconRes = R.drawable.love,
        source = "knob2"
    ),
    "Boredom" to EmotionModel(
        id = "boredom",
        name = "Boredom",
        description = "Drifting in slow, gray time",
        color = Color(0xFFA08888),
        iconRes = R.drawable.boredom,
        source = "knob2"
    ),
    "Joy" to EmotionModel(
        id = "joy",
        name = "Joy",
        description = "A bright spark bursting within",
        color = Color(0xFFFECA39),
        iconRes = R.drawable.joy,
        source = "slider1" // And another for the slider
    ),
    "Sadness" to EmotionModel(
        id = "sadness",
        name = "Sadness",
        description = "Raindrops collecting behind my eyes",
        color = Color(0xFF2390D3),
        iconRes = R.drawable.sadness,
        source = "slider1"
    ),
    "Fear" to EmotionModel(
        id = "fear",
        name = "Fear",
        description = "A chill crawling up my spine",
        color = Color(0xFF944FBC),
        iconRes = R.drawable.fear,
        source = "slider1"
    )
)

fun getEmotionFromKnobAngle(
    angle: Float,
    emotionLabels: List<String>
): EmotionModel {
    // Normalize angle to 0-360 range, starting at the top position (Anxiety)
    // The starting position (initialAngle) is -45f in the EmotionKnob.
    // Let's treat the -45f position (Anxiety) as the start of the sweep.
    // Normalized angle where 0 is the center of Anxiety and 270 is the center of Envy.
    var normalizedAngle = (angle + 45f) % 360 // Shift to make -45f (Anxiety) = 0f
    if (normalizedAngle < 0) normalizedAngle += 360f

    val sectorSize = 270f / emotionLabels.size // 270 / 4 = 67.5 degrees per sector

    return when {
        // Sector 1: Anxiety (approx 0 to 67.5)
        normalizedAngle in 0f..sectorSize -> anxietyKnobEmotions[emotionLabels[0]]!!

        // Sector 2: Envy (approx 67.5 to 135)
        normalizedAngle in sectorSize..sectorSize * 2 -> anxietyKnobEmotions[emotionLabels[3]]!!

        // Sector 3: Anger (approx 135 to 202.5)
        normalizedAngle in sectorSize * 2..sectorSize * 3 -> anxietyKnobEmotions[emotionLabels[2]]!!

        // Sector 4: Disgust (approx 202.5 to 270)
        else -> anxietyKnobEmotions[emotionLabels[1]]!!
    }
}

fun getEmotionFromKnob2Angle(
    angle: Float
): EmotionModel {
    var normalizedAngle = (angle + 45f) % 360
    if (normalizedAngle < 0) normalizedAngle += 360f
    val sectorSize = 180f / 3
    return when (angle.roundToInt()) {
        -45 -> anxietyKnobEmotions["Embarrassment"]!! // Top-Start
        45 -> anxietyKnobEmotions["Embarrassment"]!!  // Top-End
        135 -> anxietyKnobEmotions["Love"]!!          // Bottom-End
        225 -> anxietyKnobEmotions["Boredom"]!!       // Bottom-Start
        else -> anxietyKnobEmotions["Embarrassment"]!! // Default to initial state
    }
}

fun getEmotionFromSliderValue(
    value: Float
): EmotionModel {
    return when {
        // Top Third: Joy (Value > 0.66)
        value >= 0.66f -> anxietyKnobEmotions["Joy"]!!
        // Middle Third: Sadness (Value > 0.33 and <= 0.66)
        value > 0.33f && value < 0.66f -> anxietyKnobEmotions["Sadness"]!!
        // Bottom Third: Fear (Value <= 0.33)
        else -> anxietyKnobEmotions["Fear"]!!
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Opt-in for Slider API
@Composable
fun StellarEmotionsScreen(
    username: String
) {
    // Represents the collection of emotions the user has selected.
    var selectedEmotions by remember {
        mutableStateOf(listOf<EmotionModel>())
    }
    // Keeps track of the last selected emotion to show in the main display
    var lastSelectedEmotion by remember {
        mutableStateOf<EmotionModel?>(null)
    }
    // Represents the current selection on the FIRST KNOB
    val initialKnob1Emotion = anxietyKnobEmotions["Anxiety"]!!
    var knob1State by remember {
        mutableStateOf(
            EmotionControlState(
                selectedEmotion = initialKnob1Emotion,
                angle = -45f // Corresponds to Anxiety's position
            )
        )
    }
    // Represents the current selection on the SECOND KNOB
    val initialKnob2Emotion = anxietyKnobEmotions["Embarrassment"]!!
    var knob2State by remember {
        mutableStateOf(
            EmotionControlState(
                selectedEmotion = initialKnob2Emotion,
                angle = -45f // Corresponds to Embarrassment's position
            )
        )
    }
    val initialSliderEmotion = anxietyKnobEmotions["Joy"]!!
    var sliderState by remember {
        mutableStateOf(
            SliderEmotionControlState(
                selectedEmotion = initialSliderEmotion,
                value = 0.7f // Corresponds to Joy's initial position
            )
        )
    }

    // Function to update *both* the array of selected emotions *and* the last selected emotion
    val updateSelectedEmotionsAndLast: (EmotionModel) -> Unit = { newEmotion ->
        selectedEmotions = selectedEmotions.toMutableList().apply {
            // Find the index of an existing emotion from the same source
            val existingIndex = indexOfFirst { it.source == newEmotion.source }

            if (existingIndex != -1) {
                // If found, replace it
                this[existingIndex] = newEmotion
            } else {
                // Otherwise, add the new emotion
                add(newEmotion)
            }
        }
        // KEY CHANGE: Update the last selected emotion here
        lastSelectedEmotion = newEmotion
    }
    // Add initial emotions from all controls when the screen first composes
    LaunchedEffect(Unit) {
        val initialEmotions = mutableListOf<EmotionModel>()
        val initialKnob1Emotion = anxietyKnobEmotions["Anxiety"]!!
        val initialKnob2Emotion = anxietyKnobEmotions["Embarrassment"]!!
        val initialSliderEmotion = anxietyKnobEmotions["Joy"]!!

        // Add initial emotions, checking if an emotion from that source already exists (for safety)
        if (selectedEmotions.none { it.source == initialKnob1Emotion.source }) {
            initialEmotions.add(initialKnob1Emotion)
        }
        if (selectedEmotions.none { it.source == initialKnob2Emotion.source }) {
            initialEmotions.add(initialKnob2Emotion)
        }
        if (selectedEmotions.none { it.source == initialSliderEmotion.source }) {
            initialEmotions.add(initialSliderEmotion)
        }

        selectedEmotions = selectedEmotions + initialEmotions

        // KEY CHANGE: Set the last selected emotion to one of the initial values, e.g., the last one added
        if (lastSelectedEmotion == null && initialEmotions.isNotEmpty()) {
            lastSelectedEmotion = initialEmotions.last() // Set to Joy as an arbitrary initial 'last'
        }
    }
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
                .padding(horizontal = 8.dp)
                // Apply padding to respect the system status and navigation bars
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Reused Header component
            OrbitSoundHeader(
                title = "Emotions", // Updated title
                username = username,
                subtitle = "How are you feeling today captain?", // Updated subtitle
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Allow row to fill available space
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Panel
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PASS THE CURRENTLY SELECTED EMOTION
                    EmotionDisplay(
                        currentEmotion = lastSelectedEmotion ?: initialKnob1Emotion
                    )
                    // PASS THE LIST OF SELECTED EMOTIONS
                    EmotionList(
                        selectedEmotions = selectedEmotions
                    )

                    // UPDATED KNOB 1
                    EmotionKnob(
                        labelTop = "Anxiety",
                        labelBottom = "Anger",
                        labelLeft = "Disgust",
                        labelRight = "Envy",
                        knobColor = knob1State.selectedEmotion.color,
                        initialAngle = knob1State.angle,
                        onAngleChange = { newAngle ->
                            val newEmotion = getEmotionFromKnobAngle(
                                angle = newAngle,
                                emotionLabels = listOf("Anxiety", "Disgust", "Anger", "Envy")
                            )
                            knob1State = knob1State.copy(
                                selectedEmotion = newEmotion,
                                angle = newAngle
                            )
                            updateSelectedEmotionsAndLast(newEmotion)
                        }
                    )
                    // UPDATED KNOB 2
                    EmotionKnob(
                        labelTop = "Embarrassment",
                        labelBottom = "Love",
                        labelLeft = "Boredom",
                        knobColor = knob2State.selectedEmotion.color,
                        initialAngle = knob2State.angle,
                        onAngleChange = { newAngle ->
                            val newEmotion = getEmotionFromKnob2Angle(newAngle)
                            knob2State = knob2State.copy(
                                selectedEmotion = newEmotion,
                                angle = newAngle
                            )
                            updateSelectedEmotionsAndLast(newEmotion)
                        }
                    )
                }
                // Right Panel - Now wrapped in a Column to provide the correct scope
                Column(modifier = Modifier.weight(1f)) {
                    // ⭐ UPDATED SLIDER CALL
                    VolumeSlider(
                        sliderPosition = sliderState.value,
                        selectedEmotion = sliderState.selectedEmotion,
                        onValueChange = { newValue ->
                            val newEmotion = getEmotionFromSliderValue(newValue)
                            sliderState = sliderState.copy(
                                selectedEmotion = newEmotion,
                                value = newValue
                            )
                            // Update the main list of selected emotions
                            updateSelectedEmotionsAndLast(newEmotion)
                        }
                    )
                }
            }

            // Bottom Button
            Button(
                onClick = { /* TODO */ },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
                    .border(1.dp, containerBorderColor, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(text = "Ready to ship?", color = Color.White)
            }
        }
    }
}@Composable
fun EmotionDisplay(currentEmotion: EmotionModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Load drawable resource at composition time
        Icon(
            painter = painterResource(id = currentEmotion.iconRes),
            contentDescription = currentEmotion.name,
            tint = currentEmotion.color, // will tint vector drawables
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = currentEmotion.description,
            color = currentEmotion.color,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun EmotionList(selectedEmotions: List<EmotionModel>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, containerBorderColor, containerShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Iterate over selected emotions to display them
        if (selectedEmotions.isNotEmpty()) {
            selectedEmotions.forEach { emotion ->
                // Load the drawable resource at composition time
                Icon(
                    painter = painterResource(id = emotion.iconRes),
                    contentDescription = emotion.name,
                    tint = emotion.color,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // If the list is empty, show a placeholder
            Text(
                text = "No emotions selected",
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}


@Composable
fun EmotionKnob(
    labelTop: String,
    labelBottom: String,
    labelLeft: String,
    labelRight: String? = null,
    knobColor: Color,
    initialAngle: Float,
    knobRadius: Dp = 50.dp,
    // NEW: Lambda to report angle change back to the parent
    onAngleChange: ((Float) -> Unit)? = null // Optional for non-interactive knobs
) {
    // Current rotation state, managed by the parent via initialAngle,
    // but the clickable event is handled here.
    val currentRotation = remember(initialAngle) { mutableStateOf(initialAngle) }

    // Logic to cycle the angle by 90 degrees on tap
    val onKnobClick = {
        if (onAngleChange != null) {
            // Cycle through the four 90-degree steps (-45, 45, 135, 225)
            val newAngle = when (currentRotation.value.roundToInt()) {
                -45 -> 45f    // Anxiety -> Disgust
                45 -> 135f   // Disgust -> Anger
                135 -> 225f  // Anger -> Envy
                else -> -45f  // Envy -> Anxiety
            }
            currentRotation.value = newAngle
            onAngleChange.invoke(newAngle)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // --- MODIFIED: Labels are now aligned to the corners ---
        Text(text = labelTop, color = Color.White, modifier = Modifier.align(Alignment.TopStart))
        Text(text = labelLeft, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(top = knobRadius * 2 + 24.dp))
        labelRight?.let {
            Text(text = it, color = Color.White, modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(
            text = labelBottom,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(top = knobRadius * 2 + 24.dp)
        )

        // Knob
        Box(
            modifier = Modifier
                .size(knobRadius * 2)
                .padding(top = 24.dp)
                .clickable(enabled = onAngleChange != null, onClick = onKnobClick), // ADDED CLICKABLE
            contentAlignment = Alignment.Center
        ) {
            // Ticks around the knob (remain the same)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val radius = size.minDimension / 2.0f
                for (i in 0 until 16) {
                    val angle = (i / 16f) * 360f
                    if (angle < 45 || angle > 315) continue
                    val x = center.x + radius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                    val y = center.y + radius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()
                    drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
                }
            }
            // The colored circle of the knob
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(CircleShape)
                    .background(knobColor) // Use the dynamic color
                    .rotate(currentRotation.value) // Use the dynamic angle
            ) {
                // The indicator line on the knob
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .width(4.dp)
                        .height(20.dp)
                        .background(Color.Black)
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // Opt-in for Slider API
@Composable
fun ColumnScope.VolumeSlider(
    sliderPosition: Float,
    selectedEmotion: EmotionModel,
    onValueChange: (Float) -> Unit // State-hoisting for value change
) {
    // The Column now receives the weight from its parent, and it will fill the height.
    Column(
        modifier = Modifier
            .weight(1f) // Use weight to fill available space
            .fillMaxHeight()
            .border(1.dp, containerBorderColor, containerShape)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .padding(horizontal = 16.dp) // Add some padding for the slider and ticks
        ) {
            // Ticks and Labels
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tickCount = 20
                val canvasHeight = size.height
                val step = canvasHeight / tickCount

                for (i in 0..tickCount) {
                    val y = step * i
                    drawLine(
                        color = Color.White.copy(alpha=0.5f),
                        start = Offset(size.width * 0.25f, y),
                        end = Offset(size.width * 0.35f, y),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha=0.5f),
                        start = Offset(size.width * 0.65f, y),
                        end = Offset(size.width * 0.75f, y),
                        strokeWidth = 2f
                    )
                }
            }

            // Box to contain and center the vertical slider
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Slider(
                    value = sliderPosition,
                    onValueChange = onValueChange, // Use the hoisted onValueChange lambda
                    modifier = Modifier
                        // REMOVED .width(300.dp)
                        .fillMaxWidth() // New: Make it fill the available width (which becomes the vertical length)
                        .rotate(-90f),    // then rotate it to be vertical
                    colors = SliderDefaults.colors(
                        thumbColor = selectedEmotion.color,
                        activeTrackColor = Color.LightGray.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .rotate(90f) // Counter-rotate the thumb
                                .size(width = 24.dp, height = 40.dp)
                                .background(selectedEmotion.color, RoundedCornerShape(8.dp)) // ⭐ Use emotion color
                        )
                    }
                )
            }


            // Labels aligned with the slider
            Box(modifier = Modifier.fillMaxSize()) {
                Text(text = "Joy", color = accentYellow, modifier = Modifier.align(Alignment.TopEnd))
                Text(text = "Sadness", color = Color.White, modifier = Modifier.align(Alignment.CenterEnd))
                Text(text = "Fear", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp))
            }
        }
        Text(
            text = "Volume",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun StellarEmotionsScreenPreview() {
    OrbitSoundKotlinTheme {
        StellarEmotionsScreen(username = "User")
    }
}
