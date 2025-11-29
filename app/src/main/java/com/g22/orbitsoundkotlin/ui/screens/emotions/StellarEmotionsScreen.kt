package com.g22.orbitsoundkotlin.ui.screens.emotions

import com.g22.orbitsoundkotlin.ui.viewmodels.StellarEmotionsViewModel
import android.widget.Toast
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
import com.g22.orbitsoundkotlin.ui.screens.home.OrbitSoundHeader
import com.g22.orbitsoundkotlin.ui.screens.home.StarField
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import kotlin.math.cos
import kotlin.math.sin
import com.g22.orbitsoundkotlin.models.EmotionModel
import com.g22.orbitsoundkotlin.models.EmotionControlState
import com.g22.orbitsoundkotlin.models.SliderEmotionControlState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.R
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.atan2
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.net.Uri

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
        source = "knob1"
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
        source = "knob2"
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
        source = "slider1"
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
    // Normalize angle to 0-360 range
    var normalizedAngle = angle % 360f
    if (normalizedAngle < 0) normalizedAngle += 360f

    // Divide full 360° into 4 equal sectors (90° each) corresponding to the corners.
    // Angles: 0 = top, 90 = right, 180 = bottom, 270 = left
    // Sector mapping (in the same order the UI passes emotions):
    // 0..90 -> emotionLabels[0] (top-right / between top and right)
    // 90..180 -> emotionLabels[1] (bottom-right)
    // 180..270 -> emotionLabels[2] (bottom-left)
    // 270..360 -> emotionLabels[3] (top-left)

    val sectorSize = 360f / emotionLabels.size // should be 90f for 4 emotions
    val sectorIndex = (normalizedAngle / sectorSize).toInt().coerceIn(0, emotionLabels.size - 1)

    return anxietyKnobEmotions[emotionLabels[sectorIndex]]!!
}

fun getEmotionFromKnob2Angle(
    angle: Float
): EmotionModel {
    // Normalize angle to 0-360 range
    var normalizedAngle = angle % 360f
    if (normalizedAngle < 0) normalizedAngle += 360f

    // Map to 0-180 range (45° to 225°, which is 3 emotions)
    val adjustedAngle = when {
        normalizedAngle >= 315f || normalizedAngle < 45f -> 0f  // Default to first emotion
        normalizedAngle >= 225f -> 180f  // Last sector
        else -> normalizedAngle - 45f
    }

    val sectorSize = 180f / 3  // 60 degrees per emotion

    return when {
        adjustedAngle < sectorSize -> anxietyKnobEmotions["Embarrassment"]!!
        adjustedAngle < sectorSize * 2 -> anxietyKnobEmotions["Love"]!!
        else -> anxietyKnobEmotions["Boredom"]!!
    }
}

fun getEmotionFromSliderValue(
    value: Float
): EmotionModel {
    return when {
        value >= 0.66f -> anxietyKnobEmotions["Joy"]!!
        value > 0.33f && value < 0.66f -> anxietyKnobEmotions["Sadness"]!!
        else -> anxietyKnobEmotions["Fear"]!!
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StellarEmotionsScreen(
    username: String,
    onNavigateToConstellations: () -> Unit,
    viewModel: StellarEmotionsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isAnalyzingEmotion by viewModel.isAnalyzingEmotion.collectAsState()

    // Estado para almacenar la URI de la foto temporal
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Función para crear un archivo temporal para la foto
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(null), "Pictures")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File(storageDir, "EMOTION_${timeStamp}.jpg")
    }

    // Launcher para tomar la foto
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = photoUri
        if (success && uri != null) {
            viewModel.onPhotoCaptured(uri)
            Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para solicitar permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, abrir cámara
            val imageFile = createImageFile()
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            photoUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    // Función para manejar el clic del botón de cámara
    fun onCameraButtonClick() {
        val permission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso ya concedido
                val imageFile = createImageFile()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                photoUri = uri
                takePictureLauncher.launch(uri)
            }
            else -> {
                // Solicitar permiso
                cameraPermissionLauncher.launch(permission)
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is StellarEmotionsViewModel.Event.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is StellarEmotionsViewModel.Event.ShowSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is StellarEmotionsViewModel.Event.ShowOfflineSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is StellarEmotionsViewModel.Event.NavigateNext -> {
                    // Navigate when the ViewModel tells us to
                    onNavigateToConstellations()
                }
                is StellarEmotionsViewModel.Event.EmotionDetected -> {
                    // Show detected emotion in a Toast
                    Toast.makeText(context, "Emotion detected: ${event.emotion}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    var selectedEmotions by remember {
        mutableStateOf(listOf<EmotionModel>())
    }

    var lastSelectedEmotion by remember {
        mutableStateOf<EmotionModel?>(null)
    }

    val initialKnob1Emotion = anxietyKnobEmotions["Anxiety"]!!
    var knob1State by remember {
        mutableStateOf(
            EmotionControlState(
                selectedEmotion = initialKnob1Emotion,
                angle = -45f
            )
        )
    }

    val initialKnob2Emotion = anxietyKnobEmotions["Embarrassment"]!!
    var knob2State by remember {
        mutableStateOf(
            EmotionControlState(
                selectedEmotion = initialKnob2Emotion,
                angle = -45f
            )
        )
    }
    val initialSliderEmotion = anxietyKnobEmotions["Joy"]!!
    var sliderState by remember {
        mutableStateOf(
            SliderEmotionControlState(
                selectedEmotion = initialSliderEmotion,
                value = 0.7f
            )
        )
    }

    val updateSelectedEmotionsAndLast: (EmotionModel, Boolean) -> Unit = { newEmotion, hasInteracted ->
        selectedEmotions = selectedEmotions.toMutableList().apply {
            val existingIndex = indexOfFirst { it.source == newEmotion.source }

            if (existingIndex != -1) {
                if (hasInteracted) {
                    this[existingIndex] = newEmotion
                } else {
                    removeAt(existingIndex) // Remove if user hasn't interacted
                }
            } else if (hasInteracted) {
                add(newEmotion)
            }
        }

        lastSelectedEmotion = newEmotion
    }

    var hasInteractedWithKnob1 by remember { mutableStateOf(false) }
    var hasInteractedWithKnob2 by remember { mutableStateOf(false) }
    var hasInteractedWithSlider by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        selectedEmotions = emptyList()
    }

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
                .padding(horizontal = 8.dp)

                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {

            OrbitSoundHeader(
                title = "Emotions",
                username = username,
                subtitle = "How are you feeling today captain?",
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    EmotionDisplay(
                        currentEmotion = lastSelectedEmotion ?: initialKnob1Emotion
                    )

                    EmotionList(
                        selectedEmotions = selectedEmotions
                    )


                    EmotionKnob(
                        labelTop = "Envy",
                        labelBottom = "Disgust",
                        labelLeft = "Anger",
                        labelRight = "Anxiety",
                        knobColor = if (hasInteractedWithKnob1) knob1State.selectedEmotion.color else knob1State.selectedEmotion.color.copy(alpha = 0.5f),
                        initialAngle = knob1State.angle,
                        onAngleChange = { newAngle ->
                            hasInteractedWithKnob1 = true
                            val newEmotion = getEmotionFromKnobAngle(
                                angle = newAngle,
                                emotionLabels = listOf("Anxiety", "Disgust", "Anger", "Envy")
                            )
                            knob1State = knob1State.copy(
                                selectedEmotion = newEmotion,
                                angle = newAngle
                            )
                            updateSelectedEmotionsAndLast(newEmotion, hasInteractedWithKnob1)
                        }
                    )

                    EmotionKnob(
                        labelTop = "Embarrassment",
                        labelBottom = "Love",
                        labelLeft = "Boredom",
                        knobColor = if (hasInteractedWithKnob2) knob2State.selectedEmotion.color else knob2State.selectedEmotion.color.copy(alpha = 0.5f),
                        initialAngle = knob2State.angle,
                        onAngleChange = { newAngle ->
                            hasInteractedWithKnob2 = true
                            val newEmotion = getEmotionFromKnob2Angle(newAngle)
                            knob2State = knob2State.copy(
                                selectedEmotion = newEmotion,
                                angle = newAngle
                            )
                            updateSelectedEmotionsAndLast(newEmotion, hasInteractedWithKnob2)
                        }
                    )

                    // Camera button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onCameraButtonClick() },
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = containerBorderColor
                            ),
                            enabled = !isAnalyzingEmotion
                        ) {
                            if (isAnalyzingEmotion) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.camera),
                                    contentDescription = "Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {

                    VolumeSlider(
                        sliderPosition = sliderState.value,
                        selectedEmotion = sliderState.selectedEmotion,
                        onValueChange = { newValue ->
                            hasInteractedWithSlider = true
                            val newEmotion = getEmotionFromSliderValue(newValue)
                            sliderState = sliderState.copy(
                                selectedEmotion = newEmotion,
                                value = newValue
                            )

                            updateSelectedEmotionsAndLast(newEmotion, hasInteractedWithSlider)
                        }
                    )
                }
            }

            Button(
                onClick = {

                    viewModel.onReadyToShipClicked(selectedEmotions)

                    if (uiState is StellarEmotionsViewModel.UiState.Success) {
                        onNavigateToConstellations()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .border(1.dp, containerBorderColor, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                when (uiState) {
                    StellarEmotionsViewModel.UiState.Loading -> {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> Text(text = "Ready to ship?", color = Color.White)
                }
            }
        }
    }
}@Composable
fun EmotionDisplay(currentEmotion: EmotionModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = currentEmotion.iconRes),
            contentDescription = currentEmotion.name,
            tint = currentEmotion.color,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = currentEmotion.description,
            color = currentEmotion.color,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            fontSize = 12.sp
        )
    }
}

@Composable
fun EmotionList(selectedEmotions: List<EmotionModel>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, containerBorderColor, containerShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        if (selectedEmotions.isNotEmpty()) {
            selectedEmotions.forEach { emotion ->

                Icon(
                    painter = painterResource(id = emotion.iconRes),
                    contentDescription = emotion.name,
                    tint = emotion.color,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {

            Text(
                text = "No emotions selected",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
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
    onAngleChange: ((Float) -> Unit)? = null
) {
    var currentRotation by remember { mutableStateOf(initialAngle) }

    // Update rotation when initialAngle changes
    LaunchedEffect(initialAngle) {
        currentRotation = initialAngle
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Labels
        Text(text = labelTop, color = Color.White, modifier = Modifier.align(Alignment.TopStart))
        Text(text = labelLeft, color = Color.White, modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(top = knobRadius * 2 + 24.dp))
        labelRight?.let {
            Text(text = it, color = Color.White, modifier = Modifier.align(Alignment.TopEnd))
        }
        Text(
            text = labelBottom,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = knobRadius * 2 + 24.dp)
        )

        // Knob with drag gesture
        Box(
            modifier = Modifier
                .size(knobRadius * 2)
                .padding(top = 24.dp)
                .pointerInput(Unit) {
                    if (onAngleChange != null) {
                        detectDragGestures { change, _ ->
                            change.consume()

                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchPos = change.position

                            // Calculate angle from center to touch position
                            val dx = touchPos.x - center.x
                            val dy = touchPos.y - center.y
                            var angle = Math
                                .toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                                .toFloat() + 90f

                            // Normalize to 0-360
                            if (angle < 0) angle += 360f

                            // Allow full 0-360° range so corners (esquinas) are correctly detected
                            currentRotation = angle
                            onAngleChange(angle)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Tick marks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val radius = size.minDimension / 2.0f
                for (i in 0 until 16) {
                    val angle = (i / 16f) * 360f
                    // Draw ticks around full circle (no skipping)
                    // If you prefer to skip top area, reintroduce a condition here.
                    // Keep them continuous so the 4 corner sectors are visually represented.
                    // if (angle < 45 || angle > 315) continue
                    val x = center.x + radius * cos(Math.toRadians(angle.toDouble() - 90)).toFloat()
                    val y = center.y + radius * sin(Math.toRadians(angle.toDouble() - 90)).toFloat()
                    drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
                }
            }

            // Rotating knob
            Box(
                modifier = Modifier
                    .fillMaxSize(0.8f)
                    .clip(CircleShape)
                    .background(knobColor)
                    .rotate(currentRotation)
            ) {
                // Indicator line
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.VolumeSlider(
    sliderPosition: Float,
    selectedEmotion: EmotionModel,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .border(1.dp, containerBorderColor, containerShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val containerHeight = maxHeight
            val containerWidth = maxWidth

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

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .width(containerHeight * 1.95f)
                        .height(containerWidth * 1.8f)
                        .rotate(-90f),
                    colors = SliderDefaults.colors(
                        thumbColor = selectedEmotion.color,
                        activeTrackColor = Color.LightGray.copy(alpha = 0.7f),
                        inactiveTrackColor = Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .rotate(90f)
                                .size(width = 24.dp, height = 40.dp)
                                .background(selectedEmotion.color, RoundedCornerShape(8.dp))
                        )
                    }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Text(text = "Joy", color = accentYellow, modifier = Modifier.align(Alignment.TopEnd).padding(top = 220.dp))
                Text(text = "Sadness", color = Color.White, modifier = Modifier.align(Alignment.CenterEnd))
                Text(text = "Fear", color = Color.White, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 220.dp))
            }
        }
        Text(
            text = "Emotion",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun StellarEmotionsScreenPreview() {
    OrbitSoundKotlinTheme {
        StellarEmotionsScreen(username = "User", onNavigateToConstellations = {})
    }
}
