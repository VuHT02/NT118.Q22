package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.SearchMapActivity
import com.example.citymove.databinding.ItemPlaceSuggestionBinding

class PlaceSuggestionAdapter(
    private val onItemClick: (SearchMapActivity.PlaceSuggestion) -> Unit
) : ListAdapter<SearchMapActivity.PlaceSuggestion, PlaceSuggestionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPlaceSuggestionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemPlaceSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchMapActivity.PlaceSuggestion) {
            binding.tvPlaceName.text = item.name
            binding.tvPlaceAddress.text = item.address
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchMapActivity.PlaceSuggestion>() {
        override fun areItemsTheSame(
            oldItem: SearchMapActivity.PlaceSuggestion,
            newItem: SearchMapActivity.PlaceSuggestion
        ): Boolean = oldItem.name == newItem.name && oldItem.latLng == newItem.latLng

        override fun areContentsTheSame(
            oldItem: SearchMapActivity.PlaceSuggestion,
            newItem: SearchMapActivity.PlaceSuggestion
        ): Boolean = oldItem == newItem
    }
}
