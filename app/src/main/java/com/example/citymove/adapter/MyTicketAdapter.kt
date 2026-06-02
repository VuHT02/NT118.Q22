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
    private val onClick: (Transaction) -> Unit,
    private val onFeedbackClick: (Transaction) -> Unit
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
        val orangeColor = ContextCompat.getColor(holder.itemView.context, R.color.orange_primary)
        val mutedColor = ContextCompat.getColor(holder.itemView.context, R.color.text_hint)
        val secondaryColor = ContextCompat.getColor(holder.itemView.context, R.color.text_secondary)
        
        val statusText = when {
            item.isUsed -> "Đã sử dụng"
            isExpired -> "Hết hạn"
            else -> getExpiryText(item.expiryDate)
        }
        
        // Logic mới cho nút Action
        val actionText = when {
            item.isUsed -> "Đánh giá"
            isExpired -> "Đã hết hạn"
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
        
        // Cấu hình trạng thái nút
        if (item.isUsed) {
            holder.tvAction.isEnabled = true
            holder.tvAction.setBackgroundResource(R.drawable.bg_tab_selected)
            holder.tvAction.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.tvAction.backgroundTintList = android.content.res.ColorStateList.valueOf(orangeColor)
        } else {
            val canViewQr = !isExpired && item.ticketCode.isNotBlank()
            holder.tvAction.isEnabled = canViewQr
            holder.tvAction.setBackgroundResource(if (canViewQr) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            holder.tvAction.setTextColor(if (canViewQr) ContextCompat.getColor(holder.itemView.context, R.color.white) else secondaryColor)
        }

        holder.tvTitle.setTextColor(if (effectiveUsed) secondaryColor else accentColor)
        holder.tvAmount.setTextColor(if (effectiveUsed) mutedColor else accentColor)
        
        holder.iconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (effectiveUsed) ContextCompat.getColor(holder.itemView.context, R.color.bg_icon_gray) else accentColor
        )

        holder.tvAction.setOnClickListener {
            if (item.isUsed) {
                onFeedbackClick(item)
            } else {
                onClick(item)
            }
        }
        
        holder.itemView.setOnClickListener { onClick(item) }
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
            else -> ContextCompat.getColor(context, R.color.orange_primary)
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
            else -> "Còn ${diff / (24 * 60 * 60 * 1000)} ngày"
        }
    }

    private fun formatExpiryDate(expiryDate: Long): String {
        if (expiryDate <= 0) return ""
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            "Hết hạn: ${sdf.format(Date(expiryDate))}"
        } catch (e: Exception) { "" }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction) = oldItem == newItem
        }
    }
}
