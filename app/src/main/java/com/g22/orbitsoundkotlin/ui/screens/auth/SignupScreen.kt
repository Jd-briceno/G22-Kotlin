package com.g22.orbitsoundkotlin.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.ui.theme.EncodeSansExpanded
import com.g22.orbitsoundkotlin.ui.theme.RobotoMono
import com.g22.orbitsoundkotlin.ui.viewmodels.AuthViewModel

@Composable
fun SignupScreen(
    viewModel: AuthViewModel = viewModel(),
    onAuthenticated: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val callbacks = LocalAuthScreenCallbacks.current

    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.authSuccess) {
        if (uiState.authSuccess) {
            onAuthenticated()
            viewModel.consumeSuccess()
        }
    }

    val backgroundColor = Color(0xFF010B19)
    val baseColor = Color(0xFFB4B1B8).copy(alpha = 0.3f)
    val focusColor = Color(0xFF0095FC)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_tentative),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            ) // REQUIRED_IMAGE: app_logo.png

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Create Your Account",
                style = TextStyle(
                    fontFamily = EncodeSansExpanded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            val genericError = uiState.genericError
            if (genericError != null) {
                InlineErrorBanner(
                    message = genericError,
                    onDismiss = viewModel::clearGenericError
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedAuthTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = "Your Name",
                baseColor = baseColor,
                focusColor = focusColor,
                enabled = !uiState.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedAuthTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = "Your Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                enabled = !uiState.isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                )
            )

            FieldErrorText(uiState.emailError)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedAuthTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                onTrailingIconClick = { showPassword = !showPassword },
                enabled = !uiState.isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )

            FieldErrorText(uiState.passwordError)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedAuthTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = "Confirm Password",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = if (showConfirmPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                onTrailingIconClick = { showConfirmPassword = !showConfirmPassword },
                enabled = !uiState.isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation()
            )

            FieldErrorText(uiState.confirmPasswordError)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::signup,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = focusColor),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = TextStyle(
                            fontFamily = RobotoMono,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "or continue with",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Divider(
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SocialLoginButton(
                    iconRes = R.drawable.google,
                    contentDescription = "Sign up with Google",
                    onClick = callbacks.onGoogleSignIn,
                    enabled = !uiState.isLoading,
                    baseColor = baseColor
                ) // REQUIRED_IMAGE: google.png
                SocialLoginButton(
                    iconRes = R.drawable.apple,
                    contentDescription = "Sign up with Apple",
                    onClick = callbacks.onAppleSignIn,
                    enabled = !uiState.isLoading,
                    baseColor = baseColor
                ) // REQUIRED_IMAGE: apple.png
                SocialLoginButton(
                    iconRes = R.drawable.spotify_logo,
                    contentDescription = "Sign up with Spotify",
                    onClick = callbacks.onSpotifySignIn,
                    enabled = !uiState.isLoading,
                    baseColor = baseColor
                ) // REQUIRED_IMAGE: spotify_logo.png
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "Sign In",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .clickable(enabled = !uiState.isLoading, onClick = callbacks.navigateToLogin),
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = focusColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
