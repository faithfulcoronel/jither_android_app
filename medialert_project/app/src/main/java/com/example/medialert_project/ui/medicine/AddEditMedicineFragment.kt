package com.example.medialert_project.ui.medicine

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.medialert_project.R
import com.example.medialert_project.databinding.FragmentAddEditMedicineBinding
import com.example.medialert_project.util.PermissionHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AddEditMedicineFragment : Fragment() {

    private var _binding: FragmentAddEditMedicineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditMedicineViewModel by viewModels()

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

    // Permission request launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Check if alarm permission is also needed
            if (!PermissionHelper.hasAlarmPermission(requireContext())) {
                PermissionHelper.requestAllPermissions(requireActivity())
            }
        } else {
            Snackbar.make(
                binding.root,
                "Notification permission is required for medicine reminders",
                Snackbar.LENGTH_LONG
            ).setAction("Settings") {
                PermissionHelper.showPermissionDeniedDialog(requireActivity())
            }.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditMedicineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestPermissionsIfNeeded()
        setupInputs()
        observeState()
        observeEvents()
    }

    private fun requestPermissionsIfNeeded() {
        if (!PermissionHelper.hasAllPermissions(requireContext())) {
            PermissionHelper.requestAllPermissions(requireActivity())
        }
    }

    private fun setupInputs() {
        binding.formTitle.text = if (viewModel.isEditing) {
            getString(R.string.edit_medicine_title)
        } else {
            getString(R.string.add_medicine_title)
        }
        binding.deleteButton.isVisible = viewModel.isEditing

        binding.nameEditText.doAfterTextChanged { viewModel.updateName(it?.toString().orEmpty()) }
        binding.dosageEditText.doAfterTextChanged { viewModel.updateDosage(it?.toString().orEmpty()) }
        binding.instructionsEditText.doAfterTextChanged { viewModel.updateInstructions(it?.toString().orEmpty()) }

        // Make color field non-editable and clickable
        binding.colorEditText.isFocusable = false
        binding.colorEditText.isClickable = true
        binding.colorEditText.setOnClickListener { showColorPicker() }

        // Make date fields non-editable and clickable
        binding.startDateEditText.isFocusable = false
        binding.startDateEditText.isClickable = true
        binding.startDateEditText.setOnClickListener { showStartDatePicker() }

        binding.endDateEditText.isFocusable = false
        binding.endDateEditText.isClickable = true
        binding.endDateEditText.setOnClickListener { showEndDatePicker() }

        // Make times field non-editable and clickable
        binding.timesEditText.isFocusable = false
        binding.timesEditText.isClickable = true
        binding.timesEditText.setOnClickListener { showTimePickerDialog() }
        binding.timesEditText.setOnLongClickListener {
            showRemoveTimeDialog()
            true
        }

        binding.timezoneEditText.doAfterTextChanged { viewModel.updateTimezone(it?.toString().orEmpty()) }
        binding.activeSwitch.setOnCheckedChangeListener { _, isChecked -> viewModel.updateIsActive(isChecked) }

        binding.saveButton.setOnClickListener { viewModel.save() }
        binding.deleteButton.setOnClickListener { confirmDelete() }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formState.collectLatest { state ->
                    if (binding.nameEditText.text?.toString() != state.name) {
                        binding.nameEditText.setText(state.name)
                    }
                    if (binding.dosageEditText.text?.toString() != state.dosage) {
                        binding.dosageEditText.setText(state.dosage)
                    }
                    if (binding.instructionsEditText.text?.toString() != state.instructions) {
                        binding.instructionsEditText.setText(state.instructions)
                    }
                    // Format and display color
                    if (state.colorHex.isNotEmpty()) {
                        val colorName = getColorName(state.colorHex)
                        if (binding.colorEditText.text?.toString() != colorName) {
                            binding.colorEditText.setText(colorName)
                            runCatching {
                                binding.colorEditText.setBackgroundColor(Color.parseColor(state.colorHex))
                            }
                        }
                    }

                    // Format and display start date
                    val startDate = parseDate(state.startDate)
                    if (startDate != null) {
                        val formattedStart = startDate.format(dateFormatter)
                        if (binding.startDateEditText.text?.toString() != formattedStart) {
                            binding.startDateEditText.setText(formattedStart)
                        }
                    } else if (state.startDate.isNotEmpty()) {
                        binding.startDateEditText.setText(state.startDate)
                    }

                    // Format and display end date
                    if (state.endDate.isNotEmpty()) {
                        val endDate = parseDate(state.endDate)
                        if (endDate != null) {
                            val formattedEnd = endDate.format(dateFormatter)
                            if (binding.endDateEditText.text?.toString() != formattedEnd) {
                                binding.endDateEditText.setText(formattedEnd)
                            }
                        } else {
                            binding.endDateEditText.setText(state.endDate)
                        }
                    } else {
                        binding.endDateEditText.setText("")
                    }

                    // Format times in AM/PM format for display
                    val currentDisplayText = binding.timesEditText.text?.toString()
                    val times = state.times.split(',')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .mapNotNull { runCatching { LocalTime.parse(it) }.getOrNull() }

                    if (times.isNotEmpty()) {
                        val formattedTimes = times.joinToString(", ") { it.format(timeFormatter) }
                        if (currentDisplayText != formattedTimes) {
                            binding.timesEditText.setText(formattedTimes)
                        }
                    } else if (state.times.isNotEmpty() && currentDisplayText != state.times) {
                        // If times can't be parsed, show as-is
                        binding.timesEditText.setText(state.times)
                    }

                    if (binding.timezoneEditText.text?.toString() != state.timezone) {
                        binding.timezoneEditText.setText(state.timezone)
                    }
                    if (binding.activeSwitch.isChecked != state.isActive) {
                        binding.activeSwitch.isChecked = state.isActive
                    }
                    binding.formProgress.isVisible = state.isLoading
                    binding.saveButton.isEnabled = !state.isLoading
                    binding.errorMessageText.isVisible = state.errorMessage != null
                    binding.errorMessageText.text = state.errorMessage
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is FormEvent.Saved -> {
                        Snackbar.make(binding.root, getString(event.messageRes), Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is FormEvent.Deleted -> {
                        Snackbar.make(binding.root, getString(event.messageRes), Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    is FormEvent.ShowError -> {
                        Snackbar.make(binding.root, getString(event.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showTimePickerDialog() {
        // Parse existing times to get current selection
        val currentTimes = parseTimesFromState()
        val currentTime = currentTimes.lastOrNull() ?: LocalTime.of(8, 0)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentTime.hour)
            .setMinute(currentTime.minute)
            .setTitleText("Select reminder time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedTime = LocalTime.of(picker.hour, picker.minute)
            addTime(selectedTime)
        }

        picker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun parseTimesFromState(): List<LocalTime> {
        val timesText = binding.timesEditText.text?.toString() ?: return emptyList()
        if (timesText.isBlank()) return emptyList()

        return timesText.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { timeStr ->
                // Try parsing as HH:MM format
                runCatching { LocalTime.parse(timeStr) }.getOrNull()
                    // Try parsing as h:mm a format (12-hour with AM/PM)
                    ?: runCatching { LocalTime.parse(timeStr, timeFormatter) }.getOrNull()
            }
    }

    private fun addTime(time: LocalTime) {
        val currentTimes = parseTimesFromState().toMutableList()

        // Check if time already exists
        if (currentTimes.any { it == time }) {
            Snackbar.make(binding.root, "This time is already added", Snackbar.LENGTH_SHORT).show()
            return
        }

        currentTimes.add(time)
        currentTimes.sort()
        updateTimesDisplay(currentTimes)
    }

    private fun updateTimesDisplay(times: List<LocalTime>) {
        // Format times as "h:mm a" (12-hour format with AM/PM)
        val formattedTimes = times.joinToString(", ") { it.format(timeFormatter) }
        binding.timesEditText.setText(formattedTimes)

        // Update ViewModel with comma-separated HH:MM format for storage
        val storageFormat = times.joinToString(",") { it.toString() }
        viewModel.updateTimes(storageFormat)
    }

    private fun showRemoveTimeDialog() {
        val currentTimes = parseTimesFromState()
        if (currentTimes.isEmpty()) {
            Snackbar.make(binding.root, "No times to remove", Snackbar.LENGTH_SHORT).show()
            return
        }

        val timeStrings = currentTimes.map { it.format(timeFormatter) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove time")
            .setItems(timeStrings) { _, which ->
                val updatedTimes = currentTimes.toMutableList()
                updatedTimes.removeAt(which)
                updateTimesDisplay(updatedTimes)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStartDatePicker() {
        val currentDate = parseDate(binding.startDateEditText.text?.toString())
        val selection = currentDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select start date")
            .setSelection(selection)
            .build()

        picker.addOnPositiveButtonClickListener { dateMillis ->
            val selectedDate = Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            binding.startDateEditText.setText(selectedDate.format(dateFormatter))
            viewModel.updateStartDate(selectedDate.toString())
        }

        picker.show(childFragmentManager, "START_DATE_PICKER")
    }

    private fun showEndDatePicker() {
        val currentDate = parseDate(binding.endDateEditText.text?.toString())
        val selection = currentDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select end date (optional)")
            .setSelection(selection)
            .build()

        picker.addOnPositiveButtonClickListener { dateMillis ->
            val selectedDate = Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()
            binding.endDateEditText.setText(selectedDate.format(dateFormatter))
            viewModel.updateEndDate(selectedDate.toString())
        }

        picker.show(childFragmentManager, "END_DATE_PICKER")
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(dateStr) }.getOrNull()
            ?: runCatching { LocalDate.parse(dateStr, dateFormatter) }.getOrNull()
    }

    private fun showColorPicker() {
        val currentColor = parseColor(binding.colorEditText.text?.toString()) ?: Color.parseColor("#4CAF50")

        val colors = arrayOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
            "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50",
            "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800",
            "#FF5722", "#795548", "#9E9E9E", "#607D8B", "#000000"
        )

        val colorNames = arrayOf(
            "Red", "Pink", "Purple", "Deep Purple", "Indigo",
            "Blue", "Light Blue", "Cyan", "Teal", "Green",
            "Light Green", "Lime", "Yellow", "Amber", "Orange",
            "Deep Orange", "Brown", "Grey", "Blue Grey", "Black"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select color")
            .setItems(colorNames) { _, which ->
                val selectedColor = colors[which]
                binding.colorEditText.setText(colorNames[which])
                binding.colorEditText.setBackgroundColor(Color.parseColor(selectedColor))
                viewModel.updateColor(selectedColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun parseColor(colorStr: String?): Int? {
        if (colorStr.isNullOrBlank()) return null
        return runCatching {
            val hex = if (colorStr.startsWith("#")) colorStr else "#$colorStr"
            Color.parseColor(hex)
        }.getOrNull()
    }

    private fun getColorName(hex: String): String {
        return when (hex.uppercase()) {
            "#F44336" -> "Red"
            "#E91E63" -> "Pink"
            "#9C27B0" -> "Purple"
            "#673AB7" -> "Deep Purple"
            "#3F51B5" -> "Indigo"
            "#2196F3" -> "Blue"
            "#03A9F4" -> "Light Blue"
            "#00BCD4" -> "Cyan"
            "#009688" -> "Teal"
            "#4CAF50" -> "Green"
            "#8BC34A" -> "Light Green"
            "#CDDC39" -> "Lime"
            "#FFEB3B" -> "Yellow"
            "#FFC107" -> "Amber"
            "#FF9800" -> "Orange"
            "#FF5722" -> "Deep Orange"
            "#795548" -> "Brown"
            "#9E9E9E" -> "Grey"
            "#607D8B" -> "Blue Grey"
            "#000000" -> "Black"
            else -> hex
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_medicine)
            .setPositiveButton(R.string.confirm_delete_positive) { _, _ -> viewModel.delete() }
            .setNegativeButton(R.string.confirm_delete_negative, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
