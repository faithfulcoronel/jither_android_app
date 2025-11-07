package com.example.medialert_project.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicine_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MedicineScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["schedule_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("medicine_id"), Index("schedule_id")]
)
data class DoseLogEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "medicine_id")
    val medicineId: String,
    @ColumnInfo(name = "schedule_id")
    val scheduleId: String?,
    @ColumnInfo(name = "scheduled_at")
    val scheduledAt: Instant,
    @ColumnInfo(name = "acted_at")
    val actedAt: Instant?,
    val status: DoseLogStatus,
    val notes: String?,
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Instant
)

enum class DoseLogStatus {
    TAKEN,
    MISSED,
    SKIPPED
}
