package com.g22.orbitsoundkotlin.services

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Implementa IAuthService y usa Singleton (GoF) con constructor privado + getInstance()
class AuthService private constructor(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : IAuthService {

    companion object {
        @Volatile private var INSTANCE: AuthService? = null

        fun getInstance(): AuthService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthService().also { INSTANCE = it }
            }

        // optional para tests
        fun setTestInstance(fake: AuthService?) { INSTANCE = fake }

        private const val USERS_COLLECTION = "users"
    }

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // --------- MÉTODOS PÚBLICOS (override) ---------

    override suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).awaitResult()
            val user = result.user ?: return AuthResult.Error("Unable to authenticate right now.")
            val requiresProfile = requiresProfileCompletion(user.uid)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    override suspend fun signup(name: String, email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).awaitResult()
            val user = result.user ?: return AuthResult.Error("Unable to create the account right now.")
            // best-effort en background para no bloquear UI
            backgroundScope.launch {
                try {
                    ensureUserDocument(user, isNewUser = true, name = name)
                } catch (c: CancellationException) {
                    throw c
                } catch (_: Exception) { /* ignore */ }
            }
            AuthResult.Success(user.toAuthUser(), requiresProfileCompletion = true)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        return signInWithCredential(credential)
    }

    override suspend fun signInWithSpotify(): AuthResult {
        return try {
            val result = auth.signInAnonymously().awaitResult()
            val user = result.user ?: return AuthResult.Error("Spotify sign-in failed. Try again.")
            runCatching { ensureUserDocument(user, isNewUser = result.additionalUserInfo?.isNewUser == true) }
            val requiresProfile = requiresProfileCompletion(user.uid)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).awaitResult()
            Result.success(Unit)
        } catch (ex: Exception) {
            val message = when (ex) {
                is FirebaseAuthInvalidUserException -> "We couldn't find an account for $email."
                else -> ex.humanMessage()
            }
            Result.failure(Exception(message))
        }
    }

    override suspend fun updateUserInterests(
        user: AuthUser,
        interests: List<String>,
        skipped: Boolean
    ): Result<Unit> {
        return runCatching {
            val cleanInterests = interests.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            val data = buildMap<String, Any> {
                put("email", user.email ?: "")
                put("updatedAt", FieldValue.serverTimestamp())
                put("completedInterests", true) // no bloquear futuros logins
                put("interests", cleanInterests)
            }
            firestore.collection(USERS_COLLECTION)
                .document(user.id)
                .set(data, SetOptions.merge())
                .awaitResult()
            Unit
        }
    }

    // --------- HELPERS PRIVADOS ---------

    private suspend fun signInWithCredential(credential: AuthCredential): AuthResult {
        return try {
            val result = auth.signInWithCredential(credential).awaitResult()
            val user = result.user ?: return AuthResult.Error("Unable to authenticate right now.")
            runCatching { ensureUserDocument(user, isNewUser = result.additionalUserInfo?.isNewUser == true) }
            val requiresProfile = requiresProfileCompletion(user.uid)
            AuthResult.Success(user.toAuthUser(), requiresProfile)
        } catch (ex: Exception) {
            AuthResult.Error(ex.humanMessage())
        }
    }

    private suspend fun ensureUserDocument(
        user: FirebaseUser,
        isNewUser: Boolean,
        name: String? = null
    ) {
        val docRef = firestore.collection(USERS_COLLECTION).document(user.uid)
        if (isNewUser) {
            val data = mapOf(
                "email" to (user.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "name" to (name ?: user.displayName.orEmpty()),
                "completedInterests" to false
            )
            docRef.set(data, SetOptions.merge()).awaitResult()
        } else {
            val snapshot = docRef.get().awaitResult()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "email" to (user.email ?: ""),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "name" to (name ?: user.displayName.orEmpty()),
                    "completedInterests" to false
                )
                docRef.set(data, SetOptions.merge()).awaitResult()
            }
        }
    }

    private suspend fun requiresProfileCompletion(uid: String): Boolean {
        val snapshot: DocumentSnapshot =
            firestore.collection(USERS_COLLECTION).document(uid).get().awaitResult()
        if (!snapshot.exists()) return true
        val completed = snapshot.getBoolean("completedInterests") ?: false
        return !completed
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
}

/** -------- Helpers: reemplazo de tasks.await() sin kotlinx-coroutines-play-services -------- */
private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                cont.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
        addOnCanceledListener {
            if (cont.isActive) {
                cont.resumeWithException(RuntimeException("Task was cancelled"))
            }
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
