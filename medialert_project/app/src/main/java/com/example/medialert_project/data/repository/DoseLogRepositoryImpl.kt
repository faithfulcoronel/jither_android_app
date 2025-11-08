package com.example.medialert_project.data.repository

import com.example.medialert_project.data.local.dao.DoseLogDao
import com.example.medialert_project.data.local.dao.MedicineDao
import com.example.medialert_project.data.local.entity.DoseLogEntity
import com.example.medialert_project.data.local.entity.DoseLogStatus
import com.example.medialert_project.domain.model.DoseLog
import com.example.medialert_project.domain.model.DoseStatus
import com.example.medialert_project.domain.repository.DoseLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoseLogRepositoryImpl @Inject constructor(
    private val doseLogDao: DoseLogDao,
    private val medicineDao: MedicineDao
) : DoseLogRepository {

    override fun observeAllDoseLogs(): Flow<List<DoseLog>> {
        return doseLogDao.observeAllLogs().map { entities ->
            entities.mapNotNull { entity ->
                // Get medicine info for each log
                val medicineWithSchedule = medicineDao.getMedicine(entity.medicineId)
                val medicineName = medicineWithSchedule?.medicine?.name ?: "Unknown Medicine"
                val dosage = medicineWithSchedule?.medicine?.dosage ?: ""

                entity.toDomain(medicineName, dosage)
            }
        }
    }

    override fun observeLogsForMedicine(medicineId: String): Flow<List<DoseLog>> {
        return doseLogDao.observeLogsForMedicine(medicineId).map { entities ->
            // Get medicine info
            val medicineWithSchedule = medicineDao.getMedicine(medicineId)
            val medicineName = medicineWithSchedule?.medicine?.name ?: "Unknown"
            val dosage = medicineWithSchedule?.medicine?.dosage ?: ""

            entities.map { it.toDomain(medicineName, dosage) }
        }
    }

    override suspend fun recordDose(
        id: String,
        medicineId: String,
        scheduleId: String?,
        scheduledAt: Instant,
        actedAt: Instant?,
        status: DoseStatus,
        notes: String?,
        recordedAt: Instant
    ) {
        val entity = DoseLogEntity(
            id = id,
            medicineId = medicineId,
            scheduleId = scheduleId,
            scheduledAt = scheduledAt,
            actedAt = actedAt,
            status = status.toEntity(),
            notes = notes,
            recordedAt = recordedAt
        )
        doseLogDao.upsertLog(entity)
    }

    override suspend fun deleteLog(logId: String) {
        doseLogDao.deleteLog(logId)
    }

    override suspend fun deleteLogsForMedicine(medicineId: String) {
        doseLogDao.deleteLogsForMedicine(medicineId)
    }

    private fun DoseLogEntity.toDomain(medicineName: String, dosage: String): DoseLog {
        return DoseLog(
            id = id,
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            scheduleId = scheduleId,
            scheduledAt = scheduledAt,
            actedAt = actedAt,
            status = status.toDomain(),
            notes = notes,
            recordedAt = recordedAt
        )
    }

    private fun DoseStatus.toEntity(): DoseLogStatus {
        return when (this) {
            DoseStatus.TAKEN -> DoseLogStatus.TAKEN
            DoseStatus.MISSED -> DoseLogStatus.MISSED
            DoseStatus.SKIPPED -> DoseLogStatus.SKIPPED
        }
    }

    private fun DoseLogStatus.toDomain(): DoseStatus {
        return when (this) {
            DoseLogStatus.TAKEN -> DoseStatus.TAKEN
            DoseLogStatus.MISSED -> DoseStatus.MISSED
            DoseLogStatus.SKIPPED -> DoseStatus.SKIPPED
        }
    }
}
