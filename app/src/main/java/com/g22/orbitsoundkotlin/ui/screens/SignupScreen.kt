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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.ui.theme.EncodeSansExpanded
import com.g22.orbitsoundkotlin.ui.theme.OrbitSoundKotlinTheme
import com.g22.orbitsoundkotlin.ui.theme.RobotoMono

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSignUp: (email: String, password: String, confirmPassword: String) -> Unit = { _, _, _ -> },
    onAlreadyHaveAccount: () -> Unit = {},
    onGoogleSignUp: () -> Unit = {},
    onAppleSignUp: () -> Unit = {},
    onSpotifySignUp: () -> Unit = {}
) {
    val backgroundColor = Color(0xFF010B19)
    val baseColor = Color(0xFFB4B1B8).copy(alpha = 0.3f)
    val focusColor = Color(0xFF0095FC)

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(48.dp))

            // ACA VA UNA IMAGEN USADA!!!
            Image(
                painter = painterResource(id = R.drawable.logo_tentative),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )

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

            Spacer(modifier = Modifier.height(28.dp))

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

            OutlinedInputField(
                value = confirm,
                onValueChange = { confirm = it },
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
                        imageVector = if (showConfirm) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                onTrailingIconClick = { showConfirm = !showConfirm },
                enabled = !isLoading,
                baseColor = baseColor,
                focusColor = focusColor,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onSignUp(email.trim(), password.trim(), confirm.trim()) },
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
                        text = "Sign Up",
                        style = TextStyle(
                            fontFamily = RobotoMono,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Or continue with",
                style = TextStyle(
                    fontFamily = RobotoMono,
                    color = Color.White.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SocialLoginButton(
                    iconRes = R.drawable.google,
                    contentDescription = "Sign up with Google",
                    onClick = onGoogleSignUp,
                    enabled = !isLoading,
                    baseColor = baseColor
                )
                SocialLoginButton(
                    iconRes = R.drawable.apple,
                    contentDescription = "Sign up with Apple",
                    onClick = onAppleSignUp,
                    enabled = !isLoading,
                    baseColor = baseColor
                )
                SocialLoginButton(
                    iconRes = R.drawable.spotify_logo,
                    contentDescription = "Sign up with Spotify",
                    onClick = onSpotifySignUp,
                    enabled = !isLoading,
                    baseColor = baseColor
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        .background(Color.Transparent)
                        .clickable(enabled = !isLoading, onClick = onAlreadyHaveAccount),
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
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedIndicatorColor = focusColor,
                unfocusedIndicatorColor = baseColor,
                focusedLabelColor = Color.White.copy(alpha = 0.7f),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedContainerColor = focusColor.copy(alpha = 0.3f),
                unfocusedContainerColor = baseColor,
                focusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedTrailingIconColor = Color.White.copy(alpha = 0.7f),
                focusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                unfocusedLeadingIconColor = Color.White.copy(alpha = 0.7f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
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
                .size(width = 88.dp, height = 61.dp),
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
private fun SignupPreview() {
    OrbitSoundKotlinTheme(dynamicColor = false) {
        SignupScreen()
    }
}
