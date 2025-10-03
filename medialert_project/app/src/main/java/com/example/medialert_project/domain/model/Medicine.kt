package com.example.medialert_project.domain.model

import java.time.LocalDateTime

data class Medicine(
    val id: String,
    val name: String,
    val dosage: String,
    val instructions: String?,
    val colorHex: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val schedules: List<MedicineSchedule>
)
