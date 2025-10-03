package com.example.medialert_project.ui.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditMedicineFragment : Fragment() {

    private var _binding: FragmentAddEditMedicineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditMedicineViewModel by viewModels()

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
        setupInputs()
        observeState()
        observeEvents()
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
        binding.colorEditText.doAfterTextChanged { viewModel.updateColor(it?.toString().orEmpty()) }
        binding.startDateEditText.doAfterTextChanged { viewModel.updateStartDate(it?.toString().orEmpty()) }
        binding.endDateEditText.doAfterTextChanged { viewModel.updateEndDate(it?.toString().orEmpty()) }
        binding.timesEditText.doAfterTextChanged { viewModel.updateTimes(it?.toString().orEmpty()) }
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
                    if (binding.colorEditText.text?.toString() != state.colorHex) {
                        binding.colorEditText.setText(state.colorHex)
                    }
                    if (binding.startDateEditText.text?.toString() != state.startDate) {
                        binding.startDateEditText.setText(state.startDate)
                    }
                    if (binding.endDateEditText.text?.toString() != state.endDate) {
                        binding.endDateEditText.setText(state.endDate)
                    }
                    if (binding.timesEditText.text?.toString() != state.times) {
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
