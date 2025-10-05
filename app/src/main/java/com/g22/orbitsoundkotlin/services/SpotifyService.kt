package com.g22.orbitsoundkotlin.services

import android.util.Base64
import com.g22.orbitsoundkotlin.BuildConfig
import com.g22.orbitsoundkotlin.models.Track
import com.g22.orbitsoundkotlin.models.Playlist
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Random

class SpotifyService {
    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val gson = Gson()

    // 🌎 Lista de mercados (random para variar resultados)
    private val markets = listOf(
        "US", "GB", "DE", "JP", "KR", "MX", "BR", "FR", "ES", "IT",
        "CA", "AR", "AU", "CL", "CO", "NL", "SE", "NO", "FI", "DK",
        "PL", "PT", "IE", "NZ", "TR", "IL", "IN", "ID", "TH", "SG", "RU"
    )

    // 🎯 Queries especiales por género
    private val specialQueries = mapOf(
        "j-rock" to listOf("J-Rock", "Japanese Rock", "邦楽ロック"),
        "k-pop" to listOf("K-Pop", "케이팝", "Korean Pop"),
        "medieval" to listOf("Medieval", "Celtic", "Dungeons and dragons"),
        "anisong" to listOf("Anisong", "Anime", "Demon Slayer")
    )

    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            println("🔑 Getting access token...")
            println("🔑 Client ID: ${if (clientId == "YOUR_SPOTIFY_CLIENT_ID") "NOT SET" else "SET"}")
            println("🔑 Client Secret: ${if (clientSecret == "YOUR_SPOTIFY_CLIENT_SECRET") "NOT SET" else "SET"}")
            
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
            println("📡 Token response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val token = jsonResponse.get("access_token")?.asString
                println("✅ Token obtained successfully")
                token
            } else {
                println("❌ Error getting token: $responseCode")
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                println("❌ Token error response: $errorResponse")
                null
            }
        } catch (e: Exception) {
            println("❌ Exception getting token: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun getGenrePlaylists(genre: String): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()
        val random = Random()

        // 🎲 Elegir mercado aleatorio
        val market = markets[random.nextInt(markets.size)]

        // ✅ Obtener queries especiales si existen, sino usamos el género literal
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
                            println("❌ Error parsing playlist item: ${e.message}")
                        }
                    }
                } else {
                    println("❌ Error searching playlists for $query in $market: $responseCode")
                }
            } catch (e: Exception) {
                println("❌ Exception searching playlists: ${e.message}")
            }
        }

        // 🔀 Mezclar resultados
        playlists.shuffle()
        println("🔎 Found ${playlists.size} playlists for $genre in $market")
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
                        val track = Track.fromSpotify(trackObj.asMap())
                        if (track.title.isNotEmpty()) {
                            tracks.add(track)
                        }
                    }
                }

                // 🔀 Mezclar y limitar a 15
                tracks.shuffle()
                tracks.take(15)
            } else {
                println("❌ Error fetching tracks: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Exception fetching tracks: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchTracks(query: String, market: String = "US"): List<Track> = withContext(Dispatchers.IO) {
        println("🔍 Searching tracks for: '$query' in market: $market")
        
        val token = getAccessToken()
        if (token == null) {
            println("❌ No access token available")
            return@withContext emptyList()
        }
        println("✅ Access token obtained")

        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.spotify.com/v1/search?q=$encodedQuery&type=track&limit=15&market=$market")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")

            val responseCode = connection.responseCode
            println("📡 Response code: $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val tracksData = jsonResponse.getAsJsonObject("tracks")
                val items = tracksData?.getAsJsonArray("items")

                println("📊 Found ${items?.size() ?: 0} tracks")

                val tracks = mutableListOf<Track>()
                items?.forEach { item ->
                    val trackObj = item.asJsonObject
                    if (trackObj.get("id") != null) {
                        println("🔍 Raw track data: ${trackObj}")
                        // Parsear directamente con JsonObject
                        val title = trackObj.get("name")?.asString ?: "Unknown"
                        val artists = trackObj.getAsJsonArray("artists")?.map { it.asJsonObject.get("name")?.asString ?: "" }?.joinToString(", ") ?: "Unknown"
                        val durationMs = trackObj.get("duration_ms")?.asInt ?: 0
                        val minutes = durationMs / 60000
                        val seconds = (durationMs % 60000) / 1000
                        val duration = String.format("%d:%02d", minutes, seconds)
                        val album = trackObj.getAsJsonObject("album")
                        val images = album?.getAsJsonArray("images")
                        val albumArt = images?.get(0)?.asJsonObject?.get("url")?.asString ?: ""

                        val track = Track(title, artists, duration, durationMs, albumArt)
                        println("🎵 Track: ${track.title} by ${track.artist} - Image: ${track.albumArt}")
                        tracks.add(track)
                    }
                }
                println("✅ Returning ${tracks.size} tracks")
                tracks
            } else {
                println("❌ Error searching tracks: $responseCode")
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                println("❌ Error response: $errorResponse")
                
                // Si es error 400, intentar con una búsqueda más simple
                if (responseCode == 400 && query.isNotEmpty()) {
                    println("🔄 Retrying with simplified query...")
                    return@withContext searchTracks(query.split(" ").first(), market)
                }
                
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Exception searching tracks: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // 🎵 Obtener playlists destacadas
    suspend fun getFeaturedPlaylists(market: String = "US"): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()

        try {
            // Usar search con términos más específicos para obtener playlists destacadas
            val url = URL("https://api.spotify.com/v1/search?q=trending%20playlist&type=playlist&limit=10&market=$market")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")

            val responseCode = connection.responseCode
            println("🔎 Featured playlists response: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val playlistsData = jsonResponse.getAsJsonObject("playlists")
                val items = playlistsData?.getAsJsonArray("items")

                println("🔎 Found ${items?.size() ?: 0} featured playlists")

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
                        println("❌ Error parsing featured playlist item: ${e.message}")
                    }
                }
            } else {
                println("❌ Error getting featured playlists: $responseCode")
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                println("❌ Error response: $errorResponse")
            }
        } catch (e: Exception) {
            println("❌ Exception getting featured playlists: ${e.message}")
            e.printStackTrace()
        }

        playlists
    }

    // 🎵 Obtener playlists por categoría
    suspend fun getCategoryPlaylists(categoryId: String, market: String = "US"): List<Playlist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val playlists = mutableListOf<Playlist>()

        try {
            // Usar search con términos más específicos para obtener playlists de la categoría
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
            println("🔎 Category playlists response for $categoryId: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = gson.fromJson(response, JsonObject::class.java)
                val playlistsData = jsonResponse.getAsJsonObject("playlists")
                val items = playlistsData?.getAsJsonArray("items")

                println("🔎 Found ${items?.size() ?: 0} category playlists for $categoryId")

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
                        println("❌ Error parsing category playlist item: ${e.message}")
                    }
                }
            } else {
                println("❌ Error getting category playlists: $responseCode")
                val errorResponse = connection.errorStream?.bufferedReader()?.readText()
                println("❌ Error response: $errorResponse")
            }
        } catch (e: Exception) {
            println("❌ Exception getting category playlists: ${e.message}")
            e.printStackTrace()
        }

        playlists
    }
}

// Extensión para convertir JsonObject a Map
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
