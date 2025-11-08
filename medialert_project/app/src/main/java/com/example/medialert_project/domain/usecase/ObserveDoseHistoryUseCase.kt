package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.model.DoseLog
import com.example.medialert_project.domain.repository.DoseLogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDoseHistoryUseCase @Inject constructor(
    private val doseLogRepository: DoseLogRepository
) {
    operator fun invoke(): Flow<List<DoseLog>> {
        return doseLogRepository.observeAllDoseLogs()
    }

    fun forMedicine(medicineId: String): Flow<List<DoseLog>> {
        return doseLogRepository.observeLogsForMedicine(medicineId)
    }
}
