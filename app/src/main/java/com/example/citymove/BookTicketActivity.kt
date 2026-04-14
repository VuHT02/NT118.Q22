package com.example.citymove

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BookTicketActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_ticket)
        supportActionBar?.hide()

        val routeId = intent.getIntExtra("ROUTE_ID", -1)
        findViewById<TextView>(R.id.tvRouteInfo).text = "Đang đặt vé cho tuyến ID: $routeId"
    }
}