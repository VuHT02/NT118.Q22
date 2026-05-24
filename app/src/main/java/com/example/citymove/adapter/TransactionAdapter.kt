package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.R
import com.example.citymove.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private var list: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView  = v.findViewById(R.id.tvTitle)
        val tvDate: TextView   = v.findViewById(R.id.tvDate)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val iconBg: View       = v.findViewById(R.id.layoutIcon)
        val ivIcon: ImageView  = v.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvDate.text  = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
            .format(Date(item.timestamp))

        if (item.type == "PAYMENT") {
            holder.tvAmount.text = "- ${formatCurrency(item.amount)}"
            holder.tvAmount.setTextColor(0xFFFF5252.toInt())
            holder.iconBg.setBackgroundResource(R.drawable.bg_icon_circle_orange)
            holder.ivIcon.setImageResource(R.drawable.ic_ticket)
        } else {
            holder.tvAmount.text = "+ ${formatCurrency(item.amount)}"
            holder.tvAmount.setTextColor(0xFF4CAF50.toInt())
            holder.iconBg.setBackgroundResource(R.drawable.bg_icon_circle_teal)
            holder.ivIcon.setImageResource(R.drawable.ic_add)
        }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Transaction>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatCurrency(n: Long): String = String.format("%,d₫", n).replace(",", ".")
}
