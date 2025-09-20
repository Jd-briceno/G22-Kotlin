package com.g22.orbitsoundkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.g22.orbitsoundkotlin.ui.screens.GenreSelectorScreen
import com.g22.orbitsoundkotlin.ui.screens.HomeScreen
import com.g22.orbitsoundkotlin.ui.screens.LibraryScreen
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrbitSoundKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // VIEW 1 CARLOS PENUELA
                    HomeScreen()
                    // VIEW 2 CARLOS PENUELA
                    GenreSelectorScreen()
                    // VIEW 3 SANTIAGO OSORIO
                    // LibraryScreen()
                }
            }
        }
    }
}
