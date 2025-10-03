package com.example.medialert_project.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medialert_project.domain.repository.AuthRepository
import com.example.medialert_project.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class SessionState {
    object Loading : SessionState()
    object Authenticated : SessionState()
    object Unauthenticated : SessionState()
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var restoreCompleted = false

    init {
        observeSession()
        attemptRestore()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.sessionFlow.collectLatest { session ->
                if (session != null) {
                    _state.value = SessionState.Authenticated
                } else if (restoreCompleted) {
                    _state.value = SessionState.Unauthenticated
                }
            }
        }
    }

    private fun attemptRestore() {
        viewModelScope.launch {
            val result = restoreSessionUseCase()
            restoreCompleted = true
            if (result.isFailure || result.getOrNull() == null) {
                _state.value = SessionState.Unauthenticated
            }
        }
    }
}
