package com.example.medialert_project.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MedicineDto(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("name")
    val name: String,
    @SerialName("dosage")
    val dosage: String,
    @SerialName("instructions")
    val instructions: String? = null,
    @SerialName("color_hex")
    val colorHex: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class MedicineScheduleDto(
    @SerialName("id")
    val id: String,
    @SerialName("medicine_id")
    val medicineId: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("end_date")
    val endDate: String? = null,
    @SerialName("reminder_times")
    val reminderTimes: List<String>,
    @SerialName("timezone")
    val timezone: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class DoseLogDto(
    @SerialName("id")
    val id: String,
    @SerialName("medicine_id")
    val medicineId: String,
    @SerialName("schedule_id")
    val scheduleId: String,
    @SerialName("scheduled_time")
    val scheduledTime: String,
    @SerialName("taken_time")
    val takenTime: String? = null,
    @SerialName("status")
    val status: String,
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)
