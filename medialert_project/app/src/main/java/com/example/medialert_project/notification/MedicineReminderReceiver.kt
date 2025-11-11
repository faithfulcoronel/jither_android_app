package com.example.medialert_project.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medialert_project.domain.usecase.MarkDoseSkippedUseCase
import com.example.medialert_project.domain.usecase.MarkDoseTakenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Clock
import javax.inject.Inject

@AndroidEntryPoint
class MedicineReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var markDoseTakenUseCase: MarkDoseTakenUseCase

    @Inject
    lateinit var markDoseSkippedUseCase: MarkDoseSkippedUseCase

    @Inject
    lateinit var clock: Clock

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received broadcast: ${intent.action}")

        when (intent.action) {
            MedicineReminderScheduler.ACTION_MEDICINE_REMINDER -> {
                handleMedicineReminder(intent)
            }
            NotificationHelper.ACTION_MARK_TAKEN -> {
                handleMarkTaken(intent)
            }
            NotificationHelper.ACTION_SKIP_DOSE -> {
                handleSkipDose(intent)
            }
        }
    }

    private fun handleMedicineReminder(intent: Intent) {
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: return
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: return
        val scheduleId = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULE_ID)
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            NotificationHelper.NOTIFICATION_ID_BASE
        )

        Timber.d("Showing reminder for: $medicineName ($dosage)")

        // Play alarm sound
        notificationHelper.playAlarmSound()

        // Show notification
        notificationHelper.showMedicineReminder(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduleId = scheduleId,
            notificationId = notificationId
        )
    }

    private fun handleMarkTaken(intent: Intent) {
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: return
        val scheduleId = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULE_ID)
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            NotificationHelper.NOTIFICATION_ID_BASE
        )

        Timber.d("Marking dose as taken from notification: $medicineName")

        // Stop alarm sound
        notificationHelper.stopAlarmSound()

        // Dismiss the notification
        notificationHelper.cancelNotification(notificationId)

        // Record the dose
        scope.launch {
            val scheduledAt = clock.instant()
            val result = markDoseTakenUseCase(
                medicineId = medicineId,
                scheduleId = scheduleId,
                scheduledAt = scheduledAt
            )

            if (result.isSuccess) {
                Timber.d("Dose marked as taken successfully from notification")
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to mark dose as taken from notification")
            }
        }
    }

    private fun handleSkipDose(intent: Intent) {
        val medicineId = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_ID) ?: return
        val medicineName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICINE_NAME) ?: return
        val scheduleId = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULE_ID)
        val notificationId = intent.getIntExtra(
            NotificationHelper.EXTRA_NOTIFICATION_ID,
            NotificationHelper.NOTIFICATION_ID_BASE
        )

        Timber.d("Skipping dose from notification: $medicineName")

        // Stop alarm sound
        notificationHelper.stopAlarmSound()

        // Dismiss the notification
        notificationHelper.cancelNotification(notificationId)

        // Record the skip
        scope.launch {
            val scheduledAt = clock.instant()
            val result = markDoseSkippedUseCase(
                medicineId = medicineId,
                scheduleId = scheduleId,
                scheduledAt = scheduledAt
            )

            if (result.isSuccess) {
                Timber.d("Dose skipped successfully from notification")
            } else {
                Timber.e(result.exceptionOrNull(), "Failed to mark dose as skipped from notification")
            }
        }
    }
}
