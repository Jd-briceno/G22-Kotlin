package com.g22.orbitsoundkotlin

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.auth.AuthResult
import com.g22.orbitsoundkotlin.auth.AuthService
import com.g22.orbitsoundkotlin.auth.AuthUser
import com.g22.orbitsoundkotlin.data.RememberSettings
import com.g22.orbitsoundkotlin.data.UserPreferencesRepository
import com.g22.orbitsoundkotlin.data.userPreferencesStore
import com.g22.orbitsoundkotlin.ui.screens.HomeScreen
import com.g22.orbitsoundkotlin.ui.screens.InterestSelectionScreen
import com.g22.orbitsoundkotlin.ui.screens.LoginScreen
import com.g22.orbitsoundkotlin.ui.screens.SignupScreen
import com.g22.orbitsoundkotlin.ui.screens.StellarEmotionsScreen
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        setContent {
            OrbitSoundKotlinTheme {
                OrbitSoundApp()
            }
        }
    }
}

@Composable
private fun OrbitSoundApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val authService = remember { AuthService() }
    val context = LocalContext.current
    val userPreferencesRepository = remember { UserPreferencesRepository(context.userPreferencesStore) }
    val rememberSettings by userPreferencesRepository.rememberSettings.collectAsState(initial = RememberSettings())

    // Define a placeholder AuthUser for debug
    val debugUser = remember {
        AuthUser(
            id = "debug-uid-12345",
            email = "debug@example.com"
        )
    }

    val googleClientId = remember {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        resId.takeIf { it != 0 }?.let(context::getString)
    }

    val googleSignInClient = remember(googleClientId) {
        googleClientId?.let { clientId ->
            GoogleSignIn.getClient(
                context,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(clientId)
                    .requestEmail()
                    .build()
            )
        }
    }

    var destination by remember { mutableStateOf<AppDestination>(AppDestination.Login) }
    var isAuthenticating by remember { mutableStateOf(false) }

    // Testing purposes:
    //var destination by remember { mutableStateOf<AppDestination>(AppDestination.Home(debugUser)) }
    //var isAuthenticating by remember { mutableStateOf(false) }

    fun runAuthRequest(
        request: suspend () -> AuthResult,
        onSuccess: (AuthResult.Success) -> Unit
    ) {
        coroutineScope.launch {
            isAuthenticating = true
            try {
                val result = request()
                when (result) {
                    is AuthResult.Success -> {
                        onSuccess(result)   // 🔑 primero navegamos
                        isAuthenticating = false
                    }
                    is AuthResult.Error -> {
                        snackbarHostState.showSnackbar(result.message)
                        isAuthenticating = false
                    }
                }
            } catch (t: Throwable) {
                snackbarHostState.showSnackbar(
                    t.message ?: "Something went wrong. Please try again."
                )
                isAuthenticating = false
            }
        }
    }


    fun handleAuthSuccess(success: AuthResult.Success) {
        destination = if (success.requiresProfileCompletion) {
            AppDestination.Interests(success.user)
        } else {
            AppDestination.Home(success.user)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            isAuthenticating = true
            try {
                if (result.resultCode != Activity.RESULT_OK) {
                    snackbarHostState.showSnackbar("Google sign-in was cancelled.")
                    return@launch
                }
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val token = account?.idToken
                if (token.isNullOrBlank()) {
                    snackbarHostState.showSnackbar("Unable to retrieve Google credentials.")
                } else {
                    when (val auth = authService.signInWithGoogle(token)) {
                        is AuthResult.Success -> handleAuthSuccess(auth)
                        is AuthResult.Error -> snackbarHostState.showSnackbar(auth.message)
                    }
                }
            } catch (ex: ApiException) {
                snackbarHostState.showSnackbar(ex.localizedMessage ?: "Google sign-in failed.")
            } finally {
                isAuthenticating = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        when (val current = destination) {
            AppDestination.Login -> {
                LoginScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    isLoading = isAuthenticating,
                    initialEmail = rememberSettings.email,
                    initialRememberMe = rememberSettings.rememberMe,
                    showSocialProviders = false,
                    onSignIn = { email, password, remember ->
                        val sanitizedEmail = email.trim()
                        val sanitizedPassword = password.trim()
                        if (sanitizedEmail.isBlank() || sanitizedPassword.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Enter email and password to continue.")
                            }
                            return@LoginScreen
                        }
                        runAuthRequest(
                            request = { authService.signInWithEmail(sanitizedEmail, sanitizedPassword) }
                        ) { success ->
                            coroutineScope.launch {
                                userPreferencesRepository.updateRememberMe(remember, sanitizedEmail)
                            }
                            handleAuthSuccess(success)
                        }
                    },
                    onForgotPassword = { email ->
                        if (email.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Enter your email first to reset your password.")
                            }
                            return@LoginScreen
                        }
                        coroutineScope.launch {
                            isAuthenticating = true
                            val reset = authService.sendPasswordReset(email)
                            if (reset.isSuccess) {
                                snackbarHostState.showSnackbar("Password reset email sent to $email.")
                            } else {
                                val message = reset.exceptionOrNull()?.message
                                    ?: "Unable to send reset email."
                                snackbarHostState.showSnackbar(message)
                            }
                            isAuthenticating = false
                        }
                    },
                    onNavigateToSignUp = {
                        destination = AppDestination.SignUp
                    },
                    onGoogleSignIn = {
                        val client = googleSignInClient
                        if (client == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Configure Google Sign-In in Firebase before using this option.")
                            }
                            return@LoginScreen
                        }
                        isAuthenticating = true
                        client.signOut()
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    onAppleSignIn = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Apple sign-in isn't available on Android yet.")
                        }
                    },
                    onSpotifySignIn = {
                        runAuthRequest(
                            request = { authService.signInWithSpotify() }
                        ) { success ->
                            handleAuthSuccess(success)
                        }
                    }
                )
            }
            AppDestination.SignUp -> {
                SignupScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    isLoading = isAuthenticating,
                    onSignUp = { email, password, confirm ->
                        when {
                            email.isBlank() || password.isBlank() || confirm.isBlank() -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please fill in all fields.")
                                }
                            }
                            password != confirm -> {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Passwords do not match.")
                                }
                            }
                            else -> runAuthRequest(
                                request = { authService.registerWithEmail(email, password) }
                            ) { success ->
                                handleAuthSuccess(success)
                            }
                        }
                    },
                    onAlreadyHaveAccount = { destination = AppDestination.Login },
                    onGoogleSignUp = {
                        val client = googleSignInClient
                        if (client == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Configure Google Sign-In in Firebase before using this option.")
                            }
                            return@SignupScreen
                        }
                        isAuthenticating = true
                        client.signOut()
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    onAppleSignUp = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Apple sign-in isn't available on Android yet.")
                        }
                    },
                    onSpotifySignUp = {
                        runAuthRequest(
                            request = { authService.signInWithSpotify() }
                        ) { success ->
                            handleAuthSuccess(success)
                        }
                    }
                )
            }
            is AppDestination.Interests -> {
                InterestSelectionScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    isSaving = isAuthenticating,
                    onBack = {
                        destination = AppDestination.Login
                    },
                    onSkip = {
                        // Marcamos perfil como completado aunque no haya intereses,
                        // y navegamos inmediatamente a Home.
                        coroutineScope.launch {
                            authService.updateUserInterests(
                                user = current.user,
                                interests = emptyList(),
                                skipped = true
                            )
                        }
                        destination = AppDestination.Home(current.user)
                    },
                    onContinue = { selections ->
                        coroutineScope.launch {
                            authService.updateUserInterests(
                                user = current.user,
                                interests = selections,
                                skipped = false
                            )
                        }
                        destination = AppDestination.Home(current.user)
                    }
                )
            }
            is AppDestination.Home -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    HomeScreen()
                    // Testing purposes:
                    //StellarEmotionsScreen(username = current.user.email ?: "User")
                }
            }
            is AppDestination.StellarEmotions -> {
                StellarEmotionsScreen(username = current.user.email ?: "User")
            }
        }
    }
}

private sealed interface AppDestination {
    data object Login : AppDestination
    data object SignUp : AppDestination
    data class Interests(val user: AuthUser) : AppDestination
    data class Home(val user: AuthUser) : AppDestination
    data class StellarEmotions(val user: AuthUser) : AppDestination
}
