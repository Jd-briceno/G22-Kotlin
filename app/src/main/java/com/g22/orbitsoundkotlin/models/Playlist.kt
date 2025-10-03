package com.g22.orbitsoundkotlin.models

data class Playlist(
    val title: String,
    val cover: String,
    val id: String? = null,
    val description: String? = null,
    val trackCount: Int? = null
)
