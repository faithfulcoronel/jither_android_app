package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.repository.AuthRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.restoreSession()
}
