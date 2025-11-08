package com.example.medialert_project.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medialert_project.domain.model.Medicine
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val ACTION_MEDICINE_REMINDER = "com.example.medialert_project.MEDICINE_REMINDER"
    }

    fun scheduleMedicineReminders(medicine: Medicine) {
        Timber.d("Scheduling reminders for medicine: ${medicine.name}")

        if (!medicine.isActive || medicine.schedules.isEmpty()) {
            Timber.w("Medicine is inactive or has no schedules, skipping")
            return
        }

        medicine.schedules.forEach { schedule ->
            if (!schedule.isActive || schedule.reminderTimes.isEmpty()) {
                Timber.w("Schedule ${schedule.id} is inactive or has no reminder times")
                return@forEach
            }

            val timezone = schedule.timezone
            val today = LocalDate.now(timezone)

            // Check if today is within the schedule range
            if (today.isBefore(schedule.startDate)) {
                Timber.d("Schedule ${schedule.id} hasn't started yet")
                return@forEach
            }

            if (schedule.endDate != null && today.isAfter(schedule.endDate)) {
                Timber.d("Schedule ${schedule.id} has ended")
                return@forEach
            }

            // Schedule alarm for each reminder time
            schedule.reminderTimes.forEachIndexed { index, reminderTime ->
                scheduleAlarm(
                    medicine = medicine,
                    scheduleId = schedule.id,
                    reminderTime = reminderTime,
                    timezone = timezone,
                    alarmId = generateAlarmId(medicine.id, schedule.id, index)
                )
            }
        }
    }

    private fun scheduleAlarm(
        medicine: Medicine,
        scheduleId: String,
        reminderTime: LocalTime,
        timezone: ZoneId,
        alarmId: Int
    ) {
        val now = ZonedDateTime.now(timezone)
        var alarmTime = now
            .withHour(reminderTime.hour)
            .withMinute(reminderTime.minute)
            .withSecond(0)
            .withNano(0)

        // If the alarm time has passed for today, schedule for tomorrow
        if (alarmTime.isBefore(now)) {
            alarmTime = alarmTime.plusDays(1)
            Timber.d("Reminder time has passed today, scheduling for tomorrow: $alarmTime")
        }

        val triggerAtMillis = alarmTime.toInstant().toEpochMilli()

        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MEDICINE_REMINDER
            putExtra(NotificationHelper.EXTRA_MEDICINE_ID, medicine.id)
            putExtra(NotificationHelper.EXTRA_MEDICINE_NAME, medicine.name)
            putExtra(NotificationHelper.EXTRA_DOSAGE, medicine.dosage)
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, alarmId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires exact alarm permission
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Timber.d("Exact alarm scheduled for ${medicine.name} at $alarmTime (ID: $alarmId)")
                } else {
                    Timber.e("Cannot schedule exact alarms - permission not granted")
                    // Fallback to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Timber.d("Inexact alarm scheduled for ${medicine.name} at $alarmTime (ID: $alarmId)")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.d("Alarm scheduled for ${medicine.name} at $alarmTime (ID: $alarmId)")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule alarm for ${medicine.name}")
        }
    }

    fun cancelMedicineReminders(medicineId: String) {
        Timber.d("Cancelling reminders for medicine: $medicineId")

        // Cancel all possible alarm IDs for this medicine (up to 10 schedules x 10 times = 100 alarms)
        for (i in 0 until 100) {
            val alarmId = (medicineId.hashCode() + i) and 0x7FFFFFFF
            cancelAlarm(alarmId)
        }
    }

    private fun cancelAlarm(alarmId: Int) {
        val intent = Intent(context, MedicineReminderReceiver::class.java).apply {
            action = ACTION_MEDICINE_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAllMedicines(medicines: List<Medicine>) {
        Timber.d("Rescheduling all medicine reminders (${medicines.size} medicines)")
        medicines.forEach { medicine ->
            scheduleMedicineReminders(medicine)
        }
    }

    private fun generateAlarmId(medicineId: String, scheduleId: String, timeIndex: Int): Int {
        // Generate a unique but reproducible alarm ID
        val combined = "$medicineId-$scheduleId-$timeIndex"
        return combined.hashCode() and 0x7FFFFFFF // Ensure positive integer
    }
}
