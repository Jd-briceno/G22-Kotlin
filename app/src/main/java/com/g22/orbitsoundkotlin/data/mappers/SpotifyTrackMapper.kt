package com.g22.orbitsoundkotlin.data.mappers

import android.util.Log
import com.g22.orbitsoundkotlin.models.Track

/**
 * Mapper para convertir respuesta JSON de Spotify API a modelo Track.
 * Implementa el patrón Mapper para separar la lógica de parseo del modelo de dominio.
 */
class SpotifyTrackMapper : Mapper<Map<String, Any?>, Track> {
    
    companion object {
        private const val TAG = "SpotifyTrackMapper"
    }
    
    override fun map(input: Map<String, Any?>): Track {
        Log.d(TAG, "Mapeando track: ${input["name"]}")
        
        return try {
            val title = extractTitle(input)
            val artist = extractArtist(input)
            val duration = formatDuration(input)
            val durationMs = extractDurationMs(input)
            val albumArt = extractAlbumArt(input)
            
            Log.d(TAG, "Datos extraídos - Title: '$title', Artist: '$artist', Duration: $duration")
            
            Track(
                title = title,
                artist = artist,
                duration = duration,
                durationMs = durationMs,
                albumArt = albumArt,
                previewUrl = input["preview_url"] as? String
            ).also {
                Log.d(TAG, "✅ Track final: '${it.title}' por '${it.artist}' (${it.duration})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error mapeando track", e)
            throw e
        }
    }
    
    /**
     * Extrae el título de la canción del JSON de Spotify.
     */
    private fun extractTitle(json: Map<String, Any?>): String {
        return json["name"] as? String ?: ""
    }
    
    /**
     * Extrae y formatea los artistas del JSON de Spotify.
     * Convierte la lista de artistas en un string separado por comas.
     */
    private fun extractArtist(json: Map<String, Any?>): String {
        val artists = (json["artists"] as? List<*>)?.mapNotNull { artist ->
            when (artist) {
                is Map<*, *> -> artist["name"] as? String
                else -> null
            }
        }?.filter { it.isNotEmpty() } ?: emptyList()
        
        return artists.joinToString(", ")
    }
    
    /**
     * Extrae la duración en milisegundos del JSON de Spotify.
     */
    private fun extractDurationMs(json: Map<String, Any?>): Int {
        return (json["duration_ms"] as? Number)?.toInt() ?: 0
    }
    
    /**
     * Formatea la duración de milisegundos a string "MM:SS".
     */
    private fun formatDuration(json: Map<String, Any?>): String {
        val durationMs = extractDurationMs(json)
        val minutes = durationMs / 60000
        val seconds = (durationMs % 60000) / 1000
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Extrae la URL del arte del álbum del JSON de Spotify.
     * Toma la primera imagen disponible.
     */
    private fun extractAlbumArt(json: Map<String, Any?>): String {
        val album = json["album"] as? Map<String, Any?>
        val images = album?.get("images") as? List<*>
        return images?.firstOrNull()?.let { image ->
            when (image) {
                is Map<*, *> -> image["url"] as? String
                else -> null
            }
        } ?: ""
    }
}

