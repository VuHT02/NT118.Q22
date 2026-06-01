package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.R
import com.example.citymove.data.model.RewardItem

class RewardAdapter(
    private var rewards: List<RewardItem>,
    private val onExchangeClick: (RewardItem) -> Unit
) : RecyclerView.Adapter<RewardAdapter.RewardViewHolder>() {

    class RewardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnExchange: Button = view.findViewById(R.id.btnExchange)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reward, parent, false)
        return RewardViewHolder(view)
    }

    override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
        val item = rewards[position]
        holder.tvTitle.text = item.title
        holder.tvDescription.text = item.description
        holder.btnExchange.text = "${item.points} pts"
        holder.btnExchange.setOnClickListener { onExchangeClick(item) }
    }

    override fun getItemCount() = rewards.size

    fun updateData(newRewards: List<RewardItem>) {
        this.rewards = newRewards
        notifyDataSetChanged()
    }
}
