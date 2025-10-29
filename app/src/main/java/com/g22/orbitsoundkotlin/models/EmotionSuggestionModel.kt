package com.g22.orbitsoundkotlin.models

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
 * Model responsible for emotion suggestion logic and Straico API communication
 */
class EmotionSuggestionModel {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Data classes for Straico API
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
     * Query Straico API for emotion suggestion based on user's most selected emotion
     */
    suspend fun getSuggestion(mostSelectedEmotion: String): String? {
        if (mostSelectedEmotion.isEmpty()) {
            Log.e("EmotionSuggestionModel", "mostSelectedEmotion is empty")
            return null
        }

        val prompt = "The user has been selecting the emotion: '$mostSelectedEmotion' repeatedly over the last week. " +
                "Given this, suggest one concise, user-friendly desired emotion they might choose next to improve balance or wellbeing. " +
                "Return only a single-word suggestion, out of the following options: Renewal, Power, Ambition, Serenity, Protection, Guidance."

        val straicoReq = StraicoRequest(
            models = listOf("openai/gpt-5-mini"),
            message = prompt
        )

        val gson = Gson()
        val bodyString = gson.toJson(straicoReq)

        val apiKey = BuildConfig.STRAICO_API_KEY
        Log.d("EmotionSuggestionModel", "API Key present: ${apiKey.isNotEmpty()}")

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
                    Log.d("EmotionSuggestionModel", "Response code: ${resp.code}")
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        Log.e("EmotionSuggestionModel", "API call failed with code ${resp.code}: $errorBody")
                        return@withContext null
                    }
                    val respBody = resp.body?.string() ?: return@withContext null
                    Log.d("EmotionSuggestionModel", "Response body: $respBody")
                    try {
                        val straicoResp = gson.fromJson(respBody, StraicoResponse::class.java)
                        // Get the first completion from the completions map
                        val firstCompletion = straicoResp.data?.completions?.values?.firstOrNull()
                        val text = firstCompletion?.completion?.choices?.firstOrNull()?.message?.content
                        // Trim and sanitize - get only the first line
                        val suggestion = text?.trim()?.lines()?.firstOrNull()
                        Log.d("EmotionSuggestionModel", "Extracted suggestion: $suggestion")
                        suggestion
                    } catch (e: Exception) {
                        Log.e("EmotionSuggestionModel", "Error parsing response", e)
                        null
                    }
                } finally {
                    resp.close()
                }
            }
        } catch (e: Exception) {
            Log.e("EmotionSuggestionModel", "Exception calling Straico API", e)
            null
        }
    }
}

