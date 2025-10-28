package com.g22.orbitsoundkotlin.ui.screens.auth

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.g22.orbitsoundkotlin.ui.theme.RobotoMono

@Composable
fun FieldErrorText(message: String?) {
    if (message == null) return
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        style = TextStyle(
            fontFamily = RobotoMono,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp
        )
    )
}

@Composable
fun InlineErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
        contentColor = MaterialTheme.colorScheme.onError,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = TextStyle(
                    fontFamily = RobotoMono,
                    color = MaterialTheme.colorScheme.onError
                )
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    }
}

@Composable
fun OutlinedAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    baseColor: Color,
    focusColor: Color,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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
            keyboardOptions = keyboardOptions,
            enabled = enabled,
            visualTransformation = visualTransformation
        )
    }
}

@Composable
fun SocialLoginButton(
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
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
