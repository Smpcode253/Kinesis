package com.aura.assistant.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.assistant.data.db.entities.TrustedContact
import com.aura.assistant.data.db.entities.TrustLevel
import com.aura.assistant.databinding.ItemTrustedContactBinding

/**
 * RecyclerView adapter for displaying the list of trusted contacts in Settings.
 */
class TrustedContactsAdapter(
    private val onEditClick: (TrustedContact) -> Unit,
    private val onDeleteClick: (TrustedContact) -> Unit,
    private val onTrustLevelChange: (TrustedContact, Int) -> Unit
) : ListAdapter<TrustedContact, TrustedContactsAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTrustedContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTrustedContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: TrustedContact) {
            binding.tvContactName.text = contact.name
            binding.tvContactPhone.text = contact.phoneNumber
            binding.tvTrustLevel.text = TrustLevel.toLabel(contact.trustLevel)

            binding.btnEdit.setOnClickListener { onEditClick(contact) }
            binding.btnDelete.setOnClickListener { onDeleteClick(contact) }

            binding.sliderTrustLevel.value = contact.trustLevel.toFloat()
            binding.sliderTrustLevel.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val newLevel = value.toInt()
                    binding.tvTrustLevel.text = TrustLevel.toLabel(newLevel)
                    onTrustLevelChange(contact, newLevel)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TrustedContact>() {
            override fun areItemsTheSame(oldItem: TrustedContact, newItem: TrustedContact) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: TrustedContact, newItem: TrustedContact) =
                oldItem == newItem
        }
    }
}
