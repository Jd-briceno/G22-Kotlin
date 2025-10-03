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
import com.g22.orbitsoundkotlin.ui.screens.HomeScreen
import com.g22.orbitsoundkotlin.ui.screens.InterestSelectionScreen
import com.g22.orbitsoundkotlin.ui.screens.LoginScreen
import com.g22.orbitsoundkotlin.ui.screens.SignupScreen
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

    fun runAuthRequest(
        request: suspend () -> AuthResult,
        onSuccess: (AuthResult.Success) -> Unit
    ) {
        coroutineScope.launch {
            isAuthenticating = true
            when (val result = request()) {
                is AuthResult.Success -> onSuccess(result)
                is AuthResult.Error -> snackbarHostState.showSnackbar(result.message)
            }
            isAuthenticating = false
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
        if (result.resultCode != Activity.RESULT_OK) {
            isAuthenticating = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Google sign-in was cancelled.")
            }
            return@rememberLauncherForActivityResult
        }
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            if (token.isNullOrBlank()) {
                isAuthenticating = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Unable to retrieve Google credentials.")
                }
            } else {
                runAuthRequest(
                    request = { authService.signInWithGoogle(token) }
                ) { success ->
                    handleAuthSuccess(success)
                }
            }
        } catch (ex: ApiException) {
            isAuthenticating = false
            coroutineScope.launch {
                snackbarHostState.showSnackbar(ex.localizedMessage ?: "Google sign-in failed.")
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
                    onSignIn = { email, password, _ ->
                        if (email.isBlank() || password.isBlank()) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Enter email and password to continue.")
                            }
                            return@LoginScreen
                        }
                       runAuthRequest(
                           request = { authService.signInWithEmail(email, password) }
                        ) { success ->
                            handleAuthSuccess(success)
                        }
                    },
                    onForgotPassword = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Password recovery isn't available yet.")
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
                        destination = AppDestination.Home(current.user)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Skipped interest selection.")
                        }
                    },
                    onContinue = { selections ->
                        destination = AppDestination.Home(current.user)
                        coroutineScope.launch {
                            val message = if (selections.isEmpty()) {
                                "Saved without selecting interests."
                            } else {
                                "Saved ${selections.size} interests."
                            }
                            snackbarHostState.showSnackbar(message)
                        }
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
                }
            }
        }
    }
}

private sealed interface AppDestination {
    data object Login : AppDestination
    data object SignUp : AppDestination
    data class Interests(val user: AuthUser) : AppDestination
    data class Home(val user: AuthUser) : AppDestination
}
