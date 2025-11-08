package com.example.medialert_project.ui.medicine

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medialert_project.R
import com.example.medialert_project.domain.model.MedicineInput
import com.example.medialert_project.domain.usecase.DeleteMedicineUseCase
import com.example.medialert_project.domain.usecase.GetMedicineUseCase
import com.example.medialert_project.domain.usecase.UpsertMedicineUseCase
import com.example.medialert_project.notification.MedicineReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AddEditMedicineViewModel @Inject constructor(
    private val getMedicineUseCase: GetMedicineUseCase,
    private val upsertMedicineUseCase: UpsertMedicineUseCase,
    private val deleteMedicineUseCase: DeleteMedicineUseCase,
    private val reminderScheduler: MedicineReminderScheduler,
    private val clock: Clock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val medicineId: String? = savedStateHandle.get<String>("medicineId")

    private val _formState = MutableStateFlow(
        MedicineFormState(
            startDate = LocalDate.now(clock).toString(),
            timezone = clock.zone.id,
            isActive = true
        )
    )
    val formState: StateFlow<MedicineFormState> = _formState.asStateFlow()

    private val _events = Channel<FormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val isEditing: Boolean get() = medicineId != null

    init {
        if (medicineId != null) {
            loadMedicine(medicineId)
        }
    }

    private fun loadMedicine(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null) }
            val medicine = getMedicineUseCase(id)
            if (medicine == null) {
                _formState.update {
                    it.copy(isLoading = false, errorMessage = "Medicine not found")
                }
                return@launch
            }
            val schedule = medicine.schedules.firstOrNull()
            _formState.update {
                it.copy(
                    name = medicine.name,
                    dosage = medicine.dosage,
                    instructions = medicine.instructions.orEmpty(),
                    colorHex = medicine.colorHex,
                    isActive = medicine.isActive,
                    startDate = schedule?.startDate?.toString() ?: LocalDate.now(clock).toString(),
                    endDate = schedule?.endDate?.toString().orEmpty(),
                    times = schedule?.reminderTimes?.joinToString(separator = ",") { time -> time.toString() }
                        ?: "08:00",
                    timezone = schedule?.timezone?.id ?: clock.zone.id,
                    isLoading = false
                )
            }
        }
    }

    fun updateName(value: String) {
        _formState.update { it.copy(name = value) }
    }

    fun updateDosage(value: String) {
        _formState.update { it.copy(dosage = value) }
    }

    fun updateInstructions(value: String) {
        _formState.update { it.copy(instructions = value) }
    }

    fun updateColor(value: String) {
        _formState.update { it.copy(colorHex = value) }
    }

    fun updateStartDate(value: String) {
        _formState.update { it.copy(startDate = value) }
    }

    fun updateEndDate(value: String) {
        _formState.update { it.copy(endDate = value) }
    }

    fun updateTimes(value: String) {
        _formState.update { it.copy(times = value) }
    }

    fun updateTimezone(value: String) {
        _formState.update { it.copy(timezone = value) }
    }

    fun updateIsActive(value: Boolean) {
        _formState.update { it.copy(isActive = value) }
    }

    fun save() {
        viewModelScope.launch {
            val state = formState.value
            val validationError = validate(state)
            if (validationError != null) {
                _formState.update { it.copy(errorMessage = validationError) }
                return@launch
            }
            val reminderTimes = parseReminderTimes(state.times)
            if (reminderTimes == null) {
                _formState.update { it.copy(errorMessage = "Invalid reminder time format") }
                return@launch
            }

            val startDateInput = state.startDate.trim()
            val startDate = runCatching { LocalDate.parse(startDateInput) }.getOrNull()
            if (startDate == null) {
                _formState.update { it.copy(errorMessage = "Invalid start date format") }
                return@launch
            }

            val endDateInput = state.endDate.trim()
            val endDate = if (endDateInput.isBlank()) {
                null
            } else {
                runCatching { LocalDate.parse(endDateInput) }.getOrNull()
            }
            if (endDateInput.isNotBlank() && endDate == null) {
                _formState.update { it.copy(errorMessage = "Invalid end date format") }
                return@launch
            }
            _formState.update { it.copy(isLoading = true, errorMessage = null) }
            val input = MedicineInput(
                id = medicineId,
                name = state.name.trim(),
                dosage = state.dosage.trim(),
                instructions = state.instructions.trim().ifBlank { null },
                colorHex = normalizeColor(state.colorHex),
                isActive = state.isActive,
                startDate = startDate,
                endDate = endDate,
                reminderTimes = reminderTimes,
                timezone = runCatching { ZoneId.of(state.timezone.trim()) }.getOrElse { clock.zone }
            )
            val result = upsertMedicineUseCase(input)
            if (result.isSuccess) {
                val savedMedicineId = result.getOrNull()

                // Schedule notifications for the medicine
                if (savedMedicineId != null) {
                    val medicine = getMedicineUseCase(savedMedicineId)
                    if (medicine != null) {
                        Timber.d("Scheduling reminders for medicine: ${medicine.name}")
                        reminderScheduler.scheduleMedicineReminders(medicine)
                    }
                }

                _events.send(FormEvent.Saved(R.string.snackbar_medicine_saved))
            } else {
                _formState.update { it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.localizedMessage) }
                _events.send(FormEvent.ShowError(R.string.snackbar_medicine_save_error))
            }
        }
    }

    fun delete() {
        val id = medicineId ?: return
        viewModelScope.launch {
            // Cancel all reminders for this medicine
            Timber.d("Cancelling reminders for medicine: $id")
            reminderScheduler.cancelMedicineReminders(id)

            val result = runCatching { deleteMedicineUseCase(id) }
            if (result.isSuccess) {
                _events.send(FormEvent.Deleted(R.string.snackbar_medicine_deleted))
            } else {
                _events.send(FormEvent.ShowError(R.string.snackbar_medicine_delete_error))
            }
        }
    }

    private fun validate(state: MedicineFormState): String? {
        if (state.name.isBlank()) return "Name is required"
        if (state.dosage.isBlank()) return "Dosage is required"
        if (state.startDate.isBlank()) return "Start date is required"
        if (state.times.isBlank()) return "Enter at least one reminder time"
        return null
    }

    private fun parseReminderTimes(value: String): List<LocalTime>? {
        val times = value.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (times.isEmpty()) return null
        return runCatching { times.map(LocalTime::parse) }.getOrNull()
    }

    private fun normalizeColor(color: String): String {
        val trimmed = color.trim()
        return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    }
}

data class MedicineFormState(
    val name: String = "",
    val dosage: String = "",
    val instructions: String = "",
    val colorHex: String = "#4CAF50",
    val startDate: String = "",
    val endDate: String = "",
    val times: String = "08:00",
    val timezone: String = ZoneId.systemDefault().id,
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class FormEvent {
    data class Saved(val messageRes: Int) : FormEvent()
    data class Deleted(val messageRes: Int) : FormEvent()
    data class ShowError(val messageRes: Int) : FormEvent()
}
