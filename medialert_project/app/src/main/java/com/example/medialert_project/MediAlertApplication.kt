package com.example.medialert_project

import android.app.Application
import com.example.medialert_project.notification.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber
import javax.inject.Inject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@HiltAndroidApp
class MediAlertApplication : Application() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationHelperEntryPoint {
        fun notificationHelper(): NotificationHelper
    }

    override fun onCreate() {
        super.onCreate()

        // Setup Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Log app startup
        Timber.d("MediAlertApplication initialized")

        // Initialize notification helper to create channels early
        // This ensures the notification channel is created on app startup
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                NotificationHelperEntryPoint::class.java
            )
            val notificationHelper = entryPoint.notificationHelper()
            notificationHelper.ensureChannelCreated()
            Timber.d("NotificationHelper initialized, alarm channel created")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NotificationHelper")
        }

        // Setup global exception handler for debugging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            // Let system handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
