package com.g22.orbitsoundkotlin.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.g22.orbitsoundkotlin.services.AuthResult
import com.g22.orbitsoundkotlin.services.AuthService
import com.g22.orbitsoundkotlin.services.IAuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    // Usa la abstracción y por defecto inyecta el Singleton explícito (GoF)
    private val service: IAuthService = AuthService.Companion.getInstance()
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
        val email = value.trim()
        if (email.isEmpty()) return "Email is required."
        if (email.any { it.isWhitespace() }) return "Email must not contain spaces."
        if (".." in email) return "Email is invalid (consecutive dots)."

        val atIdx = email.indexOf('@')
        if (atIdx <= 0 || atIdx == email.lastIndex) return "Email is invalid (missing parts)."

        val local = email.substring(0, atIdx)
        val domain = email.substring(atIdx + 1)

        // Local part checks
        if (local.length !in 1..64) return "Email is invalid (local part length)."
        if (local.startsWith('.') || local.endsWith('.')) return "Email is invalid (dot at start/end of local-part)."

        // Domain checks
        val labels = domain.split('.')
        if (labels.size < 2) return "Email is invalid (domain must have a TLD)."
        if (labels.any { it.isEmpty() }) return "Email is invalid (empty domain label)."

        // TLD: last label
        val tld = labels.last()
        if (tld.length !in 2..24 || !tld.all { it.isLetter() }) return "Email is invalid (bad TLD)."

        // Each label 2–63, allowed [A-Za-z0-9-], not start/end with '-'
        for (label in labels) {
            if (label.length !in 2..63) return "Email is invalid (domain labels must be 2–63 chars)."
            if (!label.all { it.isLetterOrDigit() || it == '-' }) return "Email is invalid (domain contains invalid chars)."
            if (label.first() == '-' || label.last() == '-') return "Email is invalid (hyphen placement in domain)."
        }

        return null
    }



    private fun validatePassword(value: String): String? {
        val pwd = value
        val missing = mutableListOf<String>()

        if (pwd.length < 8) missing += "at least 8 characters"
        if (!pwd.any { it.isLowerCase() }) missing += "a lowercase letter"
        if (!pwd.any { it.isUpperCase() }) missing += "an uppercase letter"
        if (!pwd.any { it.isDigit() })     missing += "a number"
        if (!pwd.any { !it.isLetterOrDigit() }) missing += "a symbol"
        if (pwd.isNotEmpty() && (pwd.first().isWhitespace() || pwd.last().isWhitespace()))
            missing += "no spaces at the start or end"

        return if (missing.isEmpty()) null
        else "Password must include: ${missing.joinToString(", ")}."
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
        // RFC 5322-lite, case-insensitive, limita longitudes típicas (local <=64, total <=254)
        private val EMAIL_REGEX = Regex(
            pattern = "^(?=.{1,254}\$)(?=.{1,64}@)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\$",
            option = RegexOption.IGNORE_CASE
        )
    }
}