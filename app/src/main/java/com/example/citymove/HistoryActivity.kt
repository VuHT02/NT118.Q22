package com.example.citymove

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.TransactionAdapter
import com.example.citymove.data.model.Transaction
import com.example.citymove.viewmodel.AccountViewModel

class HistoryActivity : AppCompatActivity() {

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val layoutEmpty = findViewById<View>(R.id.layoutEmpty)

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(emptyList())
        rvHistory.adapter = adapter

        viewModel.transactions.observe(this) { list ->
            adapter.updateData(list)

            if (list.isEmpty()) {
                layoutEmpty.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            } else {
                layoutEmpty.visibility = View.GONE
                rvHistory.visibility = View.VISIBLE
            }

            updateSummary(list)
        }

        viewModel.loadTransactions()
    }

    private fun updateSummary(list: List<Transaction>) {
        val totalTopup = list.filter { it.type == "TOP_UP" }.sumOf { it.amount }
        val totalSpent = list.filter { it.type == "PAYMENT" }.sumOf { it.amount }

        findViewById<TextView>(R.id.tvTotalTransactions).text = list.size.toString()
        findViewById<TextView>(R.id.tvTotalTopup).text = formatCurrency(totalTopup)
        findViewById<TextView>(R.id.tvTotalSpent).text = formatCurrency(totalSpent)
    }


    private fun formatCurrency(n: Long) = String.format("%,dđ", n).replace(",", ".")
}