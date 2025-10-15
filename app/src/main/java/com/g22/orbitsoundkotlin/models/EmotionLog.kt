package com.g22.orbitsoundkotlin.models

data class EmotionLog(
    val id: String? = null,
    val emotions: List<String>,
    val clientTs: Long,
    val meta: Map<String, Any>? = null
)
