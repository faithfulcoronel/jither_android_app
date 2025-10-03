package com.example.medialert_project.ui.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.medialert_project.databinding.ItemMedicineBinding

class MedicineAdapter(
    private val onItemClick: (MedicineUiModel) -> Unit,
    private val onDeleteClick: (MedicineUiModel) -> Unit
) : ListAdapter<MedicineUiModel, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val binding = ItemMedicineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MedicineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MedicineViewHolder(
        private val binding: ItemMedicineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MedicineUiModel) {
            binding.medicineNameText.text = item.name
            binding.medicineDosageText.text = item.dosage
            binding.medicineTimesText.text = item.timesDescription
            val color = runCatching { Color.parseColor(item.colorHex) }.getOrDefault(Color.DKGRAY)
            binding.colorIndicator.setBackgroundColor(color)

            binding.root.setOnClickListener { onItemClick(item) }
            binding.deleteButton.setOnClickListener { onDeleteClick(item) }
        }
    }

    private object MedicineDiffCallback : DiffUtil.ItemCallback<MedicineUiModel>() {
        override fun areItemsTheSame(oldItem: MedicineUiModel, newItem: MedicineUiModel): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MedicineUiModel, newItem: MedicineUiModel): Boolean =
            oldItem == newItem
    }
}
