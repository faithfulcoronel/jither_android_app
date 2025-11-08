package com.example.medialert_project.domain.repository

import com.example.medialert_project.domain.model.DoseLog
import com.example.medialert_project.domain.model.DoseStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface DoseLogRepository {
    fun observeAllDoseLogs(): Flow<List<DoseLog>>

    fun observeLogsForMedicine(medicineId: String): Flow<List<DoseLog>>

    suspend fun recordDose(
        id: String,
        medicineId: String,
        scheduleId: String?,
        scheduledAt: Instant,
        actedAt: Instant?,
        status: DoseStatus,
        notes: String?,
        recordedAt: Instant
    )

    suspend fun deleteLog(logId: String)

    suspend fun deleteLogsForMedicine(medicineId: String)
}
