package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.MyTicketAdapter
import com.example.citymove.data.model.Transaction
import com.example.citymove.viewmodel.AccountViewModel
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

        adapter = MyTicketAdapter(emptyList()) { ticket ->
            openTicket(ticket)
        }

        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter

        btnFilterAll.setOnClickListener { applyFilter(TicketFilter.ALL, btnFilterAll, btnFilterUnused, btnFilterUsed) }
        btnFilterUnused.setOnClickListener { applyFilter(TicketFilter.UNUSED, btnFilterAll, btnFilterUnused, btnFilterUsed) }
        btnFilterUsed.setOnClickListener { applyFilter(TicketFilter.USED, btnFilterAll, btnFilterUnused, btnFilterUsed) }

        viewModel.transactions.observe(this) { list ->
            allTickets = list.filter(::isTicketTransaction)
            renderTickets(rvTickets)
        }

        applyFilter(TicketFilter.ALL, btnFilterAll, btnFilterUnused, btnFilterUsed)
        viewModel.loadTransactions()
    }

    private fun isTicketTransaction(item: Transaction): Boolean {
        return item.type == "PAYMENT" && (
            item.ticketCode.isNotBlank() ||
            item.routeName.isNotBlank() ||
            item.title.contains("vé", ignoreCase = true)
        )
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
            Toast.makeText(this, "Vé này đã được sử dụng", Toast.LENGTH_SHORT).show()
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

