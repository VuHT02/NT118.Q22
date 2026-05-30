package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var adapter: MyTicketAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tickets)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvTickets = findViewById<RecyclerView>(R.id.rvTickets)
        val layoutEmpty = findViewById<View>(R.id.layoutEmpty)

        adapter = MyTicketAdapter(emptyList()) { ticket ->
            openTicket(ticket)
        }

        rvTickets.layoutManager = LinearLayoutManager(this)
        rvTickets.adapter = adapter

        viewModel.transactions.observe(this) { list ->
            val tickets = list.filter(::isTicketTransaction)
            adapter.updateData(tickets)

            if (tickets.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvTickets.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvTickets.visibility = View.VISIBLE
            }

            updateSummary(tickets)
        }

        viewModel.loadTransactions()
    }

    private fun isTicketTransaction(item: Transaction): Boolean {
        return item.type == "PAYMENT" && (
            item.ticketCode.isNotBlank() ||
            item.routeName.isNotBlank() ||
            item.title.contains("vé", ignoreCase = true)
        )
    }

    private fun updateSummary(list: List<Transaction>) {
        findViewById<TextView>(R.id.tvTotalTickets).text = list.size.toString()
        findViewById<TextView>(R.id.tvTotalValue).text = formatCurrency(list.sumOf { abs(it.amount) })
    }

    private fun openTicket(ticket: Transaction) {
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

