package com.example.medialert_project.data.repository

import com.example.medialert_project.data.datastore.SessionDataStore
import com.example.medialert_project.domain.model.SessionData
import com.example.medialert_project.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.Email
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
        val userSession = supabaseClient.gotrue.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val session = userSession.session ?: error("Missing session")
        val accessToken = session.accessToken ?: error("Missing access token")
        val refreshToken = session.refreshToken ?: error("Missing refresh token")
        val userId = userSession.user?.id ?: error("Missing user id")
        val sessionData = SessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId
        )
        sessionDataStore.saveSession(sessionData)
        sessionData
    }

    override suspend fun signUp(email: String, password: String): Result<SessionData?> = runCatching {
        val result = supabaseClient.gotrue.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        result.session?.let { session ->
            val accessToken = session.accessToken
            val refreshToken = session.refreshToken
            val userId = result.user?.id
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
        }
    }

    override suspend fun restoreSession(): Result<SessionData?> = runCatching {
        val stored = sessionDataStore.sessionFlow.firstOrNull()
        if (stored == null) {
            null
        } else {
            val refreshed = supabaseClient.gotrue.refreshSession(stored.refreshToken)
            val session = refreshed.session ?: error("Missing session")
            val accessToken = session.accessToken ?: error("Missing access token")
            val refreshToken = session.refreshToken ?: error("Missing refresh token")
            val userId = refreshed.user?.id ?: stored.userId
            val sessionData = SessionData(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId
            )
            sessionDataStore.saveSession(sessionData)
            sessionData
        }
    }

    override suspend fun signOut() {
        runCatching { supabaseClient.gotrue.logout() }
        sessionDataStore.clearSession()
    }
}
