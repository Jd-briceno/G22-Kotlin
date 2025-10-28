package com.g22.orbitsoundkotlin

import StellarEmotionsViewModel
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch
import com.g22.orbitsoundkotlin.services.AuthResult
import com.g22.orbitsoundkotlin.services.AuthService
import com.g22.orbitsoundkotlin.services.AuthUser
import com.g22.orbitsoundkotlin.data.RememberSettings
import com.g22.orbitsoundkotlin.data.UserPreferencesRepository
import com.g22.orbitsoundkotlin.data.userPreferencesStore
import com.g22.orbitsoundkotlin.ui.screens.home.HomeScreen
import com.g22.orbitsoundkotlin.ui.screens.InterestSelectionScreen
import com.g22.orbitsoundkotlin.ui.screens.library.LibraryScreen
import com.g22.orbitsoundkotlin.ui.screens.auth.AuthScreenCallbacks
import com.g22.orbitsoundkotlin.ui.screens.auth.AuthViewModel
import com.g22.orbitsoundkotlin.ui.screens.auth.LocalAuthScreenCallbacks
import com.g22.orbitsoundkotlin.ui.screens.auth.LoginScreen
import com.g22.orbitsoundkotlin.ui.screens.auth.SignupScreen
import com.g22.orbitsoundkotlin.ui.screens.profile.ProfileScreen
import com.g22.orbitsoundkotlin.ui.screens.StellarEmotionsScreen
import com.g22.orbitsoundkotlin.ui.screens.ConstellationsScreen
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
    val authViewModel: AuthViewModel = viewModel()
    val authUiState by authViewModel.uiState.collectAsState()

    var rememberMeChecked by remember { mutableStateOf(rememberSettings.rememberMe) }

    LaunchedEffect(rememberSettings.rememberMe) {
        rememberMeChecked = rememberSettings.rememberMe
    }

    LaunchedEffect(rememberSettings.email) {
        if (rememberSettings.rememberMe && authUiState.email.isBlank()) {
            authViewModel.onEmailChange(rememberSettings.email)
        }
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

    fun runAuthRequest(
        request: suspend () -> AuthResult,
        onSuccess: (AuthResult.Success) -> Unit
    ) {
        coroutineScope.launch {
            isAuthenticating = true
            try {
                when (val result = request()) {
                    is AuthResult.Success -> {
                        onSuccess(result)
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

    val authCallbacks = AuthScreenCallbacks(
        navigateToLogin = { destination = AppDestination.Login },
        navigateToSignup = { destination = AppDestination.SignUp },
        onForgotPassword = { email ->
            if (email.isBlank()) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Enter your email first to reset your password.")
                }
            } else {
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
            }
        },
        onGoogleSignIn = {
            val client = googleSignInClient
            if (client == null) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Configure Google Sign-In in Firebase before using this option.")
                }
            } else {
                isAuthenticating = true
                client.signOut()
                googleSignInLauncher.launch(client.signInIntent)
            }
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
        },
        rememberMeValue = rememberMeChecked,
        onRememberMeChange = { rememberMeChecked = it }
    )

    val onAuthCompleted = {
        val success = authViewModel.latestAuthResult()
        if (success != null) {
            val emailForSave = authViewModel.uiState.value.email.trim()
            coroutineScope.launch {
                val storedEmail = if (rememberMeChecked) emailForSave else ""
                userPreferencesRepository.updateRememberMe(rememberMeChecked, storedEmail)
            }
            handleAuthSuccess(success)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Authentication result unavailable.")
            }
        }
        Unit
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        when (val current = destination) {
            AppDestination.Login -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CompositionLocalProvider(LocalAuthScreenCallbacks provides authCallbacks) {
                        LoginScreen(
                            viewModel = authViewModel,
                            onAuthenticated = onAuthCompleted
                        )
                    }
                }
            }

            AppDestination.SignUp -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    CompositionLocalProvider(LocalAuthScreenCallbacks provides authCallbacks) {
                        SignupScreen(
                            viewModel = authViewModel,
                            onAuthenticated = onAuthCompleted
                        )
                    }
                }
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
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        user = current.user,
                        onNavigateToStellarEmotions = {
                            destination = AppDestination.StellarEmotions(current.user)
                        },
                        onNavigateToLibrary = {
                            destination = AppDestination.Library(current.user)
                        },
                        onNavigateToProfile = {
                            destination = AppDestination.Profile(current.user)
                        }
                    )
                }
            }
            is AppDestination.StellarEmotions -> {
                val viewModel = remember {
                    StellarEmotionsViewModel(userId = current.user.id)
                }
                StellarEmotionsScreen(
                    username = current.user.email?.split("@")?.firstOrNull() ?: "Captain",
                    onNavigateToConstellations = {
                        destination = AppDestination.Constellations(current.user)
                    },
                    viewModel = viewModel
                )
            }
            is AppDestination.Constellations -> {
                val username = current.user.email?.split("@")?.firstOrNull() ?: "Captain"
                ConstellationsScreen(
                    username = username,
                    onNavigateToHome = {
                        destination = AppDestination.Home(current.user)
                    }
                )
            }
            is AppDestination.Library -> {
                LibraryScreen(
                    onNavigateToProfile = {
                        destination = AppDestination.Profile(current.user)
                    },
                    onNavigateToHome = {
                        destination = AppDestination.Home(current.user)
                    }
                )
            }
            is AppDestination.Profile -> {
                ProfileScreen(
                    onNavigateToHome = {
                        destination = AppDestination.Home(current.user)
                    }
                )
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
    data class Constellations(val user: AuthUser) : AppDestination
    data class Library(val user: AuthUser) : AppDestination
    data class Profile(val user: AuthUser) : AppDestination
}
