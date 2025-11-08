package com.example.medialert_project.ui.history

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medialert_project.R
import com.example.medialert_project.databinding.ItemDoseLogBinding

class HistoryAdapter : ListAdapter<DoseLogUiModel, HistoryAdapter.DoseLogViewHolder>(DoseLogDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseLogViewHolder {
        val binding = ItemDoseLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DoseLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoseLogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DoseLogViewHolder(
        private val binding: ItemDoseLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DoseLogUiModel) {
            binding.medicineNameText.text = item.medicineName
            binding.dosageText.text = item.dosage
            binding.scheduledTimeText.text = "Scheduled: ${item.scheduledTime}"

            // Set status with appropriate color
            binding.statusText.text = item.status
            val statusColorRes = when (item.status) {
                "TAKEN" -> R.color.status_taken
                "MISSED" -> R.color.status_missed
                "SKIPPED" -> R.color.status_skipped
                else -> R.color.status_pending
            }
            val statusColor = ContextCompat.getColor(binding.root.context, statusColorRes)
            binding.statusText.chipBackgroundColor = ColorStateList.valueOf(statusColor)

            // Show acted time based on status
            if (item.actedTime != null) {
                binding.actedTimeText.isVisible = true
                val actedLabel = when (item.status) {
                    "TAKEN" -> "Taken"
                    "SKIPPED" -> "Skipped"
                    else -> "Acted"
                }
                binding.actedTimeText.text = "$actedLabel: ${item.actedTime}"
            } else {
                binding.actedTimeText.isVisible = false
            }
        }
    }

    private object DoseLogDiffCallback : DiffUtil.ItemCallback<DoseLogUiModel>() {
        override fun areItemsTheSame(oldItem: DoseLogUiModel, newItem: DoseLogUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DoseLogUiModel, newItem: DoseLogUiModel): Boolean =
            oldItem == newItem
    }
}
