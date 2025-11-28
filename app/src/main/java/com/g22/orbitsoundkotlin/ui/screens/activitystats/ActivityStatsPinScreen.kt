package com.g22.orbitsoundkotlin.ui.screens.activitystats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Pantalla para configurar el PIN por primera vez.
 */
@Composable
fun ActivityStatsPinSetupScreen(
    pinStorage: ActivityStatsPinStorage,
    onPinSet: () -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pinSetSuccess by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24292E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Protect your Activity Stats",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Set a 4-digit PIN to keep your activity and journal private.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN Input
        PinInputField(
            value = pin,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    pin = newValue
                    errorMessage = null
                }
            },
            label = "Enter PIN",
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Confirm PIN Input
        PinInputField(
            value = confirmPin,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    confirmPin = newValue
                    errorMessage = null
                }
            },
            label = "Confirm PIN",
            modifier = Modifier.fillMaxWidth()
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF5252),
                style = TextStyle(fontSize = 12.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (pin.length != 4) {
                    errorMessage = "PIN must be 4 digits"
                    return@Button
                }
                if (confirmPin.length != 4) {
                    errorMessage = "Confirm PIN must be 4 digits"
                    return@Button
                }
                if (pin != confirmPin) {
                    errorMessage = "PINs do not match"
                    return@Button
                }
                
                // Guardar PIN
                if (pinStorage.savePin(pin)) {
                    pinSetSuccess = true
                } else {
                    errorMessage = "Failed to save PIN. Please try again."
                }
            },
            enabled = pin.length == 4 && confirmPin.length == 4,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5099BA),
                contentColor = Color.White
            )
        ) {
            Text("Save PIN", style = TextStyle(fontSize = 16.sp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text(
                text = "Cancel",
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
    
    // Navegar cuando el PIN se guarde exitosamente
    LaunchedEffect(pinSetSuccess) {
        if (pinSetSuccess) {
            delay(500) // Pequeño delay para mostrar feedback
            onPinSet()
        }
    }
}

/**
 * Pantalla para desbloquear con PIN.
 */
@Composable
fun ActivityStatsPinUnlockScreen(
    pinStorage: ActivityStatsPinStorage,
    onUnlocked: () -> Unit,
    onForgotPin: () -> Unit,
    onBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24292E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Unlock Activity Stats",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Enter your 4-digit PIN to see your stats and journal.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // PIN Input
        PinInputField(
            value = pin,
            onValueChange = { newValue ->
                if (newValue.length <= 4 && newValue.all { it.isDigit() }) {
                    pin = newValue
                    errorMessage = null
                    
                    // Auto-verificar cuando se complete el PIN
                    if (newValue.length == 4) {
                        if (pinStorage.verifyPin(newValue)) {
                            onUnlocked()
                        } else {
                            errorMessage = "Incorrect PIN. Try again."
                            pin = ""
                            attempts++
                        }
                    }
                }
            },
            label = "Enter PIN",
            modifier = Modifier.fillMaxWidth()
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF5252),
                style = TextStyle(fontSize = 12.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (pin.length != 4) {
                    errorMessage = "PIN must be 4 digits"
                    return@Button
                }
                
                if (pinStorage.verifyPin(pin)) {
                    onUnlocked()
                } else {
                    errorMessage = "Incorrect PIN. Try again."
                    pin = ""
                    attempts++
                }
            },
            enabled = pin.length == 4,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5099BA),
                contentColor = Color.White
            )
        ) {
            Text("Unlock", style = TextStyle(fontSize = 16.sp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onForgotPin) {
            Text(
                text = "Forgot PIN?",
                color = Color(0xFF5099BA)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(onClick = onBack) {
            Text(
                text = "Cancel",
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Pantalla para resetear el PIN usando la contraseña de la cuenta.
 */
@Composable
fun ActivityStatsPinResetScreen(
    onPinReset: () -> Unit,
    onCancel: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF24292E))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Reset Activity Stats PIN",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "To reset your Activity Stats PIN, please confirm your account password.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF5099BA),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedIndicatorColor = Color(0xFF5099BA),
                unfocusedIndicatorColor = Color(0xFF5099BA).copy(alpha = 0.5f),
                focusedContainerColor = Color(0xFF24292E),
                unfocusedContainerColor = Color(0xFF24292E)
            ),
            singleLine = true
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF5252),
                style = TextStyle(fontSize = 12.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (password.isEmpty()) {
                    errorMessage = "Password is required"
                    return@Button
                }
                
                isLoading = true
                // TODO: Validar contraseña con AuthService
                // Por ahora, simulamos validación
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    // TODO: Reemplazar con validación real
                    // if (authService.verifyPassword(password)) {
                    //     pinStorage.clearPin()
                    //     onPinReset()
                    // } else {
                    //     errorMessage = "Invalid password. Please try again."
                    // }
                    // Por ahora, aceptamos cualquier contraseña para desarrollo
                    onPinReset()
                    isLoading = false
                }
            },
            enabled = !isLoading && password.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5099BA),
                contentColor = Color.White
            )
        ) {
            Text(
                if (isLoading) "Verifying..." else "Confirm",
                style = TextStyle(fontSize = 16.sp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text("Cancel", style = TextStyle(fontSize = 16.sp))
        }
    }
}

/**
 * Componente reutilizable para input de PIN con indicadores visuales.
 */
@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF5099BA),
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                focusedIndicatorColor = Color(0xFF5099BA),
                unfocusedIndicatorColor = Color(0xFF5099BA).copy(alpha = 0.5f),
                focusedContainerColor = Color(0xFF24292E),
                unfocusedContainerColor = Color(0xFF24292E)
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Indicadores visuales de dígitos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            width = 2.dp,
                            color = if (index < value.length) Color(0xFF5099BA) else Color(0xFF5099BA).copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                        .background(
                            color = if (index < value.length) Color(0xFF5099BA).copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        )
                )
                if (index < 3) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }
    }
}

