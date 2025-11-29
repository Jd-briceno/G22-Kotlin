package com.g22.orbitsoundkotlin

import com.g22.orbitsoundkotlin.ui.viewmodels.StellarEmotionsViewModel
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
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
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.g22.orbitsoundkotlin.analytics.MusicAnalytics
import com.g22.orbitsoundkotlin.services.AuthResult
import com.g22.orbitsoundkotlin.services.AuthService
import com.g22.orbitsoundkotlin.services.AuthUser
import com.g22.orbitsoundkotlin.data.UserPreferencesRepository
import com.g22.orbitsoundkotlin.data.local.AppDatabase
import com.g22.orbitsoundkotlin.data.repositories.InterestsRepository
import com.g22.orbitsoundkotlin.data.userPreferencesStore
import com.g22.orbitsoundkotlin.data.repositories.LibraryCacheRepository
import com.g22.orbitsoundkotlin.ui.viewmodels.AuthViewModelFactory
import com.g22.orbitsoundkotlin.ui.screens.home.HomeScreen
import com.g22.orbitsoundkotlin.ui.screens.InterestSelectionScreen
import com.g22.orbitsoundkotlin.ui.screens.library.LibraryScreen
import com.g22.orbitsoundkotlin.ui.viewmodels.LibraryViewModel
import com.g22.orbitsoundkotlin.ui.screens.auth.AuthScreenCallbacks
import com.g22.orbitsoundkotlin.ui.viewmodels.AuthViewModel
import com.g22.orbitsoundkotlin.ui.screens.auth.LocalAuthScreenCallbacks
import com.g22.orbitsoundkotlin.ui.screens.auth.LoginScreen
import com.g22.orbitsoundkotlin.ui.screens.auth.SignupScreen
import com.g22.orbitsoundkotlin.ui.screens.profile.ProfileScreen
import com.g22.orbitsoundkotlin.ui.screens.emotions.StellarEmotionsScreen
import com.g22.orbitsoundkotlin.ui.screens.emotions.ConstellationsScreen
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import com.g22.orbitsoundkotlin.utils.SyncManager
import com.g22.orbitsoundkotlin.ui.viewmodels.InterestsViewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.InterestsViewModelFactory
import com.g22.orbitsoundkotlin.ui.viewmodels.CaptainsLogViewModel
import com.g22.orbitsoundkotlin.ui.viewmodels.CaptainsLogViewModelFactory
import com.g22.orbitsoundkotlin.ui.screens.activitystats.ActivityStatsScreen
import com.g22.orbitsoundkotlin.ui.screens.activitystats.ActivityStatsPinSetupScreen
import com.g22.orbitsoundkotlin.ui.screens.activitystats.ActivityStatsPinUnlockScreen
import com.g22.orbitsoundkotlin.ui.screens.activitystats.ActivityStatsPinResetScreen
import com.g22.orbitsoundkotlin.ui.screens.activitystats.ActivityStatsPinStorage
import com.g22.orbitsoundkotlin.ui.viewmodels.ActivityPeriod
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseApp.initializeApp(this)
        
        // 📊 Inicializar Analytics
        MusicAnalytics.initialize(this)
        
        // ✅ CONECTIVIDAD EVENTUAL: Inicializar SyncManager para Workers periódicos
        SyncManager(this).startPeriodicSync()
        
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
    val authService = remember { AuthService.getInstance() }
    val context = LocalContext.current
    // ✅ CONECTIVIDAD EVENTUAL: Inyectar Context al AuthViewModel
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context)
    )
    val authUiState by authViewModel.uiState.collectAsState()

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
        }
    )

    val onAuthCompleted = {
        val success = authViewModel.latestAuthResult()
        if (success != null) {
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
                val context = LocalContext.current
                val interestsFactory = remember(context) {
                    val database = AppDatabase.getInstance(context)
                    val interestsRepository = InterestsRepository(database)
                    val preferencesRepository = UserPreferencesRepository(context.userPreferencesStore)
                    InterestsViewModelFactory(
                        interestsRepository = interestsRepository,
                        preferencesRepository = preferencesRepository
                    )
                }
                val interestsViewModel: InterestsViewModel = viewModel(factory = interestsFactory)

                InterestSelectionScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    userId = current.user.id,
                    viewModel = interestsViewModel,
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
                    onContinue = {
                        // Ya no recibe selections, el ViewModel lo maneja
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
                        },
                        onNavigateToCaptainsLog = {
                            destination = AppDestination.CaptainsLog(current.user)
                        },
                        onNavigateToAres = {
                            destination = AppDestination.Ares(current.user)
                        }
                    )
                }
            }
            is AppDestination.StellarEmotions -> {
                val context = LocalContext.current
                val viewModel = remember {
                    StellarEmotionsViewModel(
                        userId = current.user.id,
                        repository = com.g22.orbitsoundkotlin.data.OfflineFirstEmotionRepository(context),
                        context = context
                    )
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
                // ✅ LOCAL STORAGE: Initialize Room database and cache repository
                val database = remember { AppDatabase.getInstance(context) }
                val libraryCacheRepo = remember { LibraryCacheRepository(database.libraryCacheDao()) }
                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return LibraryViewModel(
                                libraryCacheRepo = libraryCacheRepo,
                                userId = current.user.id
                            ) as T
                        }
                    }
                )
                
                // Load user's emotions and refresh recommendations when entering library
                LaunchedEffect(Unit) {
                    val userId = current.user.id
                    if (userId.isNotEmpty()) {
                        libraryViewModel.loadUserEmotionsAndRefresh(userId)
                    }
                }
                
                LibraryScreen(
                    viewModel = libraryViewModel,
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
            is AppDestination.Ares -> {
                val context = LocalContext.current
                val factory = remember {
                    com.g22.orbitsoundkotlin.ui.viewmodels.AresViewModelFactory(
                        context = context,
                        userId = current.user.id
                    )
                }
                val aresViewModel: com.g22.orbitsoundkotlin.ui.viewmodels.AresViewModel = viewModel(factory = factory)
                com.g22.orbitsoundkotlin.ui.screens.ares.AresScreen(
                    viewModel = aresViewModel,
                    onBack = {
                        destination = AppDestination.Home(current.user)
                    }
                )
            }
            is AppDestination.CaptainsLog -> {
                val context = LocalContext.current
                val pinStorage = remember { ActivityStatsPinStorage(context) }
                
                // Estado del flujo de PIN
                var pinState by remember { mutableStateOf<PinState?>(null) }
                
                // Determinar estado inicial
                LaunchedEffect(Unit) {
                    pinState = if (pinStorage.hasPin()) {
                        PinState.Unlock
                    } else {
                        PinState.Setup
                    }
                }
                
                when (val state = pinState) {
                    null -> {
                        // Loading state
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color(0xFF010B19))
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = androidx.compose.ui.graphics.Color(0xFF5099BA)
                            )
                        }
                    }
                    PinState.Setup -> {
                        ActivityStatsPinSetupScreen(
                            pinStorage = pinStorage,
                            onPinSet = {
                                pinState = PinState.Unlocked
                            },
                            onBack = {
                                destination = AppDestination.Home(current.user)
                            }
                        )
                    }
                    PinState.Unlock -> {
                        ActivityStatsPinUnlockScreen(
                            pinStorage = pinStorage,
                            onUnlocked = {
                                pinState = PinState.Unlocked
                            },
                            onForgotPin = {
                                pinState = PinState.Reset
                            },
                            onBack = {
                                destination = AppDestination.Home(current.user)
                            }
                        )
                    }
                    PinState.Reset -> {
                        ActivityStatsPinResetScreen(
                            onPinReset = {
                                pinStorage.clearPin()
                                pinState = PinState.Setup
                            },
                            onCancel = {
                                pinState = PinState.Unlock
                            }
                        )
                    }
                    PinState.Unlocked -> {
                        ActivityStatsScreen(
                            onBack = {
                                destination = AppDestination.Home(current.user)
                            }
                        )
                    }
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
    data class StellarEmotions(val user: AuthUser) : AppDestination
    data class Constellations(val user: AuthUser) : AppDestination
    data class Library(val user: AuthUser) : AppDestination
    data class Profile(val user: AuthUser) : AppDestination
    data class CaptainsLog(val user: AuthUser) : AppDestination
    data class Ares(val user: AuthUser) : AppDestination
}

/**
 * Estados del flujo de PIN para Activity Stats.
 */
private enum class PinState {
    Setup,      // Primera vez: configurar PIN
    Unlock,     // Entradas posteriores: desbloquear con PIN
    Reset,      // Reset: confirmar contraseña
    Unlocked    // Desbloqueado: mostrar ActivityStatsScreen
}
