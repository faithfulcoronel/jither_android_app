package com.example.medialert_project.data.local.converter

import androidx.room.TypeConverter
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
}
