package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.databinding.ItemSuggestedRouteBinding

data class SuggestedRoute(
    val routeId: String,
    val routeNumber: String,
    val routeName: String,
    val schedule: String,
    val frequency: String,
    val fare: String,
    val boardAt: String,
    val alightAt: String,
    val nextArrivalMin: Int,
    val distance: String = "",
    val duration: String = "",
    val transfers: Int = 0
)

class SuggestedRouteAdapter(
    private val onItemClick: (SuggestedRoute) -> Unit
) : ListAdapter<SuggestedRoute, SuggestedRouteAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(
        private val binding: ItemSuggestedRouteBinding,
        private val onItemClick: (SuggestedRoute) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SuggestedRoute) {
            binding.tvRouteNumber.text = item.routeNumber
            binding.tvRouteName.text = item.routeName
            binding.tvFare.text = item.fare

            binding.tvDistance.text = item.distance
            binding.tvTime.text = item.duration
            binding.tvTransfers.text = "${item.transfers} chuyến"

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSuggestedRouteBinding.inflate(
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

    class DiffCallback : DiffUtil.ItemCallback<SuggestedRoute>() {
        override fun areItemsTheSame(oldItem: SuggestedRoute, newItem: SuggestedRoute): Boolean {
            return oldItem.routeId == newItem.routeId
        }

        override fun areContentsTheSame(oldItem: SuggestedRoute, newItem: SuggestedRoute): Boolean {
            return oldItem == newItem
        }
    }
}
