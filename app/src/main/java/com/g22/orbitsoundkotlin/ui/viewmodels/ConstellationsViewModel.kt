package com.g22.orbitsoundkotlin.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.repositories.ConstellationLogRepository
import com.g22.orbitsoundkotlin.services.EmotionSuggestionService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * ViewModel for ConstellationsScreen
 * Manages state and business logic for emotion selection and suggestions
 */
class ConstellationsViewModel : ViewModel() {

    private val emotionSuggestionService = EmotionSuggestionService.getInstance()
    private val constellationLogRepository = ConstellationLogRepository()

    // State for selected constellation
    private val _selectedConstellation = MutableStateFlow<Constellation?>(null)
    val selectedConstellation: StateFlow<Constellation?> = _selectedConstellation.asStateFlow()

    // State for most selected emotion
    private val _mostSelectedEmotion = MutableStateFlow<String?>(null)
    val mostSelectedEmotion: StateFlow<String?> = _mostSelectedEmotion.asStateFlow()

    // State for loading
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // State for AI suggestion
    private val _suggestion = MutableStateFlow<String?>(null)
    val suggestion: StateFlow<String?> = _suggestion.asStateFlow()

    // State for suggestion loading
    private val _suggestionLoading = MutableStateFlow(false)
    val suggestionLoading: StateFlow<Boolean> = _suggestionLoading.asStateFlow()

    // Flag to track if suggestion has been emitted
    private var suggestionEmitted = false

    init {
        loadMostSelectedEmotion()
    }

    /**
     * Load the most selected emotion from Firebase for the last 7 days
     */
    private fun loadMostSelectedEmotion() {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                _isLoading.value = false
                _mostSelectedEmotion.value = null
                Log.w("ConstellationsViewModel", "No user logged in")
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

                    val mostSelected = counts.maxByOrNull { it.value }?.key
                    _mostSelectedEmotion.value = mostSelected
                    _isLoading.value = false

                    Log.d("ConstellationsViewModel", "Most selected emotion: $mostSelected")
                    Log.d("ConstellationsViewModel", "Emotion counts: $counts")

                    // Request AI suggestion if we have a most selected emotion
                    if (mostSelected != null) {
                        requestSuggestion(mostSelected)
                    } else {
                        Log.w("ConstellationsViewModel", "No most selected emotion found")
                    }
                }
                .addOnFailureListener { exception ->
                    _isLoading.value = false
                    Log.e("ConstellationsViewModel", "Failed to load emotion logs", exception)
                }
        }
    }

    /**
     * Request an emotion suggestion from the AI model
     * Utiliza caché automático del servicio EmotionSuggestionService
     */
    private fun requestSuggestion(emotion: String) {
        _suggestionLoading.value = true
        viewModelScope.launch {
            Log.d("ConstellationsViewModel", "Starting AI suggestion query...")
            val result = try {
                emotionSuggestionService.getSuggestion(emotion)
            } catch (e: Exception) {
                Log.e("ConstellationsViewModel", "Error calling suggestion API", e)
                null
            }
            Log.d("ConstellationsViewModel", "AI suggestion result: $result")
            _suggestion.value = result
            _suggestionLoading.value = false


            if (!suggestionEmitted && result != null) {
                Log.d("ConstellationsViewModel", "Suggestion ready: $result")
                suggestionEmitted = true
            } else if (result == null) {
                Log.w("ConstellationsViewModel", "No valid suggestion received from AI - may be using cached data")
            }
        }
    }

    /**
     * Set the selected constellation
     */
    fun selectConstellation(constellation: Constellation) {
        _selectedConstellation.value = constellation
        Log.d("ConstellationsViewModel", "Selected constellation: ${constellation.id} (${constellation.title})")

        // Log the selection to Firebase
        viewModelScope.launch {
            val success = constellationLogRepository.logConstellationSelection(constellation)
            if (success) {
                Log.d("ConstellationsViewModel", "Constellation selection logged successfully")
            } else {
                Log.w("ConstellationsViewModel", "Failed to log constellation selection")
            }
        }
    }

    /**
     * Clear the selected constellation
     */
    fun clearSelection() {
        _selectedConstellation.value = null
    }

    /**
     * Auto-select a constellation based on AI suggestion
     */
    fun autoSelectFromSuggestion(constellations: List<Constellation>): Constellation? {
        val suggestedEmotion = _suggestion.value ?: return null
        val normalized = suggestedEmotion.trim().lowercase()

        // Map emotion labels to constellation indices
        val matched = when (normalized) {
            "renewal" -> constellations.getOrNull(0)      // Phoenix
            "power" -> constellations.getOrNull(1)        // Draco
            "ambition" -> constellations.getOrNull(2)     // Pegasus
            "serenity" -> constellations.getOrNull(3)     // Cygnus
            "protection" -> constellations.getOrNull(4)   // Ursa Major
            "guidance" -> constellations.getOrNull(5)     // Crux
            else -> null
        }

        if (matched != null) {
            _selectedConstellation.value = matched
            Log.d("ConstellationsViewModel", "Auto-selected constellation: ${matched.id} (${matched.title})")

            // Log the auto-selection to Firebase
            viewModelScope.launch {
                val success = constellationLogRepository.logConstellationSelection(matched)
                if (success) {
                    Log.d("ConstellationsViewModel", "Auto-selected constellation logged successfully")
                } else {
                    Log.w("ConstellationsViewModel", "Failed to log auto-selected constellation")
                }
            }
        } else {
            Log.w("ConstellationsViewModel", "No constellation matched for suggestion: $suggestedEmotion")
        }

        return matched
    }

    /**
     * Helper to parse different timestamp formats
     */
    private fun tryParseDateString(value: Any?): Date? {
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
}
