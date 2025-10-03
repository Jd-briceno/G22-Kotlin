package com.g22.orbitsoundkotlin.auth

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class AuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Unable to authenticate right now.")
            val requiresProfile = requiresProfileCompletion(user.uid)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    suspend fun registerWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return AuthResult.Error("Unable to create the account right now.")
            val requiresProfile = runCatching {
                ensureUserDocument(user, isNewUser = true)
                requiresProfileCompletion(user.uid)
            }.getOrDefault(true)
            AuthResult.Success(user.toAuthUser(), requiresProfileCompletion = requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    suspend fun signInWithSpotify(): AuthResult {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: return AuthResult.Error("Spotify sign-in failed. Try again.")
            ensureUserDocument(user, isNewUser = result.additionalUserInfo?.isNewUser == true)
            val requiresProfile = requiresProfileCompletion(user.uid)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    suspend fun updateUserInterests(
        user: AuthUser,
        interests: List<String>,
        skipped: Boolean
    ): Result<Unit> {
        return runCatching {
            val data = buildMap<String, Any> {
                put("email", user.email ?: "")
                put("updatedAt", FieldValue.serverTimestamp())
                put("completedInterests", !skipped)
                if (skipped) {
                    put("interests", emptyList<String>())
                } else {
                    put("interests", interests)
                }
            }
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(data, SetOptions.merge())
                .await()
        }
    }

    private suspend fun signInWithCredential(credential: AuthCredential): AuthResult {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("Unable to authenticate right now.")
            val requiresProfile = runCatching {
                ensureUserDocument(user, isNewUser = result.additionalUserInfo?.isNewUser == true)
                requiresProfileCompletion(user.uid)
            }.getOrDefault(false)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    private suspend fun ensureUserDocument(user: FirebaseUser, isNewUser: Boolean) {
        val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        if (isNewUser) {
            val data = mapOf(
                "email" to (user.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "completedInterests" to false
            )
            docRef.set(data, SetOptions.merge()).await()
        } else {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "email" to (user.email ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "completedInterests" to false
                )
                docRef.set(data, SetOptions.merge()).await()
            }
        }
    }

    private suspend fun requiresProfileCompletion(uid: String): Boolean {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        if (!snapshot.exists()) return true
        val completed = snapshot.getBoolean("completedInterests") ?: false
        val interests = snapshot.get("interests") as? List<*> ?: emptyList<Any>()
        return !(completed && interests.isNotEmpty())
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        id = uid,
        email = email
    )

    private fun Exception.humanMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters long."
        is FirebaseAuthUserCollisionException -> "That email is already registered. Try signing in instead."
        is FirebaseAuthInvalidCredentialsException -> "Invalid credentials. Check your email and password."
        is FirebaseAuthInvalidUserException -> "We couldn't find an account with those details."
        else -> localizedMessage ?: "Something went wrong. Please try again."
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}

data class AuthUser(
    val id: String,
    val email: String?
)

sealed class AuthResult {
    data class Success(
        val user: AuthUser,
        val requiresProfileCompletion: Boolean
    ) : AuthResult()

    data class Error(val message: String) : AuthResult()
}
