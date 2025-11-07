package com.example.medialert_project.data.local.converter

import androidx.room.TypeConverter
import com.example.medialert_project.data.local.entity.DoseLogStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class RoomTypeConverters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun fromLocalTimeList(values: List<LocalTime>?): String? =
        values?.joinToString(separator = ",") { it.toString() }

    @TypeConverter
    fun toLocalTimeList(value: String?): List<LocalTime>? =
        value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.map(LocalTime::parse)

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun fromDoseLogStatus(value: DoseLogStatus?): String? = value?.name

    @TypeConverter
    fun toDoseLogStatus(value: String?): DoseLogStatus? =
        value?.let { runCatching { DoseLogStatus.valueOf(it) }.getOrNull() }
}
