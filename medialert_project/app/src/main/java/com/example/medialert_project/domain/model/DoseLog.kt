package com.example.medialert_project.domain.model

import java.time.Instant

data class DoseLog(
    val id: String,
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val scheduleId: String?,
    val scheduledAt: Instant,
    val actedAt: Instant?,
    val status: DoseStatus,
    val notes: String?,
    val recordedAt: Instant
)

enum class DoseStatus {
    TAKEN,
    MISSED,
    SKIPPED
}
