import com.g22.orbitsoundkotlin.models.EmotionModel
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class EmotionSubmission(
    val userEmail: String = "",
    val emotions: List<EmotionModel> = emptyList(),
    // @ServerTimestamp automatically records the time on the Firestore server
    @ServerTimestamp
    val timestamp: Date? = null
)

data class EmotionData(
    val id: String = "",
    val name: String = "",
    val value: Float = 0f
)