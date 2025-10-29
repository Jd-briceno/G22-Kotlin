package com.g22.orbitsoundkotlin.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Service to upload images temporarily and get public URLs
 * Uses FreeImage.host API
 */
class ImageUploadService(private val context: Context) {

    private val TAG = "ImageUploadService"

    // FreeImage.host API key
    private val API_KEY = "6d207e02198a847aa98d0a2a901485a5"

    // FreeImage.host response data classes
    private data class SuccessInfo(
        val message: String?,
        val code: Int?
    )

    private data class ImageInfo(
        val url: String?,
        val display_url: String?,
        val url_viewer: String?
    )

    private data class FreeImageResponse(
        val status_code: Int?,
        val success: SuccessInfo?,
        val image: ImageInfo?,
        val status_txt: String?
    )

    /**
     * Upload image to FreeImage.host and get public URL
     * @param imageUri The URI of the image to upload
     * @return Public URL of the uploaded image or null if failed
     */
    suspend fun uploadImage(imageUri: Uri): String? {
        return try {
            withContext(Dispatchers.IO) {
                // Convert image to base64
                val base64Image = convertImageToBase64(imageUri)
                if (base64Image == null) {
                    Log.e(TAG, "Failed to convert image to base64")
                    return@withContext null
                }

                Log.d(TAG, "Uploading image to FreeImage.host...")

                val client = OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .build()

                // FreeImage.host upload with POST (required for base64)
                val requestBody = FormBody.Builder()
                    .add("key", API_KEY)
                    .add("action", "upload")
                    .add("source", base64Image)
                    .add("format", "json")
                    .build()

                val request = Request.Builder()
                    .url("https://freeimage.host/api/1/upload")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                try {
                    val responseBody = response.body?.string()

                    Log.d(TAG, "Response code: ${response.code}")
                    if (responseBody != null) {
                        Log.d(TAG, "Response body: $responseBody")
                    }

                    if (!response.isSuccessful) {
                        Log.e(TAG, "Upload failed with code: ${response.code}")
                        return@withContext null
                    }

                    if (responseBody == null) {
                        Log.e(TAG, "Response body is null")
                        return@withContext null
                    }

                    // Parse FreeImage.host response
                    val gson = Gson()
                    val freeImageResponse = gson.fromJson(responseBody, FreeImageResponse::class.java)

                    // Try to get URL from different possible fields
                    val imageUrl = freeImageResponse.image?.url
                        ?: freeImageResponse.image?.display_url

                    if (imageUrl != null && freeImageResponse.status_code == 200) {
                        Log.d(TAG, "Image uploaded successfully: $imageUrl")
                        return@withContext imageUrl
                    } else {
                        Log.e(TAG, "No URL in FreeImage.host response or upload failed")
                        Log.e(TAG, "Status: ${freeImageResponse.status_txt}, Code: ${freeImageResponse.status_code}")
                        return@withContext null
                    }
                } finally {
                    response.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading image: ${e.message}", e)
            null
        }
    }

    /**
     * Convert image URI to base64 string (without data URI prefix)
     */
    private fun convertImageToBase64(imageUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var bytesRead: Int

                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead)
                }

                val imageBytes = byteArrayOutputStream.toByteArray()
                // Return base64 without the data URI prefix
                Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to base64", e)
            null
        }
    }
}

