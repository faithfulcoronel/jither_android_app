package com.example.medialert_project.data.repository

import com.example.medialert_project.data.local.dao.MedicineDao
import com.example.medialert_project.data.local.entity.MedicineEntity
import com.example.medialert_project.data.local.entity.MedicineScheduleEntity
import com.example.medialert_project.data.local.entity.MedicineWithScheduleEntity
import com.example.medialert_project.domain.model.Medicine
import com.example.medialert_project.domain.model.MedicineInput
import com.example.medialert_project.domain.model.MedicineSchedule
import com.example.medialert_project.domain.repository.MedicineRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class MedicineRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
    private val clock: Clock
) : MedicineRepository {

    override fun observeMedicinesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<Medicine>> {
        return medicineDao.observeMedicines().map { list ->
            list.mapNotNull { it.toDomain(zoneId) }
                .map { medicine ->
                    val activeSchedules = medicine.schedules.filter { schedule ->
                        schedule.isActive &&
                            !date.isBefore(schedule.startDate) &&
                            (schedule.endDate == null || !date.isAfter(schedule.endDate))
                    }
                    medicine.copy(schedules = activeSchedules)
                }
                .filter { it.isActive && it.schedules.isNotEmpty() }
        }
    }

    override suspend fun getMedicine(id: String): Medicine? =
        medicineDao.getMedicine(id)?.toDomain(ZoneId.systemDefault())

    override suspend fun upsertMedicine(input: MedicineInput): Result<String> = runCatching {
        val nowInstant = clock.instant()
        val nowMillis = nowInstant.toEpochMilli()
        val existing = input.id?.let { medicineDao.getMedicine(it) }
        val medicineId = input.id ?: existing?.medicine?.id ?: UUID.randomUUID().toString()
        val createdAtMillis = existing?.medicine?.createdAt ?: nowMillis
        val medicineEntity = MedicineEntity(
            id = medicineId,
            name = input.name,
            dosage = input.dosage,
            instructions = input.instructions,
            colorHex = input.colorHex,
            isActive = input.isActive,
            createdAt = createdAtMillis,
            updatedAt = nowMillis
        )
        medicineDao.upsertMedicine(medicineEntity)

        val scheduleId = existing?.schedules?.firstOrNull()?.id ?: UUID.randomUUID().toString()
        val scheduleEntity = MedicineScheduleEntity(
            id = scheduleId,
            medicineId = medicineId,
            startDate = input.startDate,
            endDate = input.endDate,
            reminderTimes = input.reminderTimes,
            timezone = input.timezone.id,
            isActive = input.isActive
        )
        medicineDao.upsertSchedules(listOf(scheduleEntity))
        medicineId
    }

    override suspend fun deleteMedicine(id: String) {
        medicineDao.deleteSchedulesForMedicine(id)
        medicineDao.deleteMedicine(id)
    }

    private fun MedicineWithScheduleEntity.toDomain(zoneId: ZoneId): Medicine? {
        val createdAt = runCatching { Instant.ofEpochMilli(medicine.createdAt) }.getOrNull()
        val updatedAt = runCatching { Instant.ofEpochMilli(medicine.updatedAt) }.getOrNull()
        if (createdAt == null || updatedAt == null) return null
        val schedules = schedules.map { schedule ->
            MedicineSchedule(
                id = schedule.id,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                reminderTimes = schedule.reminderTimes,
                timezone = runCatching { ZoneId.of(schedule.timezone) }.getOrElse { zoneId },
                isActive = schedule.isActive
            )
        }
        return Medicine(
            id = medicine.id,
            name = medicine.name,
            dosage = medicine.dosage,
            instructions = medicine.instructions,
            colorHex = medicine.colorHex,
            isActive = medicine.isActive,
            createdAt = LocalDateTime.ofInstant(createdAt, zoneId),
            updatedAt = LocalDateTime.ofInstant(updatedAt, zoneId),
            schedules = schedules
        )
    }
}
