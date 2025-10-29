package com.g22.orbitsoundkotlin.models

import com.google.gson.annotations.SerializedName

/**
 * Modelo de dominio para una canción/track.
 * Solo contiene datos, sin lógica de negocio.
 * 
 * El parseo de JSON a Track se realiza mediante SpotifyTrackMapper
 * siguiendo el patrón Mapper para separación de responsabilidades.
 */
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
)
