package com.example.medialert_project.util

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PermissionHelper {

    const val PERMISSION_REQUEST_CODE = 1001

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasNotificationPermission(context) && hasAlarmPermission(context)
    }

    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Check if exact alarm permission is granted (Android 12+)
     */
    fun hasAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Not required on older versions
        }
    }

    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Request exact alarm permission (Android 12+)
     */
    fun requestAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * Request all required permissions
     */
    fun requestAllPermissions(activity: Activity) {
        // Request notification permission first
        if (!hasNotificationPermission(activity)) {
            requestNotificationPermission(activity)
        }
        // Then guide user to alarm permission if needed
        else if (!hasAlarmPermission(activity)) {
            showAlarmPermissionDialog(activity)
        }
    }

    /**
     * Show dialog explaining alarm permission
     */
    private fun showAlarmPermissionDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Enable Alarm Permission")
            .setMessage("This app needs permission to schedule exact alarms for medicine reminders. This ensures you never miss a dose.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestAlarmPermission(context)
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    /**
     * Show dialog when permissions are denied
     */
    fun showPermissionDeniedDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs notification and alarm permissions to send medicine reminders. Please enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings(activity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Open app settings
     */
    private fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }
}
