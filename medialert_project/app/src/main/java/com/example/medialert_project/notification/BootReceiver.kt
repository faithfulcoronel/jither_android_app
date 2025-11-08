package com.example.medialert_project.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medialert_project.domain.repository.MedicineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var medicineRepository: MedicineRepository

    @Inject
    lateinit var reminderScheduler: MedicineReminderScheduler

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Timber.d("Device booted, rescheduling medicine reminders")

            scope.launch {
                try {
                    // Get all active medicines
                    val today = LocalDate.now()
                    val medicines = medicineRepository
                        .observeMedicinesForDate(today, ZoneId.systemDefault())
                        .first()

                    Timber.d("Rescheduling ${medicines.size} medicines after boot")
                    reminderScheduler.rescheduleAllMedicines(medicines)

                    Timber.d("All reminders rescheduled successfully")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to reschedule reminders after boot")
                }
            }
        }
    }
}
