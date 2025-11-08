package com.example.medialert_project

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MediAlertApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Setup Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Log app startup
        Timber.d("MediAlertApplication initialized")

        // Setup global exception handler for debugging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in thread: ${thread.name}")
            // Let system handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
