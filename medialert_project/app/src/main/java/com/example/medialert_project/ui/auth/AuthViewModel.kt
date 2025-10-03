package com.example.medialert_project.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medialert_project.domain.usecase.SignInUseCase
import com.example.medialert_project.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val authSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleMode() {
        _uiState.update { state ->
            val nextMode = if (state.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN
            state.copy(mode = nextMode, errorMessage = null, infoMessage = null, authSuccess = false)
        }
    }

    fun authenticate(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email.") }
            return
        }
        if (trimmedPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            when (_uiState.value.mode) {
                AuthMode.SIGN_IN -> handleSignIn(trimmedEmail, trimmedPassword)
                AuthMode.SIGN_UP -> handleSignUp(trimmedEmail, trimmedPassword)
            }
        }
    }

    private suspend fun handleSignIn(email: String, password: String) {
        val result = signInUseCase(email, password)
        _uiState.update { state ->
            if (result.isSuccess) {
                state.copy(isLoading = false, authSuccess = true, errorMessage = null)
            } else {
                state.copy(
                    isLoading = false,
                    authSuccess = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to sign in."
                )
            }
        }
    }

    private suspend fun handleSignUp(email: String, password: String) {
        val result = signUpUseCase(email, password)
        _uiState.update { state ->
            if (result.isSuccess) {
                val session = result.getOrNull()
                if (session != null) {
                    state.copy(
                        isLoading = false,
                        authSuccess = true,
                        infoMessage = "Account created successfully.",
                        errorMessage = null
                    )
                } else {
                    state.copy(
                        isLoading = false,
                        authSuccess = false,
                        infoMessage = "Check your inbox to confirm the registration before signing in.",
                        mode = AuthMode.SIGN_IN
                    )
                }
            } else {
                state.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Unable to sign up.",
                    authSuccess = false
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
