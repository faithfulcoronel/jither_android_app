package com.example.medialert_project.domain.usecase

import com.example.medialert_project.domain.repository.MedicineRepository
import javax.inject.Inject

class GetMedicineUseCase @Inject constructor(
    private val medicineRepository: MedicineRepository
) {
    suspend operator fun invoke(id: String) = medicineRepository.getMedicine(id)
}
