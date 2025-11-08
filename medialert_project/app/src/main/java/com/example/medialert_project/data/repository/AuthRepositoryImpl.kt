package com.example.medialert_project.data.repository

import com.example.medialert_project.data.datastore.SessionDataStore
import com.example.medialert_project.domain.model.SessionData
import com.example.medialert_project.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    override val sessionFlow: Flow<SessionData?> = sessionDataStore.sessionFlow

    override suspend fun signIn(email: String, password: String): Result<SessionData> = runCatching {
        supabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = supabaseClient.auth.currentSessionOrNull() ?: error("Missing session")
        val accessToken = session.accessToken ?: error("Missing access token")
        val refreshToken = session.refreshToken ?: error("Missing refresh token")
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: error("Missing user id")
        val sessionData = SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId
        )
        sessionDataStore.saveSession(sessionData)
        sessionData
    }

    override suspend fun signUp(email: String, password: String): Result<SessionData?> = runCatching {
        supabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val session = supabaseClient.auth.currentSessionOrNull()
        if (session != null) {
            val accessToken = session.accessToken
            val refreshToken = session.refreshToken
            val userId = supabaseClient.auth.currentUserOrNull()?.id
            if (accessToken != null && refreshToken != null && userId != null) {
                val sessionData = SessionData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId
                )
                sessionDataStore.saveSession(sessionData)
                sessionData
            } else {
                null
            }
        } else {
            null
        }
    }

    override suspend fun restoreSession(): Result<SessionData?> = runCatching {
        val stored = sessionDataStore.sessionFlow.firstOrNull()
        if (stored == null) {
            null
        } else {
            try {
                // Try to refresh the session with stored token
                supabaseClient.auth.refreshSession(stored.refreshToken)
                val session = supabaseClient.auth.currentSessionOrNull()

                if (session == null) {
                    // Session refresh failed, clear invalid data
                    sessionDataStore.clearSession()
                    return@runCatching null
                }

                val accessToken = session.accessToken
                val refreshToken = session.refreshToken
                val userId = supabaseClient.auth.currentUserOrNull()?.id

                if (accessToken == null || refreshToken == null) {
                    // Invalid session data, clear and return null
                    sessionDataStore.clearSession()
                    return@runCatching null
                }

                val sessionData = SessionData(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId ?: stored.userId
                )
                sessionDataStore.saveSession(sessionData)
                sessionData
            } catch (e: Exception) {
                // If refresh fails (expired token, network error, etc.), clear session
                sessionDataStore.clearSession()
                null
            }
        }
    }

    override suspend fun signOut() {
        try {
            supabaseClient.auth.signOut()
        } catch (ignored: Exception) {
            // Ignore sign-out errors to ensure local session data is cleared
        }
        sessionDataStore.clearSession()
    }
}
