package com.example.medialert_project.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.medialert_project.data.local.entity.DoseLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Query(
        "SELECT * FROM dose_logs ORDER BY scheduled_at DESC"
    )
    fun observeAllLogs(): Flow<List<DoseLogEntity>>

    @Query(
        "SELECT * FROM dose_logs WHERE medicine_id = :medicineId ORDER BY scheduled_at DESC"
    )
    fun observeLogsForMedicine(medicineId: String): Flow<List<DoseLogEntity>>

    @Upsert
    suspend fun upsertLog(log: DoseLogEntity)

    @Upsert
    suspend fun upsertLogs(logs: List<DoseLogEntity>)

    @Query("DELETE FROM dose_logs WHERE id = :logId")
    suspend fun deleteLog(logId: String)

    @Query("DELETE FROM dose_logs WHERE medicine_id = :medicineId")
    suspend fun deleteLogsForMedicine(medicineId: String)
}
