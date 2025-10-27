# Estructura de Screens - MVVM

## Estado de migración

- ✅ library/ - LibraryScreen migrado a MVVM
- ✅ profile/ - ProfileScreen migrado a MVVM
- ✅ stellaremotions/ - Ya usa MVVM (StellarEmotionsViewModel en raíz del proyecto)
- ⏳ auth/ - Pendiente migración (LoginScreen, SignupScreen, InterestSelectionScreen)
- ⏳ home/ - Pendiente migración (HomeScreen - ViewModel existe pero necesita refactor)
- ⏳ constellations/ - Pendiente migración
- ⏳ genre/ - Pendiente migración

## Archivos actuales (no migrados aún)

Estos archivos permanecen en `ui/screens/` hasta su migración:
- HomeScreen.kt
- HomeComponents.kt
- LoginScreen.kt
- SignupScreen.kt
- InterestSelectionScreen.kt
- ConstellationsScreen.kt
- GenreSelectorScreen.kt
- StellarEmotionsScreen.kt

`StellarEmotionsViewModel.kt` está en la raíz del proyecto.

## Patrón para nuevas migraciones

Cada screen debe seguir esta estructura al migrar:

```
feature/
├── FeatureScreen.kt      # UI Composable
└── FeatureViewModel.kt   # Lógica + Estado
```

### Template de ViewModel:

```kotlin
package com.g22.orbitsoundkotlin.ui.screens.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeatureViewModel(
    private val service: Service = Service()
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FeatureUiState())
    val uiState: StateFlow<FeatureUiState> = _uiState.asStateFlow()
    
    // Funciones públicas para la UI
    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val data = service.fetchData()
                _uiState.update { it.copy(
                    data = data,
                    loading = false
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    loading = false,
                    error = e.message
                )}
            }
        }
    }
    
    data class FeatureUiState(
        val data: List<Data> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null
    )
}
```

### Template de Screen:

```kotlin
package com.g22.orbitsoundkotlin.ui.screens.feature

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // UI que reacciona a uiState
    Column {
        if (uiState.loading) {
            LoadingIndicator()
        }
        
        if (uiState.error != null) {
            ErrorMessage(uiState.error)
        }
        
        LazyColumn {
            items(uiState.data) { item ->
                ItemCard(item)
            }
        }
    }
}
```

### Pasos para migrar una pantalla:

1. Crear carpeta en `ui/screens/nombrefeature/`
2. Crear `NombreFeatureViewModel.kt` con lógica de negocio
3. Copiar `NombreFeatureScreen.kt` a la nueva carpeta
4. Cambiar package del Screen
5. Agregar parámetro `viewModel` en la firma del Screen
6. Eliminar estado local (remember, mutableStateOf, LaunchedEffect con lógica)
7. Reemplazar referencias a estado local por `uiState.property`
8. Reemplazar llamadas a funciones/servicios por `viewModel.function()`
9. Eliminar el archivo antiguo
10. Actualizar imports en MainActivity y otros archivos

## Próximas migraciones sugeridas

**Sprint 2:**
- ConstellationsScreen + ConstellationsViewModel
- GenreSelectorScreen + GenreSelectorViewModel

**Sprint 3:**
- AuthViewModel (compartido)
- LoginScreen → usa AuthViewModel
- SignupScreen → usa AuthViewModel
- InterestSelectionScreen → usa AuthViewModel

**Sprint 4:**
- Refactorizar HomeViewModel (ya existe pero necesita mejoras)
- Mover StellarEmotionsViewModel y Screen a stellaremotions/
- MainViewModel para navegación
- Simplificar MainActivity

## Patrones de diseño implementados

### 1. SINGLETON - SpotifyService

**SpotifyService** usa el patrón Singleton para:
- Reutilizar una única instancia en toda la app
- Cachear tokens de acceso (evita llamadas innecesarias a la API)
- Mejor performance y gestión de recursos

**Uso:**
```kotlin
val spotifyService = SpotifyService.getInstance()
```

### 2. MAPPER - Transformación de datos

**SpotifyTrackMapper** separa la lógica de parseo del modelo:
- Los modelos (Track, Playlist) solo contienen datos
- Los mappers contienen lógica de transformación
- Cumple con Single Responsibility Principle

**Ver:** `data/mappers/README.md` para más detalles

### 3. MVVM - Arquitectura

- **Model:** Track, Playlist, Emotion (solo datos)
- **View:** LibraryScreen, ProfileScreen (Composables)
- **ViewModel:** LibraryViewModel, ProfileViewModel (lógica + estado)

## Notas importantes

- NO usar Hilt por ahora (DI manual con parámetros por defecto)
- SpotifyService usa Singleton: `SpotifyService.getInstance()`
- Los modelos NO deben tener lógica (usar mappers)
- Seguir el patrón establecido en LibraryViewModel
- Mantener la app funcional durante todo el proceso
- Migrar una pantalla a la vez
- Testear manualmente después de cada migración

