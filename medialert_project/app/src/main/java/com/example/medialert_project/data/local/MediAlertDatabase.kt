package com.example.medialert_project.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.medialert_project.data.local.converter.RoomTypeConverters
import com.example.medialert_project.data.local.dao.DoseLogDao
import com.example.medialert_project.data.local.dao.MedicineDao
import com.example.medialert_project.data.local.entity.DoseLogEntity
import com.example.medialert_project.data.local.entity.MedicineEntity
import com.example.medialert_project.data.local.entity.MedicineScheduleEntity

@Database(
    entities = [
        MedicineEntity::class,
        MedicineScheduleEntity::class,
        DoseLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class MediAlertDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    abstract fun doseLogDao(): DoseLogDao
}
