package com.g22.orbitsoundkotlin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.services.AuthResult
import com.g22.orbitsoundkotlin.services.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val service: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var lastAuthResult: AuthResult.Success? = null

    fun onEmailChange(value: String) {
        _uiState.update {
            it.copy(
                email = value,
                emailError = null,
                genericError = null
            )
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                genericError = null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = null,
                genericError = null
            )
        }
    }

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                name = value,
                genericError = null
            )
        }
    }

    fun login() {
        val trimmedEmail = _uiState.value.email.trim()
        val password = _uiState.value.password

        val emailError = validateEmail(trimmedEmail)
        val passwordError = validatePassword(password)

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    email = trimmedEmail,
                    emailError = emailError,
                    passwordError = passwordError,
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    email = trimmedEmail,
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    genericError = null
                )
            }

            when (val result = service.login(trimmedEmail, password)) {
                is AuthResult.Success -> {
                    lastAuthResult = result
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authSuccess = true
                        )
                    }
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            genericError = result.message
                        )
                    }
                }
            }
        }
    }

    fun signup() {
        val trimmedEmail = _uiState.value.email.trim()
        val trimmedName = _uiState.value.name.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        val emailError = validateEmail(trimmedEmail)
        val passwordError = validatePassword(password)
        val confirmError = if (password == confirmPassword) null else "Passwords do not match."

        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    email = trimmedEmail,
                    name = trimmedName,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError,
                    isLoading = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    email = trimmedEmail,
                    name = trimmedName,
                    isLoading = true,
                    emailError = null,
                    passwordError = null,
                    confirmPasswordError = null,
                    genericError = null
                )
            }

            when (val result = service.signup(trimmedName, trimmedEmail, password)) {
                is AuthResult.Success -> {
                    lastAuthResult = result
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authSuccess = true
                        )
                    }
                }

                is AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            genericError = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearGenericError() {
        _uiState.update { it.copy(genericError = null) }
    }

    fun consumeSuccess() {
        _uiState.update { it.copy(authSuccess = false) }
    }

    fun latestAuthResult(): AuthResult.Success? = lastAuthResult

    private fun validateEmail(value: String): String? {
        return if (EMAIL_REGEX.matches(value)) {
            null
        } else {
            "Enter a valid email address."
        }
    }

    private fun validatePassword(value: String): String? {
        return if (PASSWORD_REGEX.matches(value)) {
            null
        } else {
            "Password must be 8+ chars with letters and numbers."
        }
    }

    data class AuthUiState(
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val name: String = "",
        val isLoading: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val genericError: String? = null,
        val authSuccess: Boolean = false
    )

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
        private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")
    }
}
