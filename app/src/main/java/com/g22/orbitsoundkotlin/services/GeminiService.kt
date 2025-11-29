package com.g22.orbitsoundkotlin.services

import android.util.Log
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
 * Service Adapter para la API de Gemini (Google)
 * Genera queries de búsqueda musical basadas en el input emocional del usuario
 */
class GeminiService private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: GeminiService? = null

        fun getInstance(): GeminiService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeminiService().also { INSTANCE = it }
            }

        private const val TAG = "GeminiService"
        private const val GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
        private const val REQUEST_TIMEOUT_SECONDS = 30L
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // Data classes para el contrato de la API de Gemini
    private data class TextPart(
        val text: String
    )

    private data class Content(
        val parts: List<TextPart>
    )

    private data class GeminiRequest(
        val contents: List<Content>
    )

    private data class ContentPart(
        val text: String?
    )

    private data class CandidateContent(
        val parts: List<ContentPart>?
    )

    private data class Candidate(
        val content: CandidateContent?
    )

    private data class GeminiResponse(
        val candidates: List<Candidate>?
    )

    /**
     * Genera contenido usando Gemini API basado en un prompt customizado
     *
     * @param prompt El prompt completo a enviar a Gemini
     * @return Texto de respuesta de Gemini o null si falla
     */
    suspend fun generateContent(prompt: String): String? {
        if (prompt.isBlank()) {
            Log.e(TAG, "prompt is empty")
            return null
        }

        return executeApiCall(prompt)
    }

    /**
     * Ejecuta la llamada a la API de Gemini
     */
    private suspend fun executeApiCall(prompt: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not configured")
            return null
        }

        Log.d(TAG, "API Key present: ${apiKey.isNotEmpty()}")

        return try {
            withContext(Dispatchers.IO) {
                val requestBody = buildRequestBody(prompt)
                val url = "$GEMINI_API_BASE_URL?key=$apiKey"
                val request = buildRequest(requestBody, url)

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
            Log.e(TAG, "Exception calling Gemini API", e)
            null
        }
    }

    /**
     * Construye el cuerpo de la petición
     */
    private fun buildRequestBody(prompt: String): String {
        val geminiRequest = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(TextPart(text = prompt))
                )
            )
        )
        return gson.toJson(geminiRequest)
    }

    /**
     * Construye la petición HTTP
     */
    private fun buildRequest(bodyString: String, url: String): Request {
        val requestBody = bodyString.toRequestBody(jsonMediaType)

        return Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()
    }

    /**
     * Parsea la respuesta de la API y extrae el texto
     */
    private fun parseResponse(responseBody: String): String? {
        return try {
            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)

            // Obtiene el texto de la primera candidata
            val text = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (text.isNullOrBlank()) {
                Log.e(TAG, "No text found in response")
                return null
            }

            Log.d(TAG, "Extracted text: $text")
            text.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            null
        }
    }
}

