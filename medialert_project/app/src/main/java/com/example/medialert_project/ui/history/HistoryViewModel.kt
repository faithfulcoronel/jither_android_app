package com.example.medialert_project.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medialert_project.domain.model.DoseLog
import com.example.medialert_project.domain.usecase.ObserveDoseHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeDoseHistoryUseCase: ObserveDoseHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeDoseHistoryUseCase().collectLatest { doseLogs ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        doseLogs = doseLogs.map { it.toUiModel() }
                    )
                }
            }
        }
    }

    private fun DoseLog.toUiModel(): DoseLogUiModel {
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")
        val scheduledTime = scheduledAt.atZone(ZoneId.systemDefault()).format(formatter)
        val actedTime = actedAt?.atZone(ZoneId.systemDefault())?.format(formatter)

        return DoseLogUiModel(
            id = id,
            medicineName = medicineName,
            dosage = dosage,
            scheduledTime = scheduledTime,
            actedTime = actedTime,
            status = status.name,
            notes = notes
        )
    }
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val doseLogs: List<DoseLogUiModel> = emptyList()
)

data class DoseLogUiModel(
    val id: String,
    val medicineName: String,
    val dosage: String,
    val scheduledTime: String,
    val actedTime: String?,
    val status: String,
    val notes: String?
)
