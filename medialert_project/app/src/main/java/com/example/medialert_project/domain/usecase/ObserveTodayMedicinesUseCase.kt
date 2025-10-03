package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.model.Medicine
import com.example.medialert_project.domain.repository.MedicineRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveTodayMedicinesUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository,
    private val clock: Clock
) {
    operator fun invoke(zoneId: ZoneId = clock.zone): Flow<List<Medicine>> {
        val today = LocalDate.now(clock.withZone(zoneId))
        return medicineRepository.observeMedicinesForDate(today, zoneId)
    }
}
