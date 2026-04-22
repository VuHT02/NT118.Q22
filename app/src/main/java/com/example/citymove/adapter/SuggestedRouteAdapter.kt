package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.SearchMapActivity
import com.example.citymove.databinding.ItemSuggestedRouteBinding

class SuggestedRouteAdapter(
    private val onItemClick: (SearchMapActivity.SuggestedRoute) -> Unit
) : ListAdapter<SearchMapActivity.SuggestedRoute, SuggestedRouteAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSuggestedRouteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSuggestedRouteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SearchMapActivity.SuggestedRoute) {
            binding.tvRouteNumber.text = item.routeNumber
            binding.tvRouteName.text = item.routeName
            binding.tvNextArrival.text = "${item.nextArrivalMin}p nữa"
            binding.tvBoardAt.text = item.boardAt
            binding.tvAlightAt.text = item.alightAt
            
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchMapActivity.SuggestedRoute>() {
        override fun areItemsTheSame(
            oldItem: SearchMapActivity.SuggestedRoute,
            newItem: SearchMapActivity.SuggestedRoute
        ): Boolean = oldItem.routeId == newItem.routeId

        override fun areContentsTheSame(
            oldItem: SearchMapActivity.SuggestedRoute,
            newItem: SearchMapActivity.SuggestedRoute
        ): Boolean = oldItem == newItem
    }
}
