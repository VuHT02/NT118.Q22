package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.MyTicketAdapter
import com.example.citymove.data.model.Feedback
import com.example.citymove.data.model.Transaction
import com.example.citymove.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class MyTicketsActivity : AppCompatActivity() {

    private enum class TicketFilter { ALL, UNUSED, USED }

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var adapter: MyTicketAdapter
    private lateinit var layoutEmpty: View
    private lateinit var tvEmptyMessage: TextView

    private var allTickets: List<Transaction> = emptyList()
    private var currentFilter: TicketFilter = TicketFilter.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvTickets = findViewById<RecyclerView>(R.id.rvTickets)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)

        val btnFilterAll = findViewById<TextView>(R.id.btnFilterAll)
        val btnFilterUnused = findViewById<TextView>(R.id.btnFilterUnused)
        val btnFilterUsed = findViewById<TextView>(R.id.btnFilterUsed)
        val btnDemo = findViewById<TextView>(R.id.btnDemo)

        adapter = MyTicketAdapter(
            initialList = emptyList(),
            onClick = { ticket -> openTicket(ticket) },
            onFeedbackClick = { ticket -> showFeedbackDialog(ticket) }
        )

        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter

        btnFilterAll.setOnClickListener { applyFilter(TicketFilter.ALL, btnFilterAll, btnFilterUnused, btnFilterUsed) }
        btnFilterUnused.setOnClickListener { applyFilter(TicketFilter.UNUSED, btnFilterAll, btnFilterUnused, btnFilterUsed) }
        btnFilterUsed.setOnClickListener { applyFilter(TicketFilter.USED, btnFilterAll, btnFilterUnused, btnFilterUsed) }

        viewModel.transactions.observe(this) { list ->
            allTickets = list.filter(::isTicketTransaction)
            renderTickets(rvTickets)
        }

        viewModel.feedbackStatus.observe(this) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(this, "Cảm ơn bạn đã phản hồi!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Lỗi: ${it.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
                viewModel.resetFeedbackStatus()
            }
        }

        btnDemo.setOnClickListener { showDemoDialog() }
        
        applyFilter(TicketFilter.ALL, btnFilterAll, btnFilterUnused, btnFilterUsed)
        viewModel.loadTransactions()
    }

    private fun showFeedbackDialog(ticket: Transaction) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_feedback, null)
        val tvRouteName = dialogView.findViewById<TextView>(R.id.tvRouteName)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<EditText>(R.id.etComment)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        tvRouteName.text = ticket.routeName.ifBlank { ticket.title }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val comment = etComment.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val feedback = Feedback(
                routeId = ticket.id, // Hoặc ID tuyến xe nếu có
                routeName = tvRouteName.text.toString(),
                rating = rating,
                comment = comment
            )
            
            viewModel.sendFeedback(feedback)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun isTicketTransaction(item: Transaction): Boolean {
        return item.type == "PAYMENT" && (
            item.ticketCode.isNotBlank() ||
            item.routeName.isNotBlank() ||
            item.title.contains("vé", ignoreCase = true)
        )
    }

    private fun showDemoDialog() {
        if (allTickets.isEmpty()) {
            Toast.makeText(this, "Chưa có vé nào", Toast.LENGTH_SHORT).show()
            return
        }

        val ticketLabels = allTickets.mapIndexed { idx, ticket ->
            val status = if (ticket.isUsed) "✓ Đã dùng" else "⊗ Chưa dùng"
            val route = ticket.routeName.ifBlank { ticket.title.ifBlank { "Vé ${idx + 1}" } }
            "$status - $route"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Demo: Chuyển trạng thái vé")
            .setItems(ticketLabels) { _, which ->
                val selected = allTickets[which]
                toggleTicketStatus(selected)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun toggleTicketStatus(ticket: Transaction) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid ?: return

        val newIsUsed = !ticket.isUsed
        val newExpiryDate = if (newIsUsed) 0L else (System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)

        db.collection("users").document(userId)
            .collection("transactions").document(ticket.id)
            .update("isUsed", newIsUsed, "expiryDate", newExpiryDate)
            .addOnSuccessListener {
                val status = if (newIsUsed) "Đã sử dụng" else "Chưa dùng"
                Toast.makeText(this, "Cập nhật: $status", Toast.LENGTH_SHORT).show()
                viewModel.loadTransactions()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter(
        filter: TicketFilter,
        btnFilterAll: TextView,
        btnFilterUnused: TextView,
        btnFilterUsed: TextView
    ) {
        currentFilter = filter
        updateFilterUi(btnFilterAll, btnFilterUnused, btnFilterUsed)
        renderTickets(findViewById(R.id.rvTickets))
    }

    private fun updateFilterUi(btnFilterAll: TextView, btnFilterUnused: TextView, btnFilterUsed: TextView) {
        val selectedColor = ContextCompat.getColor(this, R.color.white)
        val unselectedColor = ContextCompat.getColor(this, R.color.text_hint)

        val tabs = listOf(
            btnFilterAll to (currentFilter == TicketFilter.ALL),
            btnFilterUnused to (currentFilter == TicketFilter.UNUSED),
            btnFilterUsed to (currentFilter == TicketFilter.USED)
        )

        tabs.forEach { (tab, selected) ->
            tab.setBackgroundResource(if (selected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tab.setTextColor(if (selected) selectedColor else unselectedColor)
        }
    }

    private fun renderTickets(rvTickets: RecyclerView) {
        val visibleTickets = filteredTickets()
        adapter.updateData(visibleTickets)

        if (visibleTickets.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvTickets.visibility = View.GONE
            tvEmptyMessage.text = when {
                allTickets.isEmpty() -> "Chưa có vé nào"
                currentFilter == TicketFilter.USED -> "Chưa có vé đã sử dụng"
                currentFilter == TicketFilter.UNUSED -> "Chưa có vé chưa sử dụng"
                else -> "Chưa có vé nào"
            }
        } else {
            layoutEmpty.visibility = View.GONE
            rvTickets.visibility = View.VISIBLE
        }

        updateSummary(visibleTickets)
    }

    private fun filteredTickets(): List<Transaction> {
        return when (currentFilter) {
            TicketFilter.ALL -> allTickets
            TicketFilter.UNUSED -> allTickets.filterNot { it.isUsed }
            TicketFilter.USED -> allTickets.filter { it.isUsed }
        }
    }

    private fun updateSummary(list: List<Transaction>) {
        findViewById<TextView>(R.id.tvTotalTickets).text = list.size.toString()
        findViewById<TextView>(R.id.tvTotalValue).text = formatCurrency(list.sumOf { abs(it.amount) })
    }

    private fun openTicket(ticket: Transaction) {
        if (ticket.isUsed) {
            // Toast.makeText(this, "Vé này đã được sử dụng", Toast.LENGTH_SHORT).show()
            return
        }

        val ticketCode = ticket.ticketCode.trim()
        if (ticketCode.isEmpty()) {
            Toast.makeText(this, "Vé này chưa có mã QR", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, TicketQrActivity::class.java).apply {
            putExtra(TicketQrActivity.EXTRA_TICKET_CODE, ticketCode)
            putExtra(TicketQrActivity.EXTRA_ROUTE_NAME, ticket.routeName.ifBlank { ticket.title.ifBlank { "Vé của tôi" } })
            putExtra(TicketQrActivity.EXTRA_QUANTITY, ticket.quantity.coerceAtLeast(1))
            putExtra(TicketQrActivity.EXTRA_TOTAL_PRICE, abs(ticket.amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
            putExtra(TicketQrActivity.EXTRA_DATE, ticket.date.ifBlank { formatDate(ticket.timestamp) })
        }
        startActivity(intent)
    }

    private fun formatDate(timestamp: Long): String {
        return if (timestamp > 0L) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else {
            ""
        }
    }

    private fun formatCurrency(amount: Long): String = String.format("%,dđ", amount).replace(",", ".")
}
