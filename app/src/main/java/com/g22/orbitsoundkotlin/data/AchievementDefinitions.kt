package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.Achievement

/**
 * Definitions of all achievements in the app.
 * Each achievement has a unique ID and metadata.
 */
object AchievementDefinitions {
    
    // Achievement IDs
    const val ID_FIRST_LOGIN = "first_login"
    const val ID_FIRST_LIKE = "first_like"
    const val ID_EMOTION_EXPLORER = "emotion_explorer"
    const val ID_AI_MAESTRO = "ai_maestro"
    
    val FIRST_LOGIN = Achievement(
        id = ID_FIRST_LOGIN,
        name = "Circuit Starter",
        imageUrl = "🎮", // Emoji as icon
        description = "Welcome to OrbitSounds! Start your musical journey.",
        isUnlocked = false
    )
    
    val FIRST_LIKE = Achievement(
        id = ID_FIRST_LIKE,
        name = "First Love",
        imageUrl = "❤️",
        description = "Like your first track and start building your favorites.",
        isUnlocked = false
    )
    
    val EMOTION_EXPLORER = Achievement(
        id = ID_EMOTION_EXPLORER,
        name = "Emotion Explorer",
        imageUrl = "🎭",
        description = "Add your first emotion and discover music that matches your mood.",
        isUnlocked = false
    )
    
    val AI_MAESTRO = Achievement(
        id = ID_AI_MAESTRO,
        name = "AI Maestro",
        imageUrl = "🤖",
        description = "Generate your first AI-powered playlist with Ares.",
        isUnlocked = false
    )
    
    /**
     * All available achievements in the app.
     */
    val ALL = listOf(
        FIRST_LOGIN,
        FIRST_LIKE,
        EMOTION_EXPLORER,
        AI_MAESTRO
    )
    
    /**
     * Get achievement definition by ID.
     */
    fun getById(id: String): Achievement? {
        return ALL.firstOrNull { it.id == id }
    }
}

