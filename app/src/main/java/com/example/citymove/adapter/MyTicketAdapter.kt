package com.example.citymove.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.R
import com.example.citymove.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MyTicketAdapter(
    initialList: List<Transaction> = emptyList(),
    private val onClick: (Transaction) -> Unit
) : ListAdapter<Transaction, MyTicketAdapter.VH>(Diff) {

    init {
        submitList(initialList.toList())
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvCode: TextView = v.findViewById(R.id.tvCode)
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvExpiry: TextView = v.findViewById(R.id.tvExpiry)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
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
        val item = getItem(position)
        val isExpired = isTicketExpired(item)
        val effectiveUsed = item.isUsed || isExpired

        val routeLabel = item.routeName.ifBlank { item.title.ifBlank { "Vé của tôi" } }
        val codeLabel = if (item.ticketCode.isNotBlank()) "Mã vé: ${item.ticketCode}" else "Mã vé chưa khả dụng"
        val dateLabel = if (item.date.isNotBlank()) item.date else {
            SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(item.timestamp))
        }
        val accentColor = resolveAccentColor(holder.itemView, item)
        val mutedColor = ContextCompat.getColor(holder.itemView.context, R.color.text_hint)
        val secondaryColor = ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        val statusText = when {
            item.isUsed -> "Đã sử dụng"
            isExpired -> "Hết hạn"
            else -> getExpiryText(item.expiryDate)
        }
        val statusTextColor = when {
            item.isUsed -> secondaryColor
            isExpired -> ContextCompat.getColor(holder.itemView.context, R.color.white)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.white)
        }
        val actionText = when {
            effectiveUsed -> "Đã sử dụng"
            item.ticketCode.isNotBlank() -> "Xem QR"
            else -> "Chưa có QR"
        }

        holder.tvTitle.text = routeLabel
        holder.tvCode.text = codeLabel
        holder.tvDate.text = dateLabel
        holder.tvExpiry.text = formatExpiryDate(item.expiryDate)
        holder.tvAmount.text = formatCurrency(abs(item.amount))
        holder.tvStatus.text = statusText
        holder.tvAction.text = actionText
        holder.tvAction.isEnabled = !effectiveUsed && item.ticketCode.isNotBlank()

        holder.tvTitle.setTextColor(if (effectiveUsed) secondaryColor else accentColor)
        holder.tvAmount.setTextColor(if (effectiveUsed) mutedColor else accentColor)
        holder.tvCode.setTextColor(if (effectiveUsed) mutedColor else secondaryColor)
        holder.tvDate.setTextColor(if (effectiveUsed) mutedColor else secondaryColor)

        holder.tvStatus.setBackgroundResource(
            if (effectiveUsed) R.drawable.bg_tab_unselected else if (isExpired) R.drawable.bg_tab_unselected else R.drawable.bg_tab_selected
        )
        holder.tvStatus.setTextColor(statusTextColor)

        holder.tvAction.setBackgroundResource(
            if (effectiveUsed) R.drawable.bg_tab_unselected else R.drawable.bg_tab_selected
        )
        holder.tvAction.setTextColor(
            if (effectiveUsed) secondaryColor else ContextCompat.getColor(holder.itemView.context, R.color.white)
        )

        holder.iconBg.setBackgroundResource(R.drawable.bg_icon_circle_orange)
        holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (effectiveUsed) ContextCompat.getColor(holder.itemView.context, R.color.bg_icon_gray) else accentColor
        )
        holder.ivIcon.setImageResource(R.drawable.ic_ticket)
        holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.white))

        holder.itemView.setOnClickListener { onClick(item) }
        holder.tvAction.setOnClickListener { onClick(item) }
    }

    fun updateData(newList: List<Transaction>) {
        submitList(newList.toList())
    }

    private fun formatCurrency(n: Long): String = String.format(Locale.getDefault(), "%,d₫", n).replace(",", ".")

    private fun resolveAccentColor(itemView: View, item: Transaction): Int {
        val context = itemView.context
        return when (item.transport.lowercase(Locale.getDefault())) {
            "bus" -> ContextCompat.getColor(context, R.color.orange_primary)
            "metro" -> ContextCompat.getColor(context, R.color.blue_primary)
            "waterbus", "water_bus" -> ContextCompat.getColor(context, R.color.green_500)
            else -> when (item.ticketType.lowercase(Locale.getDefault())) {
                "month", "monthly" -> ContextCompat.getColor(context, R.color.blue_primary)
                "single" -> ContextCompat.getColor(context, R.color.orange_primary)
                else -> ContextCompat.getColor(context, R.color.orange_primary)
            }
        }
    }

    private fun isTicketExpired(item: Transaction): Boolean {
        return item.expiryDate > 0 && System.currentTimeMillis() > item.expiryDate
    }

    private fun getExpiryText(expiryDate: Long): String {
        if (expiryDate <= 0) return "Không hạn"
        val now = System.currentTimeMillis()
        val diff = expiryDate - now

        return when {
            diff < 0 -> "Hết hạn"
            diff < 24 * 60 * 60 * 1000 -> "Còn < 1 ngày"
            else -> {
                val daysLeft = diff / (24 * 60 * 60 * 1000)
                "Còn $daysLeft ngày"
            }
        }
    }

    private fun formatExpiryDate(expiryDate: Long): String {
        if (expiryDate <= 0) return ""
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "Hết hạn: ${sdf.format(Date(expiryDate))}"
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem.id.isNotBlank() && oldItem.id == newItem.id ||
                    oldItem.ticketCode.isNotBlank() && oldItem.ticketCode == newItem.ticketCode
            }

            override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
                return oldItem == newItem
            }
        }
    }
}

