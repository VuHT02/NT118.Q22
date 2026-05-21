package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.databinding.ItemPlaceSuggestionBinding
import com.google.android.gms.maps.model.LatLng

data class PlaceSuggestion(
    val name: String,
    val address: String,
    val latLng: LatLng
)

class PlaceSuggestionAdapter(
    private val onItemClick: (PlaceSuggestion) -> Unit
) : ListAdapter<PlaceSuggestion, PlaceSuggestionAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(
        private val binding: ItemPlaceSuggestionBinding,
        private val onItemClick: (PlaceSuggestion) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceSuggestion) {
            binding.tvPlaceName.text = item.name
            binding.tvPlaceAddress.text = item.address
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPlaceSuggestionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<PlaceSuggestion>() {
        override fun areItemsTheSame(oldItem: PlaceSuggestion, newItem: PlaceSuggestion): Boolean {
            return oldItem.name == newItem.name && oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: PlaceSuggestion, newItem: PlaceSuggestion): Boolean {
            return oldItem == newItem
        }
    }
}