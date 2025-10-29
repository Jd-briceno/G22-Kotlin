package com.g22.orbitsoundkotlin.data

import android.content.Context
import android.net.Uri
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
 * Service to analyze facial emotions using Straico API
 */
class FacialEmotionAnalyzer(private val context: Context) {

    private val TAG = "FacialEmotionAnalyzer"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Data classes for Straico API
    private data class StraicoImageRequest(
        val models: List<String>,
        val message: String,
        val images: List<String>
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
     * Analyze facial emotion from image URI
     * @param imageUri The URI of the captured image
     * @return The detected emotion name or null if analysis failed
     */
    suspend fun analyzeEmotion(imageUri: Uri): String? {
        return try {
            withContext(Dispatchers.IO) {
                // Upload image to temporary hosting and get public URL
                val uploadService = ImageUploadService(context)
                val imageUrl = uploadService.uploadImage(imageUri)
                if (imageUrl == null) {
                    Log.e(TAG, "Failed to upload image")
                    return@withContext null
                }

                Log.d(TAG, "Image uploaded successfully: $imageUrl")

                // Prepare request
                val prompt = """
                    Analyze the facial expression in this image and determine the emotion the person is feeling.
                    You must respond with ONLY ONE WORD from this exact list: 
                    Envy, Anxiety, Anger, Disgust, Embarrassment, Boredom, Love, Joy, Sadness, Fear
                    
                    Respond with just the emotion name, nothing else.
                """.trimIndent()

                val straicoReq = StraicoImageRequest(
                    models = listOf("openai/gpt-4o-mini"),
                    message = prompt,
                    images = listOf(imageUrl)
                )

                val gson = Gson()
                val bodyString = gson.toJson(straicoReq)

                val apiKey = BuildConfig.STRAICO_API_KEY
                Log.d(TAG, "API Key present: ${apiKey.isNotEmpty()}")

                val client = OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                val reqBody = bodyString.toRequestBody(jsonMediaType)
                val req = Request.Builder()
                    .url("https://api.straico.com/v1/prompt/completion")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(reqBody)
                    .build()

                Log.d(TAG, "Sending request to Straico API...")
                val resp = client.newCall(req).execute()

                try {
                    Log.d(TAG, "Response code: ${resp.code}")
                    if (!resp.isSuccessful) {
                        val errorBody = resp.body?.string()
                        Log.e(TAG, "API call failed with code ${resp.code}: $errorBody")
                        return@withContext null
                    }

                    val respBody = resp.body?.string() ?: return@withContext null
                    Log.d(TAG, "Response body: $respBody")

                    try {
                        val straicoResp = gson.fromJson(respBody, StraicoResponse::class.java)
                        // Get the first completion from the completions map
                        val firstCompletion = straicoResp.data?.completions?.values?.firstOrNull()
                        val text = firstCompletion?.completion?.choices?.firstOrNull()?.message?.content

                        // Extract and validate emotion
                        val emotion = text?.trim()?.lines()?.firstOrNull()?.trim()
                        Log.d(TAG, "Detected emotion: $emotion")

                        // Validate that the emotion is one of the valid ones
                        val validEmotions = listOf(
                            "Envy", "Anxiety", "Anger", "Disgust",
                            "Embarrassment", "Boredom", "Love", "Joy", "Sadness", "Fear"
                        )

                        if (emotion != null && validEmotions.contains(emotion)) {
                            emotion
                        } else {
                            Log.e(TAG, "Invalid emotion received: $emotion")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        null
                    }
                } finally {
                    resp.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception analyzing emotion", e)
            null
        }
    }
}

