import com.g22.orbitsoundkotlin.models.EmotionModel
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// This data class represents the document structure in Firestore
data class EmotionSubmission(
    val userEmail: String = "",
    val emotions: List<EmotionModel> = emptyList(),
    // @ServerTimestamp automatically records the time on the Firestore server
    @ServerTimestamp
    val timestamp: Date? = null
)

// The structure for each emotion item in the array
data class EmotionData(
    val id: String = "",
    val name: String = "",
    val value: Float = 0f
    // You can add more fields like 'source' if needed (e.g., "knob1")
)