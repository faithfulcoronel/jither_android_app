package com.example.medialert_project.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class MedicineInput(
    val id: String? = null,
    val name: String,
    val dosage: String,
    val instructions: String?,
    val colorHex: String,
    val isActive: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val reminderTimes: List<LocalTime>,
    val timezone: ZoneId
)
