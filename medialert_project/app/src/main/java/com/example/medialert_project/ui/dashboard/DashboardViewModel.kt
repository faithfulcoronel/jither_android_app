package com.example.medialert_project.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medialert_project.R
import com.example.medialert_project.domain.model.Medicine
import com.example.medialert_project.domain.usecase.DeleteMedicineUseCase
import com.example.medialert_project.domain.usecase.ObserveTodayMedicinesUseCase
import com.example.medialert_project.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeTodayMedicinesUseCase: ObserveTodayMedicinesUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeTodayMedicinesUseCase().collectLatest { medicines ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        medicines = medicines.map { it.toUiModel() }
                    )
                }
            }
        }
    }

    fun deleteMedicine(id: String) {
        viewModelScope.launch {
            val result = runCatching { deleteMedicineUseCase(id) }
            if (result.isSuccess) {
                _events.send(DashboardEvent.ShowMessage(R.string.snackbar_medicine_deleted))
            } else {
                _events.send(DashboardEvent.ShowMessage(R.string.snackbar_medicine_delete_error))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { signOutUseCase() }
        }
    }

    private fun Medicine.toUiModel(): MedicineUiModel {
        val schedule = schedules.firstOrNull()
        val times = schedule?.reminderTimes?.joinToString { it.toString() } ?: "--"
        return MedicineUiModel(
            id = id,
            name = name,
            dosage = dosage,
            timesDescription = times,
            colorHex = colorHex
        )
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val medicines: List<MedicineUiModel> = emptyList()
)

data class MedicineUiModel(
    val id: String,
    val name: String,
    val dosage: String,
    val timesDescription: String,
    val colorHex: String
)

sealed class DashboardEvent {
    data class ShowMessage(val messageRes: Int) : DashboardEvent()
}
