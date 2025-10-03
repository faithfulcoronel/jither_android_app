package com.example.medialert_project.domain.repository

import com.example.medialert_project.domain.model.Medicine
import com.example.medialert_project.domain.model.MedicineInput
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

interface MedicineRepository {
    fun observeMedicinesForDate(date: LocalDate, zoneId: ZoneId): Flow<List<Medicine>>

    suspend fun getMedicine(id: String): Medicine?

    suspend fun upsertMedicine(input: MedicineInput): Result<String>

    suspend fun deleteMedicine(id: String)
}
