package com.example.medialert_project.domain.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class MedicineSchedule(
    val id: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val reminderTimes: List<LocalTime>,
    val timezone: ZoneId,
    val isActive: Boolean
)
