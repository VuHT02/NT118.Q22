package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class AllRoutesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_routes)
        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.itemMetroM1).setOnClickListener {
            openRouteDetail(101)
        }
        findViewById<View>(R.id.itemBus01).setOnClickListener {
            openRouteDetail(1)
        }
        findViewById<View>(R.id.itemBusW01).setOnClickListener {
            openRouteDetail(3)
        }
    }

    private fun openRouteDetail(routeId: Int) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra("ROUTE_ID", routeId)
        startActivity(intent)
    }
}
