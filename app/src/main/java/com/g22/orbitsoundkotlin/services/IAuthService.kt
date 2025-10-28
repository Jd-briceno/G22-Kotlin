package com.g22.orbitsoundkotlin.services

interface IAuthService {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun signup(name: String, email: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String): AuthResult
    suspend fun signInWithSpotify(): AuthResult
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun updateUserInterests(
        user: AuthUser,
        interests: List<String>,
        skipped: Boolean
    ): Result<Unit>
}