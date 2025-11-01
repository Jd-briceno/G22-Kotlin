package com.g22.orbitsoundkotlin.data

import com.g22.orbitsoundkotlin.models.Constellation
import com.g22.orbitsoundkotlin.models.EmotionModel
import java.util.Calendar

/**
 * Motor de recomendaciones musicales basado en:
 * - Constelaciones seleccionadas (tema espacial)
 * - Emociones recientes del usuario
 * - Contexto temporal (hora del día)
 * 
 * Patrón: Strategy + Factory
 * 
 * VALORES REALES DE LA APP:
 * - Constelaciones: "fenix", "draco", "pegasus", "cisne", "ursa_mayor", "cruz"
 * - Emociones: "Anxiety", "Anger", "Disgust", "Envy", "Embarrassment", "Love", 
 *              "Boredom", "Joy", "Sadness", "Fear"
 */
object MusicRecommendationEngine {
    
    /**
     * Genera secciones personalizadas de música para el usuario.
     * 
     * @param userConstellations Constelaciones seleccionadas por el usuario (de ConstellationsScreen)
     * @param recentEmotions Emociones recientes (de EmotionRepository, últimas 3-5)
     * @return Lista de secciones con query de búsqueda y nombre espacial
     */
    fun generatePlaylistSections(
        userConstellations: List<Constellation> = emptyList(),
        recentEmotions: List<EmotionModel> = emptyList()
    ): List<PlaylistSection> {
        val sections = mutableListOf<PlaylistSection>()
        
        // 1. Sección basada en hora del día (siempre presente)
        sections.add(getTimeBasedSection())
        
        // 2. Secciones basadas en constelaciones del usuario (hasta 2)
        val constellationSections = userConstellations
            .take(2)
            .map { getConstellationSection(it) }
        sections.addAll(constellationSections)
        
        // 3. Sección basada en emoción más reciente (si existe)
        recentEmotions.firstOrNull()?.let { emotion ->
            sections.add(getEmotionSection(emotion))
        }
        
        // 4. Completar con secciones por defecto si faltan
        while (sections.size < 4) {
            sections.add(getDefaultSection(sections.size))
        }
        
        return sections.take(4) // Máximo 4 secciones
    }
    
    /**
     * Sección basada en la hora del día.
     */
    private fun getTimeBasedSection(): PlaylistSection {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 5..11 -> PlaylistSection(
                title = "☀️ Morning Orbit",
                subtitle = "Energía para comenzar tu día",
                query = "morning energetic music",
                icon = "☀️"
            )
            in 12..17 -> PlaylistSection(
                title = "🌤️ Stellar Afternoon",
                subtitle = "Mantén el ritmo",
                query = "afternoon productive music",
                icon = "🌤️"
            )
            in 18..22 -> PlaylistSection(
                title = "🌆 Twilight Voyage",
                subtitle = "Relájate y desconecta",
                query = "evening chill music",
                icon = "🌆"
            )
            else -> PlaylistSection(
                title = "🌙 Midnight Constellation",
                subtitle = "Música para la noche",
                query = "night ambient music",
                icon = "🌙"
            )
        }
    }
    
    /**
     * Mapeo de constelación a estilo musical (tema espacial).
     * Usa el campo `id` de Constellation (no `title`).
     */
    private fun getConstellationSection(constellation: Constellation): PlaylistSection {
        return when (constellation.id.lowercase()) {
            "cisne", "swan", "cygnus" -> PlaylistSection(
                title = "🦢 Cisne's Symphony",
                subtitle = "Elegancia clásica",
                query = "classical orchestral music",
                icon = "🦢"
            )
            "pegasus", "pegaso" -> PlaylistSection(
                title = "🐴 Pegasus Flight",
                subtitle = "Épica y poder",
                query = "epic rock power metal",
                icon = "🐴"
            )
            "draco", "dragón" -> PlaylistSection(
                title = "🐉 Draco's Lair",
                subtitle = "Oscuridad electrónica",
                query = "dark electronic music",
                icon = "🐉"
            )
            "ursa major", "osa mayor" -> PlaylistSection(
                title = "🐻 Ursa's Path",
                subtitle = "Folk y naturaleza",
                query = "indie folk acoustic",
                icon = "🐻"
            )
            "cruz", "crux", "cruz del sur" -> PlaylistSection(
                title = "✨ Southern Cross",
                subtitle = "Ritmos latinos",
                query = "latin tropical music",
                icon = "✨"
            )
            "phoenix", "fenix", "fénix" -> PlaylistSection(
                title = "🔥 Phoenix Rising",
                subtitle = "Energía electrónica",
                query = "edm electronic dance",
                icon = "🔥"
            )
            else -> PlaylistSection(
                title = "⭐ ${constellation.title} Sounds",
                subtitle = "Música de las estrellas",
                query = "popular music hits",
                icon = "⭐"
            )
        }
    }
    
    /**
     * Mapeo de emoción a mood musical.
     * Usa el campo `name` de EmotionModel.
     */
    private fun getEmotionSection(emotion: EmotionModel): PlaylistSection {
        return when (emotion.name.lowercase()) {
            "joy", "alegría", "felicidad" -> PlaylistSection(
                title = "😊 Joyful Nebula",
                subtitle = "Vibras positivas",
                query = "happy upbeat music",
                icon = "😊"
            )
            "sadness", "tristeza" -> PlaylistSection(
                title = "😢 Melancholy Moon",
                subtitle = "Reflexión y calma",
                query = "sad melancholic acoustic",
                icon = "😢"
            )
            "anger", "ira", "enojo" -> PlaylistSection(
                title = "😤 Volcanic Beats",
                subtitle = "Descarga tu energía",
                query = "aggressive rock metal",
                icon = "😤"
            )
            "love", "amor" -> PlaylistSection(
                title = "💕 Romantic Cosmos",
                subtitle = "Para el corazón",
                query = "romantic love songs",
                icon = "💕"
            )
            "anxiety", "ansiedad" -> PlaylistSection(
                title = "🌊 Calm Waves",
                subtitle = "Tranquilidad",
                query = "calm ambient relaxing",
                icon = "🌊"
            )
            "fear", "miedo" -> PlaylistSection(
                title = "🌟 Brave Stars",
                subtitle = "Encuentra tu valor",
                query = "motivational inspiring music",
                icon = "🌟"
            )
            "boredom", "aburrimiento" -> PlaylistSection(
                title = "🎨 Discovery Zone",
                subtitle = "Algo nuevo",
                query = "eclectic alternative music",
                icon = "🎨"
            )
            "disgust", "asco" -> PlaylistSection(
                title = "🤢 Raw & Real",
                subtitle = "Expresión auténtica",
                query = "grunge alternative punk",
                icon = "🤢"
            )
            "envy", "envidia" -> PlaylistSection(
                title = "💚 Ambition Drive",
                subtitle = "Motivación competitiva",
                query = "motivational hip hop",
                icon = "💚"
            )
            "embarrassment", "vergüenza" -> PlaylistSection(
                title = "😳 Comfort Zone",
                subtitle = "Música reconfortante",
                query = "comfort acoustic indie",
                icon = "😳"
            )
            else -> PlaylistSection(
                title = "🎵 ${emotion.name} Vibes",
                subtitle = "Música para tu mood",
                query = "${emotion.name} mood music",
                icon = "🎵"
            )
        }
    }
    
    /**
     * Secciones por defecto si no hay suficientes personalizadas.
     */
    private fun getDefaultSection(index: Int): PlaylistSection {
        val defaults = listOf(
            PlaylistSection(
                title = "🚀 Orbit Crew Favorites",
                subtitle = "Lo más popular",
                query = "top hits popular music",
                icon = "🚀"
            ),
            PlaylistSection(
                title = "🌌 Cosmic Chill",
                subtitle = "Lofi espacial",
                query = "lofi chill beats",
                icon = "🌌"
            ),
            PlaylistSection(
                title = "🪐 Saturn's Rings",
                subtitle = "Exploración sonora",
                query = "experimental electronic",
                icon = "🪐"
            ),
            PlaylistSection(
                title = "💫 Starlight Classics",
                subtitle = "Éxitos atemporales",
                query = "classic rock pop",
                icon = "💫"
            )
        )
        return defaults[index % defaults.size]
    }
    
    /**
     * Data class para sección de playlist.
     */
    data class PlaylistSection(
        val title: String,
        val subtitle: String,
        val query: String,
        val icon: String
    )
}

