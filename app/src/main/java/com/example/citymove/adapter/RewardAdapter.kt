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
    private var list: List<RewardItem>,
    private val onRedeem: (RewardItem) -> Unit
) : RecyclerView.Adapter<RewardAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.tvRewardTitle)
        val desc: TextView = v.findViewById(R.id.tvRewardDesc)
        val btn: Button = v.findViewById(R.id.btnRedeem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reward, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.title.text = item.title
        holder.desc.text = item.description
        holder.btn.text = "${item.pointsRequired}đ"
        holder.btn.setOnClickListener { onRedeem(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<RewardItem>) {
        this.list = newList
        notifyDataSetChanged()
    }
}
