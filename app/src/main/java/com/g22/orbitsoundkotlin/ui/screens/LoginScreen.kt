package com.g22.orbitsoundkotlin.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.ui.theme.EncodeSansExpanded
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import com.g22.orbitsoundkotlin.ui.theme.RobotoMono

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    initialEmail: String = "",
    initialRememberMe: Boolean = false,
    showSocialProviders: Boolean = true,
    onSignIn: (email: String, password: String, rememberMe: Boolean) -> Unit = { _, _, _ -> },
    onForgotPassword: (email: String) -> Unit = { _ -> },
    onNavigateToSignUp: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onAppleSignIn: () -> Unit = {},
    onSpotifySignIn: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF010B19)
    val baseColor = Color(0xFFB4B1B8).copy(alpha = 0.3f)
    val focusColor = Color(0xFF0095FC)

    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(initialRememberMe) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(initialEmail) { email = initialEmail }
    LaunchedEffect(initialRememberMe) { rememberMe = initialRememberMe }

    Box(
        modifier = modifier
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
            Spacer(modifier = Modifier.height(60.dp))

            // ACA VA UNA IMAGEN USADA!!!
            Image(
                painter = painterResource(id = R.drawable.logo_tentative),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Login to Your Account",
                style = TextStyle(
                    fontFamily = EncodeSansExpanded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedInputField(
                value = email,
                onValueChange = { email = it },
                label = "Your Email",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                enabled = !isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedInputField(
                value = password,
                onValueChange = { password = it },
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
                enabled = !isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = focusColor,
                        uncheckedColor = Color.White.copy(alpha = 0.7f),
                        checkmarkColor = Color.White
                    )
                )
                Text(
                    text = "Remember me",
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSignIn(email.trim(), password.trim(), rememberMe) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = focusColor),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Sign In",
                        style = TextStyle(
                            fontFamily = RobotoMono,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onForgotPassword(email.trim()) },
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "Forgot Password?",
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showSocialProviders) {
                Spacer(modifier = Modifier.height(16.dp))

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
                        contentDescription = "Sign in with Google",
                        onClick = onGoogleSignIn,
                        enabled = !isLoading,
                        baseColor = baseColor
                    )
                    SocialLoginButton(
                        iconRes = R.drawable.apple,
                        contentDescription = "Sign in with Apple",
                        onClick = onAppleSignIn,
                        enabled = !isLoading,
                        baseColor = baseColor
                    )
                    SocialLoginButton(
                        iconRes = R.drawable.spotify_logo,
                        contentDescription = "Sign in with Spotify",
                        onClick = onSpotifySignIn,
                        enabled = !isLoading,
                        baseColor = baseColor
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = "Sign Up",
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(Color.Transparent)
                        .clickable(enabled = !isLoading, onClick = onNavigateToSignUp),
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

@Composable
private fun OutlinedInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    baseColor: Color,
    focusColor: Color,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Surface(color = Color.Transparent) {
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = RobotoMono,
                color = Color.White
            ),
            singleLine = true,
            label = {
                Text(
                    text = label,
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = when {
                trailingIcon != null && onTrailingIconClick != null -> {
                    {
                        IconButton(onClick = onTrailingIconClick, enabled = enabled) {
                            trailingIcon()
                        }
                    }
                }
                else -> trailingIcon
            },
            shape = RoundedCornerShape(14.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = focusColor,
                unfocusedIndicatorColor = baseColor,
                disabledIndicatorColor = baseColor,
                focusedLabelColor = Color.White.copy(alpha = 0.7f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                disabledLabelColor = Color.White.copy(alpha = 0.5f),
                focusedContainerColor = focusColor.copy(alpha = 0.3f),
                unfocusedContainerColor = baseColor,
                disabledContainerColor = baseColor.copy(alpha = 0.6f),
                focusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                disabledTrailingIconColor = Color.White.copy(alpha = 0.4f),
                focusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                disabledLeadingIconColor = Color.White.copy(alpha = 0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White.copy(alpha = 0.6f),
                cursorColor = focusColor
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            enabled = enabled,
            visualTransformation = visualTransformation
        )
    }
}

@Composable
private fun SocialLoginButton(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean,
    baseColor: Color
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = baseColor
    ) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 61.dp)
                .alpha(if (enabled) 1f else 0.5f),
            contentAlignment = Alignment.Center
        ) {
            // ACA VA UNA IMAGEN USADA!!!
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    OrbitSoundKotlinTheme(dynamicColor = false) {
        LoginScreen()
    }
}
