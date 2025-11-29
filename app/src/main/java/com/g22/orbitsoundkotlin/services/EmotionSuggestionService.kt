package com.g22.orbitsoundkotlin.services

import android.util.Log
import android.util.LruCache
import com.g22.orbitsoundkotlin.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Service Adapter para la API de Straico con caché LRU
 */
class EmotionSuggestionService private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: EmotionSuggestionService? = null

        fun getInstance(): EmotionSuggestionService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmotionSuggestionService().also { INSTANCE = it }
            }

        private const val TAG = "EmotionSuggestionService"
        private const val STRAICO_API_URL = "https://api.straico.com/v1/prompt/completion"
        private const val DEFAULT_MODEL = "openai/gpt-5-mini"
        private const val REQUEST_TIMEOUT_SECONDS = 30L
        private const val CACHE_SIZE = 20 // Máximo 20 sugerencias en caché
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // LruCache para almacenar sugerencias: Key = emoción, Value = sugerencia
    private val suggestionCache = LruCache<String, String>(CACHE_SIZE)

    // Data classes para el contrato de la API de Straico
    private data class StraicoRequest(
        val models: List<String>,
        val message: String
    )

    private data class Message(
        val content: String?
    )

    private data class CompletionChoice(
        val message: Message?
    )

    private data class CompletionInfo(
        val choices: List<CompletionChoice>?
    )

    private data class CompletionData(
        val completion: CompletionInfo?
    )

    private data class StraicoResponseData(
        val completions: Map<String, CompletionData>?
    )

    private data class StraicoResponse(
        val data: StraicoResponseData?
    )

    /**
     * Obtiene una sugerencia de emoción basada en la emoción más seleccionada por el usuario
     * Primero verifica el caché, si no hay conexión o falla la API, retorna valor cacheado
     *
     * @param mostSelectedEmotion La emoción que el usuario ha seleccionado más frecuentemente
     * @return Una sugerencia de emoción (Renewal, Power, Ambition, Serenity, Protection, Guidance) o null si falla
     */
    suspend fun getSuggestion(mostSelectedEmotion: String): String? {
        if (mostSelectedEmotion.isEmpty()) {
            Log.e(TAG, "mostSelectedEmotion is empty")
            return null
        }

        // Normalizar la clave para el caché
        val cacheKey = mostSelectedEmotion.trim().lowercase()

        // Verificar si hay una sugerencia en caché
        val cachedSuggestion = suggestionCache.get(cacheKey)
        if (cachedSuggestion != null) {
            Log.d(TAG, "Returning cached suggestion for '$mostSelectedEmotion': $cachedSuggestion")
            return cachedSuggestion
        }

        // Si no hay en caché, hacer la llamada a la API
        val prompt = buildPrompt(mostSelectedEmotion)
        val suggestion = executeApiCall(prompt)

        // Si la llamada fue exitosa, guardar en caché
        if (suggestion != null) {
            suggestionCache.put(cacheKey, suggestion)
            Log.d(TAG, "Cached new suggestion for '$mostSelectedEmotion': $suggestion")
        }

        return suggestion
    }

    /**
     * Limpia el caché de sugerencias
     */
    fun clearCache() {
        suggestionCache.evictAll()
        Log.d(TAG, "Suggestion cache cleared")
    }

    /**
     * Obtiene el tamaño actual del caché
     */
    fun getCacheSize(): Int {
        return suggestionCache.size()
    }

    /**
     * Construye el prompt para la API de IA
     */
    private fun buildPrompt(emotion: String): String {
        return "The user has been selecting the emotion: '$emotion' repeatedly over the last week. " +
                "Given this, suggest one concise, user-friendly desired emotion they might choose next to improve balance or wellbeing. " +
                "Return only a single-word suggestion, out of the following options: Renewal, Power, Ambition, Serenity, Protection, Guidance."
    }

    /**
     * Ejecuta la llamada a la API de Straico
     */
    private suspend fun executeApiCall(prompt: String): String? {
        val apiKey = BuildConfig.STRAICO_API_KEY

        if (apiKey.isEmpty()) {
            Log.e(TAG, "API Key is not configured")
            return null
        }

        Log.d(TAG, "API Key present: ${apiKey.isNotEmpty()}")

        return try {
            withContext(Dispatchers.IO) {
                val requestBody = buildRequestBody(prompt)
                val request = buildRequest(requestBody, apiKey)

                val response = client.newCall(request).execute()

                response.use { resp ->
                    Log.d(TAG, "Response code: ${resp.code}")

                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        Log.e(TAG, "API call failed with code ${resp.code}: $errorBody")
                        return@withContext null
                    }

                    val responseBody = resp.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body")
                        return@withContext null
                    }

                    Log.d(TAG, "Response body: $responseBody")
                    parseResponse(responseBody)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Straico API", e)
            null
        }
    }

    /**
     * Construye el cuerpo de la petición
     */
    private fun buildRequestBody(prompt: String): String {
        val straicoRequest = StraicoRequest(
            models = listOf(DEFAULT_MODEL),
            message = prompt
        )
        return gson.toJson(straicoRequest)
    }

    /**
     * Construye la petición HTTP
     */
    private fun buildRequest(bodyString: String, apiKey: String): Request {
        val requestBody = bodyString.toRequestBody(jsonMediaType)

        return Request.Builder()
            .url(STRAICO_API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parsea la respuesta de la API
     */
    private fun parseResponse(responseBody: String): String? {
        return try {
            val straicoResponse = gson.fromJson(responseBody, StraicoResponse::class.java)

            // Obtiene la primera completion del mapa de completions
            val firstCompletion = straicoResponse.data?.completions?.values?.firstOrNull()
            val text = firstCompletion?.completion?.choices?.firstOrNull()?.message?.content

            // Limpia y sanitiza - obtiene solo la primera línea
            val suggestion = text?.trim()?.lines()?.firstOrNull()

            Log.d(TAG, "Extracted suggestion: $suggestion")
            suggestion
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            null
        }
    }

    /**
     * Método opcional para configurar un cliente personalizado (útil para testing)
     */
    fun setCustomClient(customClient: OkHttpClient) {
        // En una implementación más completa, esto permitiría inyectar un cliente mock
        // Por ahora lo dejamos como placeholder para futuras mejoras
    }
}

