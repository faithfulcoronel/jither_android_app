package com.example.medialert_project.data.repository

import com.example.medialert_project.data.datastore.SessionDataStore
import com.example.medialert_project.data.local.dao.MedicineDao
import com.example.medialert_project.data.local.entity.MedicineEntity
import com.example.medialert_project.data.local.entity.MedicineScheduleEntity
import com.example.medialert_project.data.local.entity.MedicineWithScheduleEntity
import com.example.medialert_project.data.remote.model.MedicineDto
import com.example.medialert_project.data.remote.model.MedicineScheduleDto
import com.example.medialert_project.domain.model.Medicine
import com.example.medialert_project.domain.model.MedicineInput
import com.example.medialert_project.domain.model.MedicineSchedule
import com.example.medialert_project.domain.repository.MedicineRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepositoryImpl @Inject constructor(
    private val medicineDao: MedicineDao,
    private val supabaseClient: SupabaseClient,
    private val sessionDataStore: SessionDataStore,
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

        // Save to local database first
        medicineDao.upsertMedicineWithSchedules(
            medicineEntity,
            listOf(scheduleEntity)
        )

        // Sync to Supabase
        syncMedicineToSupabase(medicineEntity, listOf(scheduleEntity))

        medicineId
    }

    override suspend fun deleteMedicine(id: String) {
        // Delete from local database first
        medicineDao.deleteSchedulesForMedicine(id)
        medicineDao.deleteMedicine(id)

        // Sync deletion to Supabase
        deleteMedicineFromSupabase(id)
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

    // Supabase Sync Methods
    private suspend fun syncMedicineToSupabase(
        medicineEntity: MedicineEntity,
        schedules: List<MedicineScheduleEntity>
    ) {
        try {
            val session = sessionDataStore.sessionFlow.firstOrNull()
            val userId = session?.userId
            if (userId == null) {
                Timber.w("No user session, skipping Supabase sync")
                return
            }

            // Convert timestamps to ISO-8601 format
            val createdAt = Instant.ofEpochMilli(medicineEntity.createdAt)
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val updatedAt = Instant.ofEpochMilli(medicineEntity.updatedAt)
                .atZone(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            // Create MedicineDto
            val medicineDto = MedicineDto(
                id = medicineEntity.id,
                userId = userId,
                name = medicineEntity.name,
                dosage = medicineEntity.dosage,
                instructions = medicineEntity.instructions,
                colorHex = medicineEntity.colorHex,
                isActive = medicineEntity.isActive,
                createdAt = createdAt,
                updatedAt = updatedAt
            )

            // Upsert medicine to Supabase
            supabaseClient.postgrest["medicines"].upsert(medicineDto)
            Timber.d("Medicine synced to Supabase: ${medicineEntity.id}")

            // Sync schedules
            for (schedule in schedules) {
                val scheduleDto = MedicineScheduleDto(
                    id = schedule.id,
                    medicineId = schedule.medicineId,
                    startDate = schedule.startDate.toString(),
                    endDate = schedule.endDate?.toString(),
                    reminderTimes = schedule.reminderTimes.map { it.toString() },
                    timezone = schedule.timezone,
                    isActive = schedule.isActive,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
                supabaseClient.postgrest["medicine_schedules"].upsert(scheduleDto)
                Timber.d("Schedule synced to Supabase: ${schedule.id}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync medicine to Supabase, data saved locally")
            // Don't throw - allow offline operation
        }
    }

    private suspend fun deleteMedicineFromSupabase(medicineId: String) {
        try {
            val session = sessionDataStore.sessionFlow.firstOrNull()
            val userId = session?.userId
            if (userId == null) {
                Timber.w("No user session, skipping Supabase delete sync")
                return
            }

            // Delete schedules first (cascade should handle this, but being explicit)
            supabaseClient.postgrest["medicine_schedules"]
                .delete {
                    filter {
                        eq("medicine_id", medicineId)
                    }
                }

            // Delete medicine
            supabaseClient.postgrest["medicines"]
                .delete {
                    filter {
                        eq("id", medicineId)
                        eq("user_id", userId)
                    }
                }
            Timber.d("Medicine deleted from Supabase: $medicineId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete medicine from Supabase, deleted locally")
            // Don't throw - allow offline operation
        }
    }
}
