package com.g22.orbitsoundkotlin.services

import android.util.Base64
import android.util.Log
import com.g22.orbitsoundkotlin.BuildConfig
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.models.Playlist
import com.g22.orbitsoundkotlin.data.mappers.SpotifyTrackMapper
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

/**
 * Servicio para interactuar con la API de Spotify.
 * Implementa el patrón Singleton para reutilizar instancia y caché de tokens.
 */
class SpotifyService private constructor() {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val gson = Gson()
    private val trackMapper = SpotifyTrackMapper()
    
    init {
        Log.d(TAG, "SpotifyService inicializado (Singleton)")
        Log.d(TAG, "Client ID configurado: ${if (clientId.isNotEmpty()) "Sí" else "NO - FALTA CONFIGURAR"}")
    }
    
    companion object {
        private const val TAG = "SpotifyService"
        
        @Volatile
        private var instance: SpotifyService? = null
        
        /**
         * Obtiene la instancia única de SpotifyService (Singleton).
         * Thread-safe usando double-checked locking.
         */
        fun getInstance(): SpotifyService {
            return instance ?: synchronized(this) {
                instance ?: SpotifyService().also { instance = it }
            }
        }
    }
    
    // Caché de token para evitar solicitudes innecesarias
    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var tokenExpiry: Long = 0

    private val markets = listOf(
        "US", "GB", "DE", "JP", "KR", "MX", "BR", "FR", "ES", "IT",
        "CA", "AR", "AU", "CL", "CO", "NL", "SE", "NO", "FI", "DK",
        "PL", "PT", "IE", "NZ", "TR", "IL", "IN", "ID", "TH", "SG", "RU"
    )

    private val specialQueries = mapOf(
        "j-rock" to listOf("J-Rock", "Japanese Rock", "邦楽ロック"),
        "k-pop" to listOf("K-Pop", "케이팝", "Korean Pop"),
        "medieval" to listOf("Medieval", "Celtic", "Dungeons and dragons"),
        "anisong" to listOf("Anisong", "Anime", "Demon Slayer")
    )

    /**
     * Obtiene el access token de Spotify.
     * Reutiliza el token en caché si aún es válido (patrón Singleton).
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        // Reutilizar token en caché si aún es válido
        val currentTime = System.currentTimeMillis()
        if (cachedToken != null && currentTime < tokenExpiry) {
            Log.d(TAG, "Usando token en caché")
            return@withContext cachedToken
        }
        
        Log.d(TAG, "Solicitando nuevo token de acceso...")
        try {
            val credentials = Base64.encodeToString(
                "$clientId:$clientSecret".toByteArray(),
                Base64.NO_WRAP
            )

            val url = URL("https://accounts.spotify.com/api/token")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Basic $credentials")
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true

            val postData = "grant_type=client_credentials"
            connection.outputStream.use { it.write(postData.toByteArray()) }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code del token: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val token = jsonResponse.get("access_token")?.asString
                
                // Cachear el token (Spotify tokens duran 1 hora)
                cachedToken = token
                tokenExpiry = currentTime + 3600000 // 1 hora
                
                Log.d(TAG, "Token obtenido exitosamente")
                token
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Sin detalles"
                Log.e(TAG, "Error obteniendo token: $responseCode - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción obteniendo token", e)
            null
        }
    }

    suspend fun getGenrePlaylists(genre: String): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        val random = Random()

        val market = markets[random.nextInt(markets.size)]
        val queries = specialQueries[genre.lowercase()] ?: listOf(genre)

        for (query in queries) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=playlist&limit=10&market=$market")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                    val playlistsData = jsonResponse.getAsJsonObject("playlists")
                    val items = playlistsData?.getAsJsonArray("items")

                    items?.forEach { item ->
                        try {
                            val playlistObj = item.asJsonObject
                            val images = playlistObj.getAsJsonArray("images")
                            val coverUrl = images?.get(0)?.asJsonObject?.get("url")?.asString ?: ""

                            playlists.add(
                                Playlist(
                                    title = playlistObj.get("name")?.asString ?: "",
                                    cover = coverUrl,
                                    id = playlistObj.get("id")?.asString,
                                    description = playlistObj.get("description")?.asString,
                                    trackCount = playlistObj.getAsJsonObject("tracks")?.get("total")?.asInt
                                )
                            )
                        } catch (e: Exception) {
                            // Skip invalid playlist items
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip failed queries
            }
        }

        playlists.shuffle()
        playlists
    }

    suspend fun getPlaylistTracks(playlistId: String, market: String = "US"): List<Track> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()

        try {
            val url = URL("https://api.spotify.com/v1/playlists/$playlistId/tracks?market=$market&limit=100")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val items = jsonResponse.getAsJsonArray("items")

                val tracks = mutableListOf<Track>()
                items?.forEach { item ->
                    val trackObj = item.asJsonObject.getAsJsonObject("track")
                    if (trackObj != null && trackObj.get("id") != null) {
                        val track = trackMapper.map(trackObj.asMap())
                        if (track.title.isNotEmpty()) {
                            tracks.add(track)
                        }
                    }
                }

                tracks.shuffle()
                tracks.take(15)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchTracks(query: String, market: String = "US"): List<Track> = withContext(Dispatchers.IO) {
        Log.d(TAG, "searchTracks llamado con query: '$query'")
        
        val token = getAccessToken()
        if (token == null) {
            Log.e(TAG, "No se pudo obtener token de acceso")
            return@withContext emptyList()
        }

        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val apiUrl = "https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=15&market=$market"
            Log.d(TAG, "URL de búsqueda: $apiUrl")
            
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code de búsqueda: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                Log.d(TAG, "Respuesta recibida, tamaño: ${response.length} caracteres")
                
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val tracksData = jsonResponse.getAsJsonObject("tracks")
                val items = tracksData?.getAsJsonArray("items")
                
                Log.d(TAG, "Items encontrados en respuesta: ${items?.size() ?: 0}")

                val tracks = mutableListOf<Track>()
                items?.forEachIndexed { index, item ->
                    try {
                        val trackObj = item.asJsonObject
                        if (trackObj.get("id") != null) {
                            val track = trackMapper.map(trackObj.asMap())
                            tracks.add(track)
                            Log.d(TAG, "Track $index parseado: ${track.title} - ${track.artist}")
                        } else {
                            Log.w(TAG, "Track $index sin ID, omitido")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando track $index", e)
                    }
                }
                
                Log.d(TAG, "Total de tracks parseados exitosamente: ${tracks.size}")
                tracks
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Sin detalles"
                Log.e(TAG, "Error en búsqueda: $responseCode - $errorBody")
                
                if (responseCode == 400 && query.isNotEmpty()) {
                    Log.d(TAG, "Intentando búsqueda simplificada...")
                    return@withContext searchTracks(query.split(" ").first(), market)
                }
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en searchTracks", e)
            emptyList()
        }
    }

    suspend fun getFeaturedPlaylists(market: String = "US"): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()

        try {
            val url = URL("https://api.spotify.com/v1/search?q=trending%20playlist&type=playlist&limit=10&market=$market")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val playlistsData = jsonResponse.getAsJsonObject("playlists")
                val items = playlistsData?.getAsJsonArray("items")

                items?.forEach { item ->
                    try {
                        val playlistObj = item.asJsonObject
                        val images = playlistObj.getAsJsonArray("images")
                        val coverUrl = images?.get(0)?.asJsonObject?.get("url")?.asString ?: ""

                        playlists.add(
                            Playlist(
                                title = playlistObj.get("name")?.asString ?: "",
                                cover = coverUrl,
                                id = playlistObj.get("id")?.asString,
                                description = playlistObj.get("description")?.asString,
                                trackCount = playlistObj.getAsJsonObject("tracks")?.get("total")?.asInt
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid playlist items
                    }
                }
            }
        } catch (e: Exception) {
            // Skip on error
        }

        playlists
    }

    suspend fun getCategoryPlaylists(categoryId: String, market: String = "US"): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()

        try {
            val searchTerm = when (categoryId.lowercase()) {
                "pop" -> "pop%20playlist"
                "rock" -> "rock%20playlist"
                "jazz" -> "jazz%20playlist"
                "electronic" -> "electronic%20playlist"
                else -> "$categoryId%20playlist"
            }
            val url = URL("https://api.spotify.com/v1/search?q=$searchTerm&type=playlist&limit=10&market=$market")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val playlistsData = jsonResponse.getAsJsonObject("playlists")
                val items = playlistsData?.getAsJsonArray("items")

                items?.forEach { item ->
                    try {
                        val playlistObj = item.asJsonObject
                        val images = playlistObj.getAsJsonArray("images")
                        val coverUrl = images?.get(0)?.asJsonObject?.get("url")?.asString ?: ""

                        playlists.add(
                            Playlist(
                                title = playlistObj.get("name")?.asString ?: "",
                                cover = coverUrl,
                                id = playlistObj.get("id")?.asString,
                                description = playlistObj.get("description")?.asString,
                                trackCount = playlistObj.getAsJsonObject("tracks")?.get("total")?.asInt
                            )
                        )
                    } catch (e: Exception) {
                        // Skip invalid playlist items
                    }
                }
            }
        } catch (e: Exception) {
            // Skip on error
        }

        playlists
    }
}

private fun JsonObject.asMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    this.entrySet().forEach { entry ->
        when (val value = entry.value) {
            is com.google.gson.JsonPrimitive -> {
                map[entry.key] = when {
                    value.isString -> value.asString
                    value.isNumber -> value.asNumber
                    value.isBoolean -> value.asBoolean
                    else -> value.asString
                }
            }
            is com.google.gson.JsonObject -> map[entry.key] = value.asMap()
            is com.google.gson.JsonArray -> {
                map[entry.key] = value.map { element ->
                    when (element) {
                        is com.google.gson.JsonPrimitive -> {
                            when {
                                element.isString -> element.asString
                                element.isNumber -> element.asNumber
                                element.isBoolean -> element.asBoolean
                                else -> element.asString
                            }
                        }
                        is com.google.gson.JsonObject -> element.asMap()
                        else -> element.toString()
                    }
                }
            }
            else -> map[entry.key] = value.toString()
        }
    }
    return map
}
