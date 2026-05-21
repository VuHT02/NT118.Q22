package com.example.citymove.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.R
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.databinding.ItemRouteCardBinding

class RouteAdapter(
    private val onCardClick: (RouteModel) -> Unit,
    private val onCtaClick: (RouteModel) -> Unit
) : ListAdapter<RouteModel, RouteAdapter.RouteViewHolder>(RouteDiffCallback()) {

    inner class RouteViewHolder(private val binding: ItemRouteCardBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: RouteModel) {
            val lineColor = try { Color.parseColor(route.lineColor) } catch (e: Exception) { Color.GRAY }

            binding.viewAccentBar.setBackgroundColor(lineColor)
            binding.tvLineBadge.text = route.lineCode
            binding.tvLineBadge.setTextColor(lineColor)
            binding.frameBadge.background = createBadgeBackground(lineColor)

            binding.tvRouteName.text = route.name
            binding.tvRouteMeta.text = route.metaText

            if (route.status == RouteStatus.ACTIVE) {
                binding.tvStatus.text = "Hoạt động"
                binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_stat_green)
            } else {
                binding.tvStatus.text = "Sắp mở"
                binding.tvStatus.setTextColor(Color.parseColor("#E65100"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_badge_orange)
            }

            binding.tvDuration.text = route.durationText
            binding.lineLeft.setBackgroundColor(lineColor)
            binding.lineRight.setBackgroundColor(lineColor)

            binding.tvStartStation.text = route.startStation
            binding.tvEndStation.text   = route.endStation

            binding.tvPriceLabel.visibility = if (route.showPricePrefix) View.VISIBLE else View.GONE
            binding.tvPrice.text = route.priceOrDateText

            binding.btnCTA.text = route.ctaText
            if (route.status == RouteStatus.ACTIVE) {
                binding.btnCTA.setTextColor(Color.WHITE)
                binding.btnCTA.setBackgroundResource(R.drawable.bg_button_rounded)
                binding.btnCTA.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F97316"))
            } else {
                binding.btnCTA.setTextColor(Color.parseColor("#F97316"))
                binding.btnCTA.setBackgroundResource(R.drawable.bg_button_rounded)
                binding.btnCTA.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
            }

            binding.root.setOnClickListener { onCardClick(route) }
            binding.btnCTA.setOnClickListener   { onCtaClick(route) }
        }

        private fun createBadgeBackground(lineColor: Int): GradientDrawable {
            val bg = GradientDrawable()
            bg.shape = GradientDrawable.RECTANGLE
            bg.cornerRadius = 10f
            val alpha = Color.argb(30, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))
            bg.setColor(alpha)
            return bg
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RouteDiffCallback : DiffUtil.ItemCallback<RouteModel>() {
        override fun areItemsTheSame(oldItem: RouteModel, newItem: RouteModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RouteModel, newItem: RouteModel) = oldItem == newItem
    }
}
