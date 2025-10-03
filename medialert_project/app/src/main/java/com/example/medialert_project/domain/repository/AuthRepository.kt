package com.example.medialert_project.domain.repository

import com.example.medialert_project.domain.model.SessionData
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val sessionFlow: Flow<SessionData?>

    suspend fun signIn(email: String, password: String): Result<SessionData>

    suspend fun signUp(email: String, password: String): Result<SessionData?>

    suspend fun restoreSession(): Result<SessionData?>

    suspend fun signOut()
}
