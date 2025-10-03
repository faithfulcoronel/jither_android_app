package com.example.medialert_project.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.medialert_project.R
import com.example.medialert_project.databinding.FragmentDashboardBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    private lateinit var adapter: MedicineAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MedicineAdapter(
            onItemClick = { medicine ->
                findNavController().navigate(
                    R.id.action_dashboardFragment_to_addEditMedicineFragment,
                    bundleOf("medicineId" to medicine.id)
                )
            },
            onDeleteClick = { medicine -> confirmDelete(medicine) }
        )
        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.medicineRecyclerView.adapter = adapter

        binding.addMedicineFab.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addEditMedicineFragment)
        }

        observeState()
        observeEvents()
        setupMenu()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    binding.emptyStateText.isVisible = !state.isLoading && state.medicines.isEmpty()
                    adapter.submitList(state.medicines)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.events.collectLatest { event ->
                when (event) {
                    is DashboardEvent.ShowMessage -> {
                        Snackbar.make(binding.root, getString(event.messageRes), Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: android.view.Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sign_out -> {
                        viewModel.signOut()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.STARTED)
    }

    private fun confirmDelete(medicine: MedicineUiModel) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.confirm_delete_medicine)
            .setPositiveButton(R.string.confirm_delete_positive) { _, _ ->
                viewModel.deleteMedicine(medicine.id)
            }
            .setNegativeButton(R.string.confirm_delete_negative, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
