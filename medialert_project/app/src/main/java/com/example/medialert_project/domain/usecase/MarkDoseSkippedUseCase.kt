package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.model.DoseStatus
import com.example.medialert_project.domain.repository.DoseLogRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class MarkDoseSkippedUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository,
    private val clock: Clock
) {
    suspend operator fun invoke(
        medicineId: String,
        scheduleId: String?,
        scheduledAt: Instant,
        notes: String? = null
    ): Result<String> = runCatching {
        val now = clock.instant()
        val logId = UUID.randomUUID().toString()

        doseLogRepository.recordDose(
            id = logId,
            medicineId = medicineId,
            scheduleId = scheduleId,
            scheduledAt = scheduledAt,
            actedAt = now,
            status = DoseStatus.SKIPPED,
            notes = notes,
            recordedAt = now
        )

        logId
    }
}
