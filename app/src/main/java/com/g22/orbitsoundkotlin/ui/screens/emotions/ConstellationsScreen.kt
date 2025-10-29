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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.ui.screens.home.StarField
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme

// New imports for Firebase and date parsing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.g22.orbitsoundkotlin.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log

@Composable
fun ConstellationsScreen(username: String, onNavigateToHome: () -> Unit = {}) {

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

    val constellations = remember {
        listOf(
            Constellation("fenix", "Phoenix", "From my own ashes, I carry the fire that will guide me home.", "Renewal, courage after loss, and embracing change.", R.drawable.fenix),
            Constellation("draco", "Draco", "My power is ancient, a force of nature that commands respect.", "Wisdom, authority, and untamed strength.", R.drawable.draco),
            Constellation("pegasus", "Pegasus", "With wings of starlight, I soar towards my highest aspirations.", "Inspiration, creativity, and boundless ambition.", R.drawable.pegasus),
            Constellation("cisne", "Cygnus", "In stillness, I find my grace and the quiet strength to glide on.", "Peace, elegance, and profound serenity.", R.drawable.cisne),
            Constellation("ursa_mayor", "Ursa Major", "I am the steadfast guardian, a shield against the darkness.", "Strength, family, and unwavering protection.", R.drawable.ursa_mayor),
            Constellation("cruz", "Crux", "My light is a beacon, a fixed point in the ever-changing cosmos.", "Faith, direction, and unerring guidance.", R.drawable.cruz)
        )
    }

    var selectedConstellation by remember { mutableStateOf<Constellation?>(null) }
    val context = LocalContext.current

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
            MostSelectedEmotionLabel(onSuggestion = { suggestedEmotion ->
                // Do not override a manual selection. Autoselect only if none chosen yet.
                if (suggestedEmotion == null) return@MostSelectedEmotionLabel
                if (selectedConstellation != null) return@MostSelectedEmotionLabel

                when {
                    suggestedEmotion.equals("Renewal", ignoreCase = true) -> selectedConstellation = constellations[0]
                    suggestedEmotion.equals("Power", ignoreCase = true) -> selectedConstellation = constellations[1]
                    suggestedEmotion.equals("Ambition", ignoreCase = true) -> selectedConstellation = constellations[2]
                    suggestedEmotion.equals("Serenity", ignoreCase = true) -> selectedConstellation = constellations[3]
                    suggestedEmotion.equals("Protection", ignoreCase = true) -> selectedConstellation = constellations[4]
                    suggestedEmotion.equals("Guidance", ignoreCase = true) -> selectedConstellation = constellations[5]
                    else -> {
                        // try to match by id or title if Straico returned something slightly different
                        val byId = constellations.find { it.id.equals(suggestedEmotion, ignoreCase = true) }
                        val byTitle = constellations.find { it.title.equals(suggestedEmotion, ignoreCase = true) }
                        selectedConstellation = byId ?: byTitle
                    }
                }
            })

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
                    ConstellationButton(drawableId = R.drawable.fenix, label = "Renewal", isSelected = selectedConstellation?.id == constellations[0].id, onClick = { selectedConstellation = constellations[0] })
                    ConstellationButton(drawableId = R.drawable.draco, label = "Power", isSelected = selectedConstellation?.id == constellations[1].id, onClick = { selectedConstellation = constellations[1] })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    ConstellationButton(drawableId = R.drawable.pegasus, label = "Ambition", isSelected = selectedConstellation?.id == constellations[2].id, onClick = { selectedConstellation = constellations[2] })
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
                    ConstellationButton(drawableId = R.drawable.cisne, label = "Serenity", isSelected = selectedConstellation?.id == constellations[3].id, onClick = { selectedConstellation = constellations[3] })
                    ConstellationButton(drawableId = R.drawable.ursa_mayor, label = "Protection", isSelected = selectedConstellation?.id == constellations[4].id, onClick = { selectedConstellation = constellations[4] })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    ConstellationButton(drawableId = R.drawable.cruz, label = "Guidance", isSelected = selectedConstellation?.id == constellations[5].id, onClick = { selectedConstellation = constellations[5] })
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


// New helper: attempts to parse different timestamp formats. Tries Firestore Timestamp first, then some string patterns.
fun tryParseDateString(value: Any?): Date? {
    if (value == null) return null
    if (value is Date) return value
    if (value is Timestamp) return value.toDate()
    if (value is String) {
        var s = value
        // Normalize common Spanish am/pm and UTC token so SimpleDateFormat can parse
        s = s.replace("a.m.", "AM").replace("p.m.", "PM").replace("UTC", "GMT")
        val patterns = listOf(
            "dd 'de' MMMM 'de' yyyy, hh:mm:ss a z",
            "dd 'de' MMMM 'de' yyyy, HH:mm:ss z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd HH:mm:ss"
        )
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.forLanguageTag("es"))
                return fmt.parse(s)
            } catch (_: Exception) {
                // ignore and try next
            }
        }
    }
    return null
}

// --- Straico API client helper ---
private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

suspend fun queryStraicoSuggestion(mostSelectedEmotion: String?): String? {
    if (mostSelectedEmotion == null) {
        Log.e("StraicoAPI", "mostSelectedEmotion is null")
        return null
    }

    // Build request payload for Straico API
    data class StraicoRequest(
        val models: List<String>,
        val message: String
    )

    data class Message(
        val content: String?
    )

    data class CompletionChoice(
        val message: Message?
    )

    data class CompletionInfo(
        val choices: List<CompletionChoice>?
    )

    data class CompletionData(
        val completion: CompletionInfo?
    )

    data class StraicoResponseData(
        val completions: Map<String, CompletionData>?
    )

    data class StraicoResponse(
        val data: StraicoResponseData?
    )

    val prompt = "The user has been selecting the emotion: '$mostSelectedEmotion' repeatedly over the last week. " +
            "Given this, suggest one concise, user-friendly desired emotion they might choose next to improve balance or wellbeing. " +
            "Return only a single-word suggestion, out of the following options: Renewal, Power, Ambition, Serenity, Protection, Guidance."

    val straicoReq = StraicoRequest(
        models = listOf("anthropic/claude-3.5-sonnet"),
        message = prompt
    )

    val gson = Gson()
    val bodyString = gson.toJson(straicoReq)

    val apiKey = BuildConfig.STRAICO_API_KEY
    Log.d("StraicoAPI", "API Key present: ${apiKey.isNotEmpty()}")

    return try {
        withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val reqBody = bodyString.toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("https://api.straico.com/v1/prompt/completion")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(reqBody)
                .build()

            val resp = client.newCall(req).execute()
            try {
                Log.d("StraicoAPI", "Response code: ${resp.code}")
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    Log.e("StraicoAPI", "API call failed with code ${resp.code}: $errorBody")
                    return@withContext null
                }
                val respBody = resp.body?.string() ?: return@withContext null
                Log.d("StraicoAPI", "Response body: $respBody")
                try {
                    val straicoResp = gson.fromJson(respBody, StraicoResponse::class.java)
                    // Get the first completion from the completions map
                    val firstCompletion = straicoResp.data?.completions?.values?.firstOrNull()
                    val text = firstCompletion?.completion?.choices?.firstOrNull()?.message?.content
                    // Trim and sanitize - get only the first line
                    val suggestion = text?.trim()?.lines()?.firstOrNull()
                    Log.d("StraicoAPI", "Extracted suggestion: $suggestion")
                    suggestion
                } catch (e: Exception) {
                    Log.e("StraicoAPI", "Error parsing response", e)
                    null
                }
            } finally {
                resp.close()
            }
        }
    } catch (e: Exception) {
        Log.e("StraicoAPI", "Exception calling Straico API", e)
        null
    }
}


@Composable
fun MostSelectedEmotionLabel(onSuggestion: (String?) -> Unit = {}) {
    var mostSelected by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var suggestion by remember { mutableStateOf<String?>(null) }
    var suggestionLoading by remember { mutableStateOf(false) }
    // ensure we only emit suggestion once to avoid reselect loops
    var suggestionEmitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                // no user logged in
                loading = false
                mostSelected = null
                return@launch
            }

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).collection("emotionLogs")
                .get()
                .addOnSuccessListener { snapshot ->
                    val counts = mutableMapOf<String, Int>()
                    val now = Date()
                    val weekAgo = Date(now.time - TimeUnit.DAYS.toMillis(7))

                    for (doc in snapshot.documents) {
                        val rawTs = doc.get("timestamp")
                        val docDate = tryParseDateString(rawTs)

                        // If we couldn't parse the timestamp, try Firestore's getDate API
                        val fallbackDate = doc.getDate("timestamp")
                        val dateToCheck = docDate ?: fallbackDate

                        if (dateToCheck == null) {
                            // If still null, skip this log
                            continue
                        }

                        if (dateToCheck.before(weekAgo)) continue

                        val emotions = doc.get("emotions") as? List<*>
                        emotions?.forEach { e ->
                            val map = e as? Map<*, *>
                            val name = (map?.get("name") ?: map?.get("id")) as? String ?: return@forEach
                            counts[name] = counts.getOrDefault(name, 0) + 1
                        }
                    }

                    mostSelected = counts.maxByOrNull { it.value }?.key
                    loading = false

                    Log.d("ConstellationsScreen", "Most selected emotion: $mostSelected")
                    Log.d("ConstellationsScreen", "Emotion counts: $counts")

                    // Now call Straico to get a suggested desired emotion (fire-and-forget using coroutine)
                    if (mostSelected != null) {
                        suggestionLoading = true
                        scope.launch {
                            Log.d("ConstellationsScreen", "Starting Straico query...")
                            val result = try {
                                queryStraicoSuggestion(mostSelected)
                            } catch (e: Exception) {
                                Log.e("ConstellationsScreen", "Error calling Straico", e)
                                null
                            }
                            Log.d("ConstellationsScreen", "Straico result: $result")
                            suggestion = result
                            suggestionLoading = false
                            // fire suggestion callback if available and not emitted yet
                            if (!suggestionEmitted) {
                                // prefer Straico suggestion, fallback to mostSelected
                                onSuggestion(suggestion ?: mostSelected)
                                suggestionEmitted = true
                            }
                        }
                    } else {
                        Log.w("ConstellationsScreen", "No most selected emotion found, skipping Straico query")
                    }
                }
                .addOnFailureListener {
                    loading = false
                }
        }
    }

    // If Straico didn't return anything but we have a mostSelected after loading, emit once
    LaunchedEffect(loading, suggestion, mostSelected) {
        if (!loading && !suggestionEmitted) {
            if (suggestion != null || mostSelected != null) {
                onSuggestion(suggestion ?: mostSelected)
                suggestionEmitted = true
            }
        }
    }

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
