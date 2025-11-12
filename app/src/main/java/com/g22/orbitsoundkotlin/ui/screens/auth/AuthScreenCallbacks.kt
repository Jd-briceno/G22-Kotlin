package com.g22.orbitsoundkotlin.ui.screens.auth

import androidx.compose.runtime.staticCompositionLocalOf

data class AuthScreenCallbacks(
    val navigateToLogin: () -> Unit = {},
    val navigateToSignup: () -> Unit = {},
    val onForgotPassword: (String) -> Unit = {},
    val onGoogleSignIn: () -> Unit = {},
    val onAppleSignIn: () -> Unit = {},
    val onSpotifySignIn: () -> Unit = {}
)

val LocalAuthScreenCallbacks = staticCompositionLocalOf { AuthScreenCallbacks() }
