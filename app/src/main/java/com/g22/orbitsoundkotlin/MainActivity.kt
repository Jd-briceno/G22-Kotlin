package com.g22.orbitsoundkotlin

import com.g22.orbitsoundkotlin.ui.viewmodels.StellarEmotionsViewModel
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
                        }
                    )
                }
            }
            is AppDestination.StellarEmotions -> {
                val context = LocalContext.current
                val viewModel = remember {
                    StellarEmotionsViewModel(userId = current.user.id, context = context)
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
            is AppDestination.CaptainsLog -> {
                // TODO: Implementar CaptainsLogScreen en el BLOQUE VIEW
                // Por ahora, mostramos un placeholder temporal que procesa datos para verificar la FEATURE
                val context = LocalContext.current
                val userId = current.user.id
                val userEmail = current.user.email
                val viewModel: CaptainsLogViewModel = viewModel(
                    factory = CaptainsLogViewModelFactory(context, userId, userEmail)
                )
                val uiState by viewModel.uiState.collectAsState()
                
                // Cargar logs al entrar
                LaunchedEffect(Unit) {
                    viewModel.loadSessionLogs(ActivityPeriod.WEEK)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = "Activity Stats - Feature Test",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (uiState.isLoading) {
                            androidx.compose.material3.CircularProgressIndicator()
                            androidx.compose.material3.Text(
                                text = "Procesando datos...",
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        } else {
                            androidx.compose.material3.Text(
                                text = "Sesiones encontradas: ${uiState.sessionLogs.size}",
                                color = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            if (uiState.error != null) {
                                androidx.compose.material3.Text(
                                    text = "Error: ${uiState.error}",
                                    color = androidx.compose.ui.graphics.Color.Red,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            
                            if (uiState.sessionLogs.isEmpty()) {
                                androidx.compose.material3.Text(
                                    text = "No se encontraron sesiones.\n\nVerifica:\n- login_telemetry tiene logins exitosos\n- outbox tiene operaciones\n- search_history tiene búsquedas",
                                    color = androidx.compose.ui.graphics.Color.Yellow,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            } else {
                                androidx.compose.material3.Text(
                                    text = "✅ FEATURE FUNCIONANDO!\n\nRevisa la tabla session_activity_logs\nen Database Inspector",
                                    color = androidx.compose.ui.graphics.Color.Green,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                                
                                // Mostrar detalles de las primeras 3 sesiones
                                uiState.sessionLogs.take(3).forEachIndexed { index, log ->
                                    androidx.compose.material3.Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        androidx.compose.material3.Text(
                                            text = "Sesión ${index + 1}:\n" +
                                                    "Duración: ${log.durationMinutes} min\n" +
                                                    "Acciones: ${log.totalActions}\n" +
                                                    "Búsquedas: ${log.totalSearches}\n" +
                                                    "Login: ${log.loginType}",
                                            color = androidx.compose.ui.graphics.Color.Black,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }
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
}
