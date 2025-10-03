package com.example.medialert_project.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.medialert_project.domain.model.SessionData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN = preferencesKey<String>("access_token")
        val REFRESH_TOKEN = preferencesKey<String>("refresh_token")
        val USER_ID = preferencesKey<String>("user_id")
    }

    val sessionFlow: Flow<SessionData?> = context.sessionDataStore.data.map { prefs ->
        val accessToken = prefs[Keys.ACCESS_TOKEN]
        val refreshToken = prefs[Keys.REFRESH_TOKEN]
        val userId = prefs[Keys.USER_ID]
        if (accessToken != null && refreshToken != null && userId != null) {
            SessionData(accessToken = accessToken, refreshToken = refreshToken, userId = userId)
        } else {
            null
        }
    }

    suspend fun saveSession(session: SessionData) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = session.accessToken
            prefs[Keys.REFRESH_TOKEN] = session.refreshToken
            prefs[Keys.USER_ID] = session.userId
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
