package com.example.citymove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MetroRoutesActivity : AppCompatActivity() {

    private val routes = listOf(
        RouteData(1, "MRT1", "Metro số 1", "Bến Thành", "Suối Tiên",     RouteType.METRO, "12.000đ", "18 phút"),
        RouteData(8, "MRT1", "Metro số 1", "Bình Thái", "Tham Lương",    RouteType.METRO, "12.000đ", "18 phút"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metro_routes)
        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val container = findViewById<LinearLayout>(R.id.routeListContainer)
        container.removeAllViews()
        routes.forEach { container.addView(buildRouteCard(it)) }
    }

    private fun buildRouteCard(route: RouteData): View {
        val card = CardView(this).apply {
            radius = dpToPx(14).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(10) }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        val badge = TextView(this).apply {
            text = route.code
            setTextColor(Color.WHITE)
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            background = makeRoundedBg("#1565C0", 8)
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
        }

        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = dpToPx(10) }
        }

        textBlock.addView(TextView(this).apply {
            text = route.name
            setTextColor(Color.parseColor("#0b1928"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        textBlock.addView(TextView(this).apply {
            text = "${route.from} → ${route.to}"
            setTextColor(Color.parseColor("#5F6B7A"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(2) }
        })
        textBlock.addView(TextView(this).apply {
            text = "${route.duration} · ${route.fare}"
            setTextColor(Color.parseColor("#9aa5b4"))
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dpToPx(2) }
        })

        val btnView = TextView(this).apply {
            text = "Xem"
            setTextColor(Color.parseColor("#1565C0"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 13f
        }

        row.addView(badge)
        row.addView(textBlock)
        row.addView(btnView)
        card.addView(row)

        card.setOnClickListener {
            startActivity(Intent(this, RouteDetailActivity::class.java).apply {
                putExtra("ROUTE_ID", route.id)
            })
        }

        return card
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    private fun makeRoundedBg(colorHex: String, radiusDp: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.parseColor(colorHex))
            cornerRadius = dpToPx(radiusDp).toFloat()
        }
}
