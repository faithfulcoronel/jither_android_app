package com.example.medialert_project.di

import com.example.medialert_project.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        Timber.d("Initializing Supabase client...")

        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY

        Timber.d("Supabase URL: ${if (url.isNotBlank()) "configured" else "EMPTY"}")
        Timber.d("Supabase Key: ${if (key.isNotBlank()) "configured" else "EMPTY"}")

        // Validate credentials before creating client
        require(url.isNotBlank()) {
            val error = "SUPABASE_URL is not configured. Please check local.properties"
            Timber.e(error)
            error
        }
        require(key.isNotBlank()) {
            val error = "SUPABASE_ANON_KEY is not configured. Please check local.properties"
            Timber.e(error)
            error
        }

        return try {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = key
            ) {
                install(Auth)
                install(Postgrest)
            }.also {
                Timber.d("Supabase client initialized successfully")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Supabase client")
            throw e
        }
    }
}
