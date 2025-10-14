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
import androidx.compose.runtime.remember
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

/**
 * DRAWABLES REQUERIDOS (res/drawable):
 * - logo_tentative.png   // REQUIRED_IMAGE: logo_tentative.png
 * - google.png           // REQUIRED_IMAGE: google.png
 * - apple.png            // REQUIRED_IMAGE: apple.png
 * - spotify_logo.png     // REQUIRED_IMAGE: spotify_logo.png
 */
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

    val director = remember { SignupScreenDirector() }
    val builder = remember { DefaultSignupScreenBuilder() }
    val content = director.constructDefault(
        builder = builder,
        config = SignupScreenDirector.Configuration(
            branding = SignupBranding(
                logoRes = R.drawable.logo_tentative,
                title = "Create Your Account"
            ),
            colors = SignupColors(
                background = backgroundColor,
                base = baseColor,
                focus = focusColor
            ),
            fields = listOf(
                SignupFieldConfig(
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
                    trailingIcon = null,
                    onTrailingIconClick = null,
                    enabled = !isLoading,
                    keyboardType = KeyboardType.Email,
                    visualTransformation = VisualTransformation.None
                ),
                SignupFieldConfig(
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
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                ),
                SignupFieldConfig(
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
                    keyboardType = KeyboardType.Password,
                    visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation()
                )
            ),
            primaryAction = SignupPrimaryActionConfig(
                label = "Sign Up",
                onClick = { onSignUp(email.trim(), password.trim(), confirm.trim()) },
                enabled = !isLoading
            ),
            socialSection = SignupSocialSectionConfig(
                label = "Or continue with",
                buttons = emptyList() /*
                listOf(
                    SignupSocialButtonConfig(
                        iconRes = R.drawable.google,
                        description = "Sign up with Google",
                        onClick = onGoogleSignUp,
                        enabled = !isLoading
                    ), // REQUIRED_IMAGE: google.png
                    SignupSocialButtonConfig(
                        iconRes = R.drawable.apple,
                        description = "Sign up with Apple",
                        onClick = onAppleSignUp,
                        enabled = !isLoading
                    ), // REQUIRED_IMAGE: apple.png
                    SignupSocialButtonConfig(
                        iconRes = R.drawable.spotify_logo,
                        description = "Sign up with Spotify",
                        onClick = onSpotifySignUp,
                        enabled = !isLoading
                    ) // REQUIRED_IMAGE: spotify_logo.png
                )
                */
            ),
            secondaryAction = SignupSecondaryActionConfig(
                prompt = "Already have an account? ",
                actionLabel = "Sign In",
                onActionClick = onAlreadyHaveAccount,
                enabled = !isLoading
            )
        )
    )

    SignupScreenContentView(
        modifier = modifier,
        content = content,
        isLoading = isLoading
    )
}

@Composable
private fun SignupScreenContentView(
    modifier: Modifier,
    content: SignupScreenContent,
    isLoading: Boolean
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(content.colors.background)
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
                painter = painterResource(id = content.branding.logoRes),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            ) // REQUIRED_IMAGE: logo_tentative.png

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = content.branding.title,
                style = TextStyle(
                    fontFamily = EncodeSansExpanded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            content.fields.forEachIndexed { index, field ->
                OutlinedInputField(
                    value = field.value,
                    onValueChange = field.onValueChange,
                    label = field.label,
                    leadingIcon = field.leadingIcon,
                    trailingIcon = field.trailingIcon,
                    onTrailingIconClick = field.onTrailingIconClick,
                    enabled = field.enabled,
                    baseColor = content.colors.base,
                    focusColor = content.colors.focus,
                    keyboardType = field.keyboardType,
                    visualTransformation = field.visualTransformation
                )

                if (index < content.fields.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = content.primaryAction.onClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = content.primaryAction.enabled,
                colors = ButtonDefaults.buttonColors(containerColor = content.colors.focus),
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
                        text = content.primaryAction.label,
                        style = TextStyle(
                            fontFamily = RobotoMono,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            /* Sprint 1: Los botones sociales se reactivarán en el próximo sprint.
            Text(
                text = content.socialSection.label,
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
                content.socialSection.buttons.forEach { button ->
                    SocialLoginButton(
                        iconRes = button.iconRes,
                        contentDescription = button.description,
                        onClick = button.onClick,
                        enabled = button.enabled,
                        baseColor = content.colors.base
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            */

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = content.secondaryAction.prompt,
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = content.secondaryAction.actionLabel,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .background(Color.Transparent)
                        .clickable(
                            enabled = content.secondaryAction.enabled,
                            onClick = content.secondaryAction.onActionClick
                        ),
                    style = TextStyle(
                        fontFamily = RobotoMono,
                        color = content.colors.focus,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class SignupScreenContent(
    val branding: SignupBranding,
    val colors: SignupColors,
    val fields: List<SignupFieldConfig>,
    val primaryAction: SignupPrimaryActionConfig,
    val socialSection: SignupSocialSectionConfig,
    val secondaryAction: SignupSecondaryActionConfig
)

private data class SignupBranding(
    @DrawableRes val logoRes: Int,
    val title: String
)

private data class SignupColors(
    val background: Color,
    val base: Color,
    val focus: Color
)

private data class SignupFieldConfig(
    val value: String,
    val onValueChange: (String) -> Unit,
    val label: String,
    val leadingIcon: @Composable (() -> Unit)?,
    val trailingIcon: @Composable (() -> Unit)?,
    val onTrailingIconClick: (() -> Unit)?,
    val enabled: Boolean,
    val keyboardType: KeyboardType,
    val visualTransformation: VisualTransformation
)

private data class SignupPrimaryActionConfig(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean
)

private data class SignupSocialSectionConfig(
    val label: String,
    val buttons: List<SignupSocialButtonConfig>
)

private data class SignupSocialButtonConfig(
    @DrawableRes val iconRes: Int,
    val description: String?,
    val onClick: () -> Unit,
    val enabled: Boolean
)

private data class SignupSecondaryActionConfig(
    val prompt: String,
    val actionLabel: String,
    val onActionClick: () -> Unit,
    val enabled: Boolean
)


private interface SignupScreenBuilder {
    fun reset()
    fun setBranding(branding: SignupBranding)
    fun setColors(colors: SignupColors)
    fun addField(field: SignupFieldConfig)
    fun setPrimaryAction(primaryAction: SignupPrimaryActionConfig)
    fun setSocialSection(socialSection: SignupSocialSectionConfig)
    fun setSecondaryAction(secondaryAction: SignupSecondaryActionConfig)
    fun build(): SignupScreenContent
}

private class DefaultSignupScreenBuilder : SignupScreenBuilder {
    private var branding: SignupBranding? = null
    private var colors: SignupColors? = null
    private val fields = mutableListOf<SignupFieldConfig>()
    private var primaryAction: SignupPrimaryActionConfig? = null
    private var socialSection: SignupSocialSectionConfig? = null
    private var secondaryAction: SignupSecondaryActionConfig? = null

    override fun reset() {
        branding = null
        colors = null
        fields.clear()
        primaryAction = null
        socialSection = null
        secondaryAction = null
    }

    override fun setBranding(branding: SignupBranding) {
        this.branding = branding
    }

    override fun setColors(colors: SignupColors) {
        this.colors = colors
    }

    override fun addField(field: SignupFieldConfig) {
        fields.add(field)
    }

    override fun setPrimaryAction(primaryAction: SignupPrimaryActionConfig) {
        this.primaryAction = primaryAction
    }

    override fun setSocialSection(socialSection: SignupSocialSectionConfig) {
        this.socialSection = socialSection
    }

    override fun setSecondaryAction(secondaryAction: SignupSecondaryActionConfig) {
        this.secondaryAction = secondaryAction
    }

    override fun build(): SignupScreenContent {
        val finalBranding = requireNotNull(branding) { "Branding must be set before building the screen." }
        val finalColors = requireNotNull(colors) { "Colors must be set before building the screen." }
        val finalPrimaryAction = requireNotNull(primaryAction) { "Primary action must be set before building the screen." }
        val finalSocialSection = requireNotNull(socialSection) { "Social section must be set before building the screen." }
        val finalSecondaryAction = requireNotNull(secondaryAction) { "Secondary action must be set before building the screen." }

        return SignupScreenContent(
            branding = finalBranding,
            colors = finalColors,
            fields = fields.toList(),
            primaryAction = finalPrimaryAction,
            socialSection = finalSocialSection,
            secondaryAction = finalSecondaryAction
        )
    }
}

// Director coordinates the construction sequence for the default signup layout.
private class SignupScreenDirector {
    data class Configuration(
        val branding: SignupBranding,
        val colors: SignupColors,
        val fields: List<SignupFieldConfig>,
        val primaryAction: SignupPrimaryActionConfig,
        val socialSection: SignupSocialSectionConfig,
        val secondaryAction: SignupSecondaryActionConfig
    )

    fun constructDefault(
        builder: SignupScreenBuilder,
        config: Configuration
    ): SignupScreenContent {
        builder.reset()
        builder.setBranding(config.branding)
        builder.setColors(config.colors)
        config.fields.forEach { builder.addField(it) }
        builder.setPrimaryAction(config.primaryAction)
        builder.setSocialSection(config.socialSection)
        builder.setSecondaryAction(config.secondaryAction)
        return builder.build()
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
            modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.size(width = 88.dp, height = 61.dp),
            contentAlignment = Alignment.Center
        ) {
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
