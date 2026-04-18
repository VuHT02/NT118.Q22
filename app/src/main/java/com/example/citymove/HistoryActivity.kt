package com.example.citymove

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.TransactionAdapter
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
        rvHistory.layoutManager = LinearLayoutManager(this)
        
        adapter = TransactionAdapter(emptyList())
        rvHistory.adapter = adapter

        viewModel.transactions.observe(this) { list ->
            adapter.updateData(list)
        }

        viewModel.loadTransactions()
    }
}
