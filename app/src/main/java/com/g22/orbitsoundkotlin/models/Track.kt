package com.g22.orbitsoundkotlin.models

import com.google.gson.annotations.SerializedName

data class Track(
    val title: String,
    val artist: String,
    val duration: String, // formatted string "3:45"
    @SerializedName("duration_ms")
    val durationMs: Int, // raw milliseconds
    @SerializedName("album_art")
    val albumArt: String,
    @SerializedName("preview_url")
    val previewUrl: String? = null // optional, for 30s preview playback
) {
    companion object {
        fun fromSpotify(json: Map<String, Any?>): Track {
            val durationMs = (json["duration_ms"] as? Number)?.toInt() ?: 0
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000

            // Manejar artists como List<Any> (que puede contener Map<String, Any?>)
            val artists = (json["artists"] as? List<*>)?.mapNotNull { artist ->
                when (artist) {
                    is Map<*, *> -> artist["name"] as? String
                    else -> null
                }
            }?.filter { it.isNotEmpty() } ?: emptyList()

            // Manejar album como Map<String, Any?>
            val album = json["album"] as? Map<String, Any?>
            val images = album?.get("images") as? List<*>
            val albumArtUrl = images?.firstOrNull()?.let { image ->
                when (image) {
                    is Map<*, *> -> image["url"] as? String
                    else -> null
                }
            } ?: ""

            return Track(
                title = json["name"] as? String ?: "",
                artist = artists.joinToString(", "),
                duration = String.format("%d:%02d", minutes, seconds),
                durationMs = durationMs,
                albumArt = albumArtUrl,
                previewUrl = json["preview_url"] as? String
            )
        }
    }
}
