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
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MyTicketAdapter(
    private var list: List<Transaction>,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<MyTicketAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvCode: TextView = v.findViewById(R.id.tvCode)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvAmount: TextView = v.findViewById(R.id.tvAmount)
        val tvAction: TextView = v.findViewById(R.id.tvAction)
        val iconBg: View = v.findViewById(R.id.layoutIcon)
        val ivIcon: ImageView = v.findViewById(R.id.ivIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_ticket, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        val routeLabel = item.routeName.ifBlank { item.title.ifBlank { "Vé của tôi" } }
        val codeLabel = if (item.ticketCode.isNotBlank()) "Mã vé: ${item.ticketCode}" else "Mã vé chưa khả dụng"
        val dateLabel = if (item.date.isNotBlank()) item.date else {
            SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(item.timestamp))
        }

        holder.tvTitle.text = routeLabel
        holder.tvCode.text = codeLabel
        holder.tvDate.text = dateLabel
        holder.tvAmount.text = formatCurrency(abs(item.amount))
        holder.tvAction.text = if (item.ticketCode.isNotBlank()) "Xem QR" else "Chưa có QR"
        holder.tvAction.isEnabled = item.ticketCode.isNotBlank()

        holder.iconBg.setBackgroundResource(R.drawable.bg_icon_circle_orange)
        holder.ivIcon.setImageResource(R.drawable.ic_ticket)

        holder.itemView.setOnClickListener { onClick(item) }
        holder.tvAction.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size

    fun updateData(newList: List<Transaction>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatCurrency(n: Long): String = String.format("%,d₫", n).replace(",", ".")
}

