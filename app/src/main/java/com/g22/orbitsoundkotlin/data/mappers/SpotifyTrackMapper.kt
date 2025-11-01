package com.g22.orbitsoundkotlin.data.mappers

import android.util.Log
import com.g22.orbitsoundkotlin.models.Track
import com.google.gson.JsonObject

/**
 * Mapper para convertir respuesta JSON de Spotify API a modelo Track.
 * Implementa el patrón Mapper para separar la lógica de parseo del modelo de dominio.
 */
class SpotifyTrackMapper : Mapper<JsonObject, Track> {
    
    companion object {
        private const val TAG = "SpotifyTrackMapper"
    }
    
    override fun map(input: JsonObject): Track {
        Log.d(TAG, "Mapeando track desde JsonObject: ${input.get("name")?.asString}")
        
        return try {
            val title = extractTitle(input)
            val artist = extractArtist(input)
            val duration = formatDuration(input)
            val durationMs = extractDurationMs(input)
            val albumArt = extractAlbumArt(input)
            val previewUrl = input.get("preview_url")?.takeIf { !it.isJsonNull }?.asString
            
            Track(
                title = title,
                artist = artist,
                duration = duration,
                durationMs = durationMs,
                albumArt = albumArt,
                previewUrl = previewUrl
            ).also {
                Log.d(TAG, "✅ Track mapeado: ${it.title} - ${it.artist} (${it.duration})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mapeando track: ${input.get("name")?.asString}", e)
            throw e
        }
    }
    
    /**
     * Extrae el título de la canción del JSON de Spotify.
     */
    private fun extractTitle(json: JsonObject): String {
        return json.get("name")?.asString ?: ""
    }
    
    /**
     * Extrae y formatea los artistas del JSON de Spotify.
     * Convierte la lista de artistas en un string separado por comas.
     */
    private fun extractArtist(json: JsonObject): String {
        val artistsArray = json.getAsJsonArray("artists") ?: return ""
        val artists = mutableListOf<String>()
        
        for (i in 0 until artistsArray.size()) {
            val artistObj = artistsArray[i].asJsonObject
            val name = artistObj.get("name")?.asString
            if (name != null && name.isNotEmpty()) {
                artists.add(name)
            }
        }
        
        return artists.joinToString(", ")
    }
    
    /**
     * Extrae la duración en milisegundos del JSON de Spotify.
     */
    private fun extractDurationMs(json: JsonObject): Int {
        return json.get("duration_ms")?.asInt ?: 0
    }
    
    /**
     * Formatea la duración de milisegundos a string "MM:SS".
     */
    private fun formatDuration(json: JsonObject): String {
        val durationMs = extractDurationMs(json)
        val minutes = durationMs / 60000
        val seconds = (durationMs % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Extrae la URL del arte del álbum del JSON de Spotify.
     * Toma la primera imagen disponible.
     */
    private fun extractAlbumArt(json: JsonObject): String {
        val album = json.getAsJsonObject("album") ?: return ""
        val images = album.getAsJsonArray("images") ?: return ""
        if (images.size() == 0) return ""
        
        val firstImage = images[0].asJsonObject
        return firstImage.get("url")?.asString ?: ""
    }
}

