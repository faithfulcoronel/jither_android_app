package com.example.medialert_project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.medialert_project.data.local.entity.MedicineEntity
import com.example.medialert_project.data.local.entity.MedicineScheduleEntity
import com.example.medialert_project.data.local.entity.MedicineWithScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Transaction
    @Query("SELECT * FROM medicines ORDER BY name")
    fun observeMedicines(): Flow<List<MedicineWithScheduleEntity>>

    @Transaction
    @Query("SELECT * FROM medicines WHERE id = :medicineId LIMIT 1")
    suspend fun getMedicine(medicineId: String): MedicineWithScheduleEntity?

    @Upsert
    suspend fun upsertMedicine(medicine: MedicineEntity)

    @Upsert
    suspend fun upsertSchedules(schedules: List<MedicineScheduleEntity>)

    @Query("DELETE FROM medicine_schedules WHERE medicine_id = :medicineId")
    suspend fun deleteSchedulesForMedicine(medicineId: String)

    @Query("DELETE FROM medicines WHERE id = :medicineId")
    suspend fun deleteMedicine(medicineId: String)
}
