package com.example.medialert_project.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medialert_project.R
import com.example.medialert_project.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "medicine_alarms_v2"
        const val CHANNEL_NAME = "Medicine Alarms"
        const val NOTIFICATION_ID_BASE = 1000

        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "dosage"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_NOTIFICATION_ID = "notification_id"

        const val ACTION_MARK_TAKEN = "com.example.medialert_project.MARK_TAKEN"
        const val ACTION_SKIP_DOSE = "com.example.medialert_project.SKIP_DOSE"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var currentRingtone: Ringtone? = null

    init {
        createNotificationChannel()
    }

    /**
     * Ensures notification channel is created. This is called automatically on init,
     * but can be called again if needed.
     */
    fun ensureChannelCreated() {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channel if it exists
            try {
                notificationManager.deleteNotificationChannel("medicine_reminders")
                Timber.d("Deleted old notification channel")
            } catch (e: Exception) {
                Timber.w(e, "Could not delete old channel")
            }

            // Create new channel with alarm sound
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Critical alarms for medicine reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC

                // Use alarm sound for medicine reminders
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()

                setSound(alarmSound, audioAttributes)
                setBypassDnd(true) // Allow notification even in Do Not Disturb mode
                enableLights(true)
                lightColor = android.graphics.Color.RED
            }

            notificationManager.createNotificationChannel(channel)
            Timber.d("Notification channel created: $CHANNEL_ID with alarm sound")
        }
    }

    fun showMedicineReminder(
        medicineId: String,
        medicineName: String,
        dosage: String,
        scheduleId: String?,
        notificationId: Int
    ) {
        Timber.d("Showing notification for medicine: $medicineName")

        // Intent to open the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full-screen intent for showing alarm when device is locked
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 30000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Mark as Taken" action
        val markTakenIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MARK_TAKEN
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for "Skip" action
        val skipIntent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_SKIP_DOSE
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, medicineName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with alarm functionality
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_message, medicineName, dosage))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_message, medicineName, dosage))
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_check,
                context.getString(R.string.notification_action_taken),
                markTakenPendingIntent
            )
            .addAction(
                R.drawable.ic_skip,
                context.getString(R.string.notification_action_skip),
                skipPendingIntent
            )

        // For Android 7.1 and below, set sound and vibration manually
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
        }

        val notification = notificationBuilder.build()

        notificationManager.notify(notificationId, notification)
        Timber.d("Notification displayed with ID: $notificationId")
    }

    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Timber.d("Notification cancelled: $notificationId")
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Timber.d("All notifications cancelled")
    }

    /**
     * Plays an alarm sound for medicine reminders.
     * This method explicitly plays the alarm sound to ensure it's audible.
     */
    fun playAlarmSound() {
        try {
            // Stop any currently playing ringtone
            stopAlarmSound()

            // Get the default alarm ringtone URI
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            // Create and configure the ringtone
            currentRingtone = RingtoneManager.getRingtone(context, alarmUri)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                currentRingtone?.audioAttributes = audioAttributes
            } else {
                // For older versions, use the alarm stream
                @Suppress("DEPRECATION")
                currentRingtone?.streamType = AudioManager.STREAM_ALARM
            }

            // Play the alarm sound
            currentRingtone?.play()
            Timber.d("Alarm sound playing")
        } catch (e: Exception) {
            Timber.e(e, "Failed to play alarm sound")
        }
    }

    /**
     * Stops the currently playing alarm sound
     */
    fun stopAlarmSound() {
        try {
            currentRingtone?.stop()
            currentRingtone = null
            Timber.d("Alarm sound stopped")
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop alarm sound")
        }
    }
}
