# Plan de Migración Completa a MVVM - Orbit Sound

## 📋 Índice

1. [Estado Actual](#estado-actual)
2. [Sprints de Migración](#sprints-de-migración)
3. [Sprint 2: Pantallas Simples](#sprint-2-pantallas-simples-1-2-semanas)
4. [Sprint 3: Módulo de Autenticación](#sprint-3-módulo-de-autenticación-2-3-semanas)
5. [Sprint 4: HomeScreen y StellarEmotions](#sprint-4-homescreen-y-stellaremotions-1-2-semanas)
6. [Sprint 5: MainActivity y Navegación](#sprint-5-mainactivity-y-navegación-2-3-semanas)
7. [Sprint 6: Optimizaciones Finales](#sprint-6-optimizaciones-finales-1-semana)
8. [Checklist Final](#checklist-final)

---

## 🎯 Estado Actual

### ✅ Completado

- **LibraryScreen** + LibraryViewModel (MVVM completo)
- **ProfileScreen** + ProfileViewModel (MVVM completo)
- **SpotifyService** (patrón Singleton implementado)
- **SpotifyTrackMapper** (patrón Mapper implementado)
- **Track.kt** refactorizado (modelo puro sin lógica)
- Estructura de carpetas creada para todas las features

### ⏳ Pendiente de Migración

1. ConstellationsScreen
2. GenreSelectorScreen
3. LoginScreen, SignupScreen, InterestSelectionScreen (módulo auth)
4. HomeScreen (necesita refactor del ViewModel existente)
5. StellarEmotionsScreen (mover a carpeta correcta)
6. MainActivity (simplificación y separación de navegación)

### 📦 Patrones Implementados

- ✅ **MVVM** (parcial: Library, Profile)
- ✅ **Singleton** (SpotifyService)
- ✅ **Mapper** (SpotifyTrackMapper)
- ⏳ **Repository** (EmotionRepository existe, necesita expansión)

---

## 🚀 Sprints de Migración

### Resumen de Prioridades

| Sprint | Duración | Pantallas | Complejidad |
|--------|----------|-----------|-------------|
| Sprint 2 | 1-2 semanas | Constellations, GenreSelector | Baja ⭐ |
| Sprint 3 | 2-3 semanas | Login, Signup, InterestSelection | Media ⭐⭐ |
| Sprint 4 | 1-2 semanas | Home, StellarEmotions | Media ⭐⭐ |
| Sprint 5 | 2-3 semanas | MainActivity, Navegación | Alta ⭐⭐⭐ |
| Sprint 6 | 1 semana | Testing, Optimización, DI | Media ⭐⭐ |

**Tiempo estimado total:** 7-11 semanas

---

## 📱 Sprint 2: Pantallas Simples (1-2 semanas) ⭐

### Objetivo
Migrar las pantallas independientes más simples que no tienen dependencias complejas.

---

### 🔹 Paso 1: ConstellationsScreen

#### Estado Actual
- Archivo: `ui/screens/ConstellationsScreen.kt`
- Maneja selección de constelaciones
- Muestra toast messages
- Tiene estado local (selectedConstellations)

#### Qué hacer:

1. **Crear ConstellationsViewModel.kt**
   
   Ubicación: `ui/screens/constellations/ConstellationsViewModel.kt`
   
   ```kotlin
   package com.g22.orbitsoundkotlin.ui.screens.constellations
   
   import androidx.lifecycle.ViewModel
   import kotlinx.coroutines.flow.MutableStateFlow
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.asStateFlow
   import kotlinx.coroutines.flow.update
   
   class ConstellationsViewModel : ViewModel() {
       
       private val _uiState = MutableStateFlow(ConstellationsUiState())
       val uiState: StateFlow<ConstellationsUiState> = _uiState.asStateFlow()
       
       fun toggleConstellation(constellation: String) {
           _uiState.update { state ->
               val current = state.selectedConstellations
               val updated = if (current.contains(constellation)) {
                   current - constellation
               } else {
                   current + constellation
               }
               state.copy(selectedConstellations = updated)
           }
       }
       
       fun saveConstellations() {
           val count = _uiState.value.selectedConstellations.size
           _uiState.update { 
               it.copy(
                   toastMessage = "Guardadas $count constelaciones",
                   showToast = true
               )
           }
       }
       
       fun clearToast() {
           _uiState.update { it.copy(showToast = false, toastMessage = null) }
       }
       
       data class ConstellationsUiState(
           val selectedConstellations: Set<String> = emptySet(),
           val showToast: Boolean = false,
           val toastMessage: String? = null
       )
   }
   ```

2. **Migrar ConstellationsScreen.kt**
   
   - Mover archivo a: `ui/screens/constellations/ConstellationsScreen.kt`
   - Cambiar package: `package com.g22.orbitsoundkotlin.ui.screens.constellations`
   - Agregar parámetro ViewModel
   - Eliminar estado local con `remember { mutableStateOf(...) }`
   - Reemplazar con `uiState.selectedConstellations`, etc.

3. **Actualizar MainActivity.kt**
   
   - Cambiar import: `import com.g22.orbitsoundkotlin.ui.screens.constellations.ConstellationsScreen`

4. **Eliminar archivo antiguo**
   
   - Eliminar: `ui/screens/ConstellationsScreen.kt`

#### Archivos modificados:
- ✅ `ui/screens/constellations/ConstellationsViewModel.kt` (NUEVO)
- ✅ `ui/screens/constellations/ConstellationsScreen.kt` (MOVIDO + REFACTOR)
- ✅ `MainActivity.kt` (import actualizado)
- ❌ `ui/screens/ConstellationsScreen.kt` (ELIMINADO)

---

### 🔹 Paso 2: GenreSelectorScreen

#### Estado Actual
- Archivo: `ui/screens/GenreSelectorScreen.kt`
- Maneja selección de géneros musicales
- Tiene lógica de validación (mínimo/máximo géneros seleccionados)
- Muestra mensajes de error

#### Qué hacer:

1. **Crear GenreSelectorViewModel.kt**
   
   Ubicación: `ui/screens/genre/GenreSelectorViewModel.kt`
   
   ```kotlin
   package com.g22.orbitsoundkotlin.ui.screens.genre
   
   import androidx.lifecycle.ViewModel
   import kotlinx.coroutines.flow.MutableStateFlow
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.asStateFlow
   import kotlinx.coroutines.flow.update
   
   class GenreSelectorViewModel : ViewModel() {
       
       private val _uiState = MutableStateFlow(GenreSelectorUiState())
       val uiState: StateFlow<GenreSelectorUiState> = _uiState.asStateFlow()
       
       companion object {
           const val MIN_GENRES = 3
           const val MAX_GENRES = 5
       }
       
       fun toggleGenre(genre: String) {
           _uiState.update { state ->
               val current = state.selectedGenres
               val updated = if (current.contains(genre)) {
                   current - genre
               } else {
                   if (current.size < MAX_GENRES) {
                       current + genre
                   } else {
                       state.copy(
                           errorMessage = "Máximo $MAX_GENRES géneros"
                       )
                       return@update state
                   }
               }
               state.copy(
                   selectedGenres = updated,
                   errorMessage = null
               )
           }
       }
       
       fun canProceed(): Boolean {
           return _uiState.value.selectedGenres.size >= MIN_GENRES
       }
       
       fun validateAndProceed(onSuccess: () -> Unit) {
           val count = _uiState.value.selectedGenres.size
           when {
               count < MIN_GENRES -> {
                   _uiState.update { 
                       it.copy(errorMessage = "Selecciona al menos $MIN_GENRES géneros")
                   }
               }
               count > MAX_GENRES -> {
                   _uiState.update { 
                       it.copy(errorMessage = "Máximo $MAX_GENRES géneros")
                   }
               }
               else -> {
                   _uiState.update { it.copy(errorMessage = null) }
                   onSuccess()
               }
           }
       }
       
       data class GenreSelectorUiState(
           val selectedGenres: Set<String> = emptySet(),
           val errorMessage: String? = null
       )
   }
   ```

2. **Migrar GenreSelectorScreen.kt**
   
   - Mover archivo a: `ui/screens/genre/GenreSelectorScreen.kt`
   - Cambiar package
   - Agregar ViewModel
   - Eliminar estado local
   - Usar `uiState` y funciones del ViewModel

3. **Actualizar MainActivity.kt**
   
   - Cambiar import: `import com.g22.orbitsoundkotlin.ui.screens.genre.GenreSelectorScreen`

4. **Eliminar archivo antiguo**
   
   - Eliminar: `ui/screens/GenreSelectorScreen.kt`

#### Archivos modificados:
- ✅ `ui/screens/genre/GenreSelectorViewModel.kt` (NUEVO)
- ✅ `ui/screens/genre/GenreSelectorScreen.kt` (MOVIDO + REFACTOR)
- ✅ `MainActivity.kt` (import actualizado)
- ❌ `ui/screens/GenreSelectorScreen.kt` (ELIMINADO)

---

### ✅ Checklist Sprint 2

- [ ] ConstellationsViewModel creado
- [ ] ConstellationsScreen migrado y funcionando
- [ ] GenreSelectorViewModel creado
- [ ] GenreSelectorScreen migrado y funcionando
- [ ] Imports actualizados en MainActivity
- [ ] Archivos antiguos eliminados
- [ ] Testing manual de ambas pantallas
- [ ] Actualizar README.md de screens con el progreso

---

## 🔐 Sprint 3: Módulo de Autenticación (2-3 semanas) ⭐⭐

### Objetivo
Crear un **AuthViewModel compartido** para las tres pantallas de autenticación y migrarlas a MVVM.

---

### 🔹 Paso 1: Crear AuthViewModel compartido

#### Análisis de AuthService.kt actual

**Responsabilidades de AuthService:**
- Autenticación con Firebase (email/password, Google)
- Registro de usuarios
- Verificación de sesión
- Gestión de perfil en Firestore

**Problema:** Se usa directamente en los Composables.

**Solución:** Crear AuthViewModel que encapsule AuthService.

#### Qué hacer:

1. **Crear AuthViewModel.kt**
   
   Ubicación: `ui/screens/auth/AuthViewModel.kt`
   
   ```kotlin
   package com.g22.orbitsoundkotlin.ui.screens.auth
   
   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import com.g22.orbitsoundkotlin.auth.AuthService
   import kotlinx.coroutines.flow.MutableStateFlow
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.asStateFlow
   import kotlinx.coroutines.flow.update
   import kotlinx.coroutines.launch
   
   class AuthViewModel(
       private val authService: AuthService = AuthService()
   ) : ViewModel() {
       
       private val _uiState = MutableStateFlow(AuthUiState())
       val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
       
       init {
           checkAuthStatus()
       }
       
       private fun checkAuthStatus() {
           viewModelScope.launch {
               val isAuthenticated = authService.isUserLoggedIn()
               _uiState.update { it.copy(
                   isAuthenticated = isAuthenticated,
                   isLoading = false
               )}
           }
       }
       
       fun loginWithEmail(email: String, password: String) {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true, errorMessage = null) }
               try {
                   authService.loginWithEmail(email, password)
                   _uiState.update { it.copy(
                       isAuthenticated = true,
                       isLoading = false
                   )}
               } catch (e: Exception) {
                   _uiState.update { it.copy(
                       isLoading = false,
                       errorMessage = e.message ?: "Error al iniciar sesión"
                   )}
               }
           }
       }
       
       fun loginWithGoogle(tokenId: String) {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true, errorMessage = null) }
               try {
                   authService.loginWithGoogle(tokenId)
                   _uiState.update { it.copy(
                       isAuthenticated = true,
                       isLoading = false
                   )}
               } catch (e: Exception) {
                   _uiState.update { it.copy(
                       isLoading = false,
                       errorMessage = e.message ?: "Error con Google"
                   )}
               }
           }
       }
       
       fun signupWithEmail(email: String, password: String, username: String) {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true, errorMessage = null) }
               try {
                   authService.signupWithEmail(email, password, username)
                   _uiState.update { it.copy(
                       isAuthenticated = true,
                       isLoading = false
                   )}
               } catch (e: Exception) {
                   _uiState.update { it.copy(
                       isLoading = false,
                       errorMessage = e.message ?: "Error al registrarse"
                   )}
               }
           }
       }
       
       fun saveUserInterests(genres: Set<String>, constellations: Set<String>) {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true) }
               try {
                   authService.saveUserPreferences(genres, constellations)
                   _uiState.update { it.copy(
                       onboardingComplete = true,
                       isLoading = false
                   )}
               } catch (e: Exception) {
                   _uiState.update { it.copy(
                       isLoading = false,
                       errorMessage = e.message ?: "Error al guardar preferencias"
                   )}
               }
           }
       }
       
       fun logout() {
           viewModelScope.launch {
               authService.logout()
               _uiState.update { AuthUiState() }
           }
       }
       
       fun clearError() {
           _uiState.update { it.copy(errorMessage = null) }
       }
       
       data class AuthUiState(
           val isAuthenticated: Boolean = false,
           val isLoading: Boolean = true,
           val onboardingComplete: Boolean = false,
           val errorMessage: String? = null
       )
   }
   ```

---

### 🔹 Paso 2: Migrar LoginScreen

#### Qué hacer:

1. **Mover LoginScreen.kt**
   
   - De: `ui/screens/LoginScreen.kt`
   - A: `ui/screens/auth/login/LoginScreen.kt`
   - Cambiar package: `package com.g22.orbitsoundkotlin.ui.screens.auth.login`

2. **Refactorizar LoginScreen**
   
   - Agregar parámetro: `viewModel: AuthViewModel = viewModel()`
   - Eliminar estado local de email, password, isLoading
   - Usar `uiState.isLoading`, `uiState.errorMessage`
   - Reemplazar llamadas a AuthService por `viewModel.loginWithEmail()`, etc.

3. **Actualizar MainActivity**
   
   - Cambiar import: `import com.g22.orbitsoundkotlin.ui.screens.auth.login.LoginScreen`
   - Considerar compartir la instancia de AuthViewModel entre pantallas de auth

---

### 🔹 Paso 3: Migrar SignupScreen

#### Qué hacer:

1. **Mover SignupScreen.kt**
   
   - De: `ui/screens/SignupScreen.kt`
   - A: `ui/screens/auth/signup/SignupScreen.kt`
   - Cambiar package

2. **Refactorizar SignupScreen**
   
   - Usar el mismo AuthViewModel compartido
   - Eliminar estado local
   - Usar `viewModel.signupWithEmail()`

3. **Actualizar MainActivity**
   
   - Cambiar import

---

### 🔹 Paso 4: Migrar InterestSelectionScreen

#### Qué hacer:

1. **Mover InterestSelectionScreen.kt**
   
   - De: `ui/screens/InterestSelectionScreen.kt`
   - A: `ui/screens/auth/interests/InterestSelectionScreen.kt`
   - Cambiar package

2. **Refactorizar InterestSelectionScreen**
   
   - Usar AuthViewModel para guardar preferencias
   - Puede reutilizar ConstellationsViewModel y GenreSelectorViewModel para la selección
   - Usar `viewModel.saveUserInterests()`

3. **Actualizar MainActivity**
   
   - Cambiar import

---

### ✅ Checklist Sprint 3

- [ ] AuthService revisado y analizado
- [ ] AuthViewModel creado con todas las funciones de autenticación
- [ ] LoginScreen migrado a auth/login/
- [ ] SignupScreen migrado a auth/signup/
- [ ] InterestSelectionScreen migrado a auth/interests/
- [ ] AuthViewModel compartido correctamente entre pantallas
- [ ] Imports actualizados en MainActivity
- [ ] Archivos antiguos eliminados
- [ ] Testing manual del flujo completo de auth
- [ ] Actualizar README.md

---

## 🏠 Sprint 4: HomeScreen y StellarEmotions (1-2 semanas) ⭐⭐

### Objetivo
Refactorizar HomeScreen (que ya tiene un ViewModel antiguo) y mover StellarEmotionsScreen a su carpeta correcta.

---

### 🔹 Paso 1: Refactorizar HomeViewModel

#### Estado Actual
Existe un HomeViewModel pero probablemente usa patrones antiguos (Observer personalizado en lugar de StateFlow).

#### Qué hacer:

1. **Leer HomeViewModel actual**
   
   - Ubicación: Buscar donde esté definido
   - Analizar el código y sus dependencias

2. **Refactorizar a StateFlow**
   
   - Reemplazar cualquier Observer personalizado por StateFlow/SharedFlow
   - Seguir el patrón de LibraryViewModel
   - Usar `viewModelScope.launch` para operaciones asíncronas

3. **Mover a carpeta correcta**
   
   - Mover a: `ui/screens/home/HomeViewModel.kt`
   - Cambiar package

---

### 🔹 Paso 2: Migrar HomeScreen

#### Qué hacer:

1. **Mover HomeScreen.kt y HomeComponents.kt**
   
   - De: `ui/screens/HomeScreen.kt`
   - A: `ui/screens/home/HomeScreen.kt`
   - De: `ui/screens/HomeComponents.kt`
   - A: `ui/screens/home/HomeComponents.kt` o `ui/screens/shared/HomeComponents.kt`

2. **Refactorizar HomeScreen**
   
   - Agregar parámetro ViewModel
   - Eliminar estado local
   - Usar `uiState` del ViewModel refactorizado

3. **Actualizar imports en MainActivity**

---

### 🔹 Paso 3: Mover StellarEmotionsScreen

#### Estado Actual
- StellarEmotionsViewModel.kt está en la raíz del proyecto
- StellarEmotionsScreen.kt está en `ui/screens/`
- Ya sigue MVVM correctamente

#### Qué hacer:

1. **Mover ambos archivos**
   
   - StellarEmotionsViewModel.kt → `ui/screens/stellaremotions/StellarEmotionsViewModel.kt`
   - StellarEmotionsScreen.kt → `ui/screens/stellaremotions/StellarEmotionsScreen.kt`

2. **Actualizar packages**
   
   - Ambos: `package com.g22.orbitsoundkotlin.ui.screens.stellaremotions`

3. **Actualizar imports en MainActivity**
   
   - `import com.g22.orbitsoundkotlin.ui.screens.stellaremotions.StellarEmotionsScreen`

4. **Eliminar archivos antiguos**

---

### ✅ Checklist Sprint 4

- [ ] HomeViewModel refactorizado (StateFlow en lugar de Observer)
- [ ] HomeScreen migrado a home/
- [ ] HomeComponents movido a ubicación correcta
- [ ] StellarEmotionsViewModel movido a stellaremotions/
- [ ] StellarEmotionsScreen movido a stellaremotions/
- [ ] Imports actualizados en MainActivity
- [ ] Archivos antiguos eliminados
- [ ] Testing manual de Home y StellarEmotions
- [ ] Actualizar README.md

---

## 🎯 Sprint 5: MainActivity y Navegación (2-3 semanas) ⭐⭐⭐

### Objetivo
Simplificar MainActivity extrayendo la lógica de navegación, posiblemente implementando Jetpack Navigation Compose.

---

### 🔹 Paso 1: Analizar MainActivity actual

#### Problemas identificados:
- Contiene toda la lógica de navegación
- Maneja autenticación directamente
- Tiene lógica de negocio en Composables
- No usa Navigation Compose

#### Qué hacer:

1. **Leer MainActivity.kt completo**
2. **Identificar:**
   - Todas las rutas de navegación
   - Lógica de negocio que debería estar en ViewModels
   - Estado compartido entre pantallas

---

### 🔹 Paso 2: Implementar Jetpack Navigation

#### Qué hacer:

1. **Agregar dependencia en build.gradle.kts**
   
   ```kotlin
   implementation("androidx.navigation:navigation-compose:2.7.5")
   ```

2. **Crear Navigation.kt**
   
   Ubicación: `ui/navigation/Navigation.kt`
   
   ```kotlin
   package com.g22.orbitsoundkotlin.ui.navigation
   
   import androidx.compose.runtime.Composable
   import androidx.navigation.NavHostController
   import androidx.navigation.compose.NavHost
   import androidx.navigation.compose.composable
   import androidx.navigation.compose.rememberNavController
   
   sealed class Screen(val route: String) {
       object Login : Screen("login")
       object Signup : Screen("signup")
       object Interests : Screen("interests")
       object Home : Screen("home")
       object Library : Screen("library")
       object Profile : Screen("profile")
       object StellarEmotions : Screen("stellar_emotions")
       object Constellations : Screen("constellations")
       object GenreSelector : Screen("genre_selector")
   }
   
   @Composable
   fun OrbitSoundNavigation(
       navController: NavHostController = rememberNavController(),
       startDestination: String
   ) {
       NavHost(
           navController = navController,
           startDestination = startDestination
       ) {
           composable(Screen.Login.route) {
               LoginScreen(
                   onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                   onLoginSuccess = { navController.navigate(Screen.Home.route) }
               )
           }
           
           composable(Screen.Home.route) {
               HomeScreen(
                   onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                   onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
               )
           }
           
           // ... más rutas
       }
   }
   ```

3. **Crear MainViewModel (opcional)**
   
   Para manejar el estado de autenticación global:
   
   ```kotlin
   package com.g22.orbitsoundkotlin
   
   import androidx.lifecycle.ViewModel
   import androidx.lifecycle.viewModelScope
   import com.g22.orbitsoundkotlin.auth.AuthService
   import kotlinx.coroutines.flow.MutableStateFlow
   import kotlinx.coroutines.flow.StateFlow
   import kotlinx.coroutines.flow.asStateFlow
   import kotlinx.coroutines.launch
   
   class MainViewModel(
       private val authService: AuthService = AuthService()
   ) : ViewModel() {
       
       private val _isAuthenticated = MutableStateFlow<Boolean?>(null)
       val isAuthenticated: StateFlow<Boolean?> = _isAuthenticated.asStateFlow()
       
       init {
           checkAuthStatus()
       }
       
       private fun checkAuthStatus() {
           viewModelScope.launch {
               _isAuthenticated.value = authService.isUserLoggedIn()
           }
       }
   }
   ```

4. **Simplificar MainActivity**
   
   ```kotlin
   class MainActivity : ComponentActivity() {
       override fun onCreate(savedInstanceState: Bundle?) {
           super.onCreate(savedInstanceState)
           setContent {
               OrbitSoundTheme {
                   val viewModel: MainViewModel = viewModel()
                   val isAuthenticated by viewModel.isAuthenticated.collectAsState()
                   
                   when (isAuthenticated) {
                       null -> LoadingScreen()
                       true -> OrbitSoundNavigation(startDestination = Screen.Home.route)
                       false -> OrbitSoundNavigation(startDestination = Screen.Login.route)
                   }
               }
           }
       }
   }
   ```

---

### ✅ Checklist Sprint 5

- [ ] Dependencia de Navigation Compose agregada
- [ ] Navigation.kt creado con todas las rutas
- [ ] MainViewModel creado (opcional pero recomendado)
- [ ] MainActivity simplificado
- [ ] Todas las pantallas actualizadas para usar Navigation
- [ ] Testing manual del flujo completo de navegación
- [ ] Verificar que no hay pantallas huérfanas
- [ ] Actualizar README.md

---

## 🔧 Sprint 6: Optimizaciones Finales (1 semana) ⭐⭐

### Objetivo
Pulir la arquitectura, agregar mappers faltantes, y preparar para DI futuro.

---

### 🔹 Paso 1: Crear mappers adicionales

#### Qué hacer:

1. **SpotifyPlaylistMapper**
   
   Si Playlist también se parsea desde JSON, crear:
   - `data/mappers/SpotifyPlaylistMapper.kt`
   - Mover lógica de parseo de Playlist (si existe)

2. **EmotionMapper** (si aplica)
   
   Si hay transformación de datos de emociones entre Firestore y UI.

---

### 🔹 Paso 2: Revisar todos los modelos

#### Qué hacer:

1. **Verificar que NO contengan lógica**
   
   - Track.kt ✅ (ya está limpio)
   - Playlist.kt
   - Emotion.kt
   - EmotionLog.kt
   - UserProfile.kt
   - Friend.kt
   - Constellation.kt
   - Achievement.kt

2. **Mover cualquier lógica a mappers o ViewModels**

---

### 🔹 Paso 3: Documentar patrones de diseño

#### Qué hacer:

1. **Actualizar README principal**
   
   Crear/actualizar `README.md` en la raíz con:
   - Arquitectura MVVM completa
   - Patrones implementados
   - Estructura de carpetas final
   - Cómo agregar nuevas features

2. **Documentar cada patrón**
   
   - Ya existe: `data/mappers/README.md` ✅
   - Agregar: `ui/navigation/README.md`
   - Agregar: `services/README.md` (explicar Singleton)

---

### 🔹 Paso 4: Testing básico (opcional)

#### Qué hacer:

1. **Tests unitarios para ViewModels**
   
   Ejemplo para LibraryViewModel:
   
   ```kotlin
   class LibraryViewModelTest {
       
       @Test
       fun `searchTracks actualiza estado correctamente`() = runTest {
           val viewModel = LibraryViewModel()
           viewModel.searchTracks("lofi")
           
           val state = viewModel.uiState.value
           assert(state.searchResults.isNotEmpty())
       }
   }
   ```

2. **Tests para Mappers**
   
   ```kotlin
   class SpotifyTrackMapperTest {
       
       @Test
       fun `map convierte JSON a Track correctamente`() {
           val mapper = SpotifyTrackMapper()
           val json = mapOf(
               "name" to "Test Song",
               "artists" to listOf(mapOf("name" to "Test Artist"))
               // ...
           )
           
           val track = mapper.map(json)
           assertEquals("Test Song", track.title)
       }
   }
   ```

---

### 🔹 Paso 5: Preparar para Hilt (futuro)

#### Qué hacer:

1. **Documentar plan de migración a Hilt**
   
   Crear: `DI-MIGRATION-PLAN.md`
   
   Contenido:
   - Por qué migrar a Hilt ahora
   - Qué componentes se van a inyectar
   - Módulos de Hilt necesarios
   - Orden de migración

2. **Identificar dependencias para DI**
   
   - SpotifyService (Singleton)
   - AuthService
   - EmotionRepository
   - UserPreferencesRepository
   - Todos los ViewModels

---

### ✅ Checklist Sprint 6

- [ ] Mappers adicionales creados (Playlist, etc.)
- [ ] Todos los modelos revisados y limpios
- [ ] README principal actualizado
- [ ] Documentación de patrones completa
- [ ] Tests unitarios básicos (opcional)
- [ ] Plan de migración a Hilt documentado
- [ ] Código limpio y sin warnings
- [ ] Performance verificada

---

## ✅ Checklist Final: MVVM Completo

### Pantallas

- [x] LibraryScreen + LibraryViewModel
- [x] ProfileScreen + ProfileViewModel
- [ ] ConstellationsScreen + ConstellationsViewModel
- [ ] GenreSelectorScreen + GenreSelectorViewModel
- [ ] LoginScreen + AuthViewModel
- [ ] SignupScreen + AuthViewModel
- [ ] InterestSelectionScreen + AuthViewModel
- [ ] HomeScreen + HomeViewModel (refactorizado)
- [ ] StellarEmotionsScreen + StellarEmotionsViewModel (movidos)

### Servicios

- [x] SpotifyService (Singleton)
- [ ] AuthService (encapsulado en AuthViewModel)
- [x] EmotionRepository (ya existe)
- [x] UserPreferencesRepository (ya existe)

### Modelos

- [x] Track.kt (sin lógica)
- [ ] Playlist.kt (revisar)
- [ ] Emotion.kt (revisar)
- [ ] EmotionLog.kt (revisar)
- [ ] UserProfile.kt (revisar)
- [ ] Friend.kt (revisar)
- [ ] Constellation.kt (revisar)
- [ ] Achievement.kt (revisar)

### Mappers

- [x] SpotifyTrackMapper
- [ ] SpotifyPlaylistMapper (si aplica)
- [ ] EmotionMapper (si aplica)

### Navegación

- [ ] Jetpack Navigation Compose implementado
- [ ] MainActivity simplificado
- [ ] MainViewModel creado

### Documentación

- [x] `ui/screens/README.md`
- [x] `data/mappers/README.md`
- [ ] `README.md` principal actualizado
- [ ] `ui/navigation/README.md`
- [ ] `services/README.md`
- [ ] `DI-MIGRATION-PLAN.md`

### Testing

- [ ] Tests unitarios para ViewModels
- [ ] Tests para Mappers
- [ ] Testing manual completo de todas las pantallas

---

## 📊 Métricas de Progreso

### Actual
- **Pantallas migradas:** 2/9 (22%)
- **Patrones implementados:** 3/4 (MVVM parcial, Singleton, Mapper)
- **Sprints completados:** 1/6

### Objetivo
- **Pantallas migradas:** 9/9 (100%)
- **Patrones implementados:** 4/4 (MVVM completo + DI preparado)
- **Sprints completados:** 6/6

---

## 🎓 Recursos y Referencias

### Documentación oficial
- [Android MVVM Guide](https://developer.android.com/topic/architecture)
- [StateFlow y SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

### Patrones implementados
- **MVVM:** Separación View-ViewModel-Model
- **Singleton:** SpotifyService con instancia única
- **Mapper:** Transformación de datos separada de modelos
- **Repository:** Abstracción de fuentes de datos

### Mejores prácticas aplicadas
- ✅ ViewModels nunca tienen referencias a Views
- ✅ Modelos son data classes sin lógica
- ✅ StateFlow para estado reactivo
- ✅ viewModelScope para coroutines
- ✅ DI manual preparado para Hilt
- ✅ Separación clara de responsabilidades

---

## 📝 Notas Importantes

1. **Mantener la app funcional:** Cada migración debe dejar la app en estado funcional.
2. **Testing manual:** Después de cada sprint, probar manualmente todas las pantallas.
3. **No saltar pasos:** Seguir el orden de sprints para evitar dependencias rotas.
4. **Commits frecuentes:** Hacer commit después de cada pantalla migrada.
5. **Backup:** Tener backup antes de cambios grandes (MainActivity).
6. **DI manual por ahora:** No implementar Hilt hasta terminar Sprint 6.

---

## 🚀 Cómo Empezar

### Paso Inmediato
1. Leer este documento completo
2. Empezar con Sprint 2, Paso 1: ConstellationsScreen
3. Seguir el orden establecido
4. Actualizar este documento con el progreso

### Comando para empezar
```bash
# Crear branch para el sprint 2
git checkout -b feature/mvvm-sprint-2

# Empezar con ConstellationsScreen
mkdir -p app/src/main/java/com/g22/orbitsoundkotlin/ui/screens/constellations
```

---

**Última actualización:** Octubre 2025  
**Versión:** 1.0  
**Estado:** Sprint 1 completado ✅ | Sprint 2 pendiente ⏳

