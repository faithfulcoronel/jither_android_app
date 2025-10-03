package com.example.medialert_project.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val dosage: String,
    val instructions: String?,
    @ColumnInfo(name = "color_hex")
    val colorHex: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "medicine_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicine_id")]
)
data class MedicineScheduleEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "medicine_id")
    val medicineId: String,
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate?,
    @ColumnInfo(name = "reminder_times")
    val reminderTimes: List<LocalTime>,
    val timezone: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean
)

data class MedicineWithScheduleEntity(
    @Embedded
    val medicine: MedicineEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicine_id"
    )
    val schedules: List<MedicineScheduleEntity>
)
