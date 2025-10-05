// Data/repository/FirebaseEmotionRepository.kt
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseEmotionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : EmotionRepository {

    override suspend fun saveEmotionData(submission: EmotionSubmission): Result<Unit> {
        return try {
            // Reference the 'emotion_submissions' collection and add a new document
            firestore.collection("emotion_submissions")
                .add(submission)
                .await() // Converts the Firebase Task into a Kotlin suspend function
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}