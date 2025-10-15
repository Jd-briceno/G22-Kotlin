// Data/repository/FirebaseEmotionRepository.kt
import com.g22.orbitsoundkotlin.data.EmotionRepository
import com.g22.orbitsoundkotlin.models.EmotionLog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseEmotionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : EmotionRepository {

    override suspend fun logEmotions(emotionLog: EmotionLog): Result<Unit> {
        return try {
            firestore.collection("emotion_logs")
                .add(emotionLog)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveEmotionData(submission: EmotionSubmission): Result<Unit> {
        return try {
            firestore.collection("emotion_submissions")
                .add(submission)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}