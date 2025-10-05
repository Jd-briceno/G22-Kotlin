interface EmotionRepository {
    // Suspend function for coroutine-friendly asynchronous operation
    suspend fun saveEmotionData(submission: EmotionSubmission): Result<Unit>
}