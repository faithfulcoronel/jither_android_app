package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.model.MedicineInput
import com.example.medialert_project.domain.repository.MedicineRepository
import javax.inject.Inject

class UpsertMedicineUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    suspend operator fun invoke(input: MedicineInput) = medicineRepository.upsertMedicine(input)
}
