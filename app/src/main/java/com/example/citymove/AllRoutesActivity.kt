package com.example.citymove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

// ── Data model ────────────────────────────────────────────────────
data class RouteData(
    val id       : Int,
    val code     : String,
    val name     : String,
    val from     : String,
    val to       : String,
    val type     : RouteType,
    val fare     : String,
    val duration : String,
)

enum class RouteType(
    val label      : String,
    val badgeColor : String,
    val textColor  : String,
    val sectionColor: String,
) {
    METRO   ("Metro",    "#1565C0", "#FFFFFF", "#1565C0"),
    BUS     ("Xe buýt",  "#F97316", "#FFFFFF", "#F97316"),
    WATERBUS("Waterbus", "#0F766E", "#FFFFFF", "#0F766E"),
}

class AllRoutesActivity : AppCompatActivity() {

    // ── Danh sách tuyến — thêm tuyến mới vào đây ─────────────────
    private val routes = listOf(
        RouteData(1,  "MRT1",   "Metro số 1",     "Bến Thành",  "Suối Tiên",   RouteType.METRO,    "12.000đ", "18 phút"),
        RouteData(2,  "BUS 01", "Buýt số 01",     "Bến Thành",  "Suối Tiên",   RouteType.BUS,      "10.000đ", "23 phút"),
        RouteData(3,  "BUS 02", "Buýt số 02",     "Bến Thành",  "Chợ Lớn",     RouteType.BUS,      "10.000đ", "20 phút"),
        RouteData(4,  "BUS 36", "Buýt số 36",     "Cơ quan",    "Nhà",         RouteType.BUS,      "10.000đ", "30 phút"),
        RouteData(5,  "W01",    "Waterbus W01",   "Bạch Đằng", "Linh Đông",   RouteType.WATERBUS, "15.000đ", "35 phút"),
        RouteData(6,  "W02",    "Waterbus W02",   "Bạch Đằng", "Lò Gốm",     RouteType.WATERBUS, "15.000đ", "40 phút"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_routes)
        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        renderRoutes()
    }

    private fun renderRoutes() {
        val container = findViewById<LinearLayout>(R.id.routeListContainer)
        container.removeAllViews()

        // Group theo type
        val grouped = routes.groupBy { it.type }

        // Thứ tự hiển thị
        listOf(RouteType.METRO, RouteType.BUS, RouteType.WATERBUS).forEach { type ->
            val group = grouped[type] ?: return@forEach

            // Section label
            val label = TextView(this).apply {
                text = type.label
                setTextColor(Color.parseColor(type.sectionColor))
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dpToPx(12)
                lp.bottomMargin = dpToPx(8)
                layoutParams = lp
            }
            container.addView(label)

            // Route cards
            group.forEach { route ->
                container.addView(buildRouteCard(route))
            }
        }
    }

    private fun buildRouteCard(route: RouteData): View {
        val card = CardView(this).apply {
            radius = dpToPx(14).toFloat()
            cardElevation = dpToPx(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dpToPx(10)
            layoutParams = lp
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))
        }

        // Badge code
        val badge = TextView(this).apply {
            text = route.code
            setTextColor(Color.parseColor(route.type.textColor))
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setBackgroundColor(Color.parseColor(route.type.badgeColor))
            setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6))
            background = makeRoundedBg(route.type.badgeColor, 8)
        }

        // Text block
        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = dpToPx(10)
            layoutParams = lp
        }

        val tvName = TextView(this).apply {
            text = route.name
            setTextColor(Color.parseColor("#0b1928"))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val tvRoute = TextView(this).apply {
            text = "${route.from} → ${route.to}"
            setTextColor(Color.parseColor("#5F6B7A"))
            textSize = 12f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dpToPx(2)
            layoutParams = lp
        }

        val tvMeta = TextView(this).apply {
            text = "${route.duration} · ${route.fare}"
            setTextColor(Color.parseColor("#9aa5b4"))
            textSize = 11f
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dpToPx(2)
            layoutParams = lp
        }

        textBlock.addView(tvName)
        textBlock.addView(tvRoute)
        textBlock.addView(tvMeta)

        // Nút Xem
        val btnView = TextView(this).apply {
            text = "Xem"
            setTextColor(Color.parseColor(route.type.sectionColor))
            setTypeface(null, android.graphics.Typeface.BOLD)
            textSize = 13f
        }

        row.addView(badge)
        row.addView(textBlock)
        row.addView(btnView)
        card.addView(row)

        // Click → RouteDetailActivity
        card.setOnClickListener {
            startActivity(
                Intent(this, RouteDetailActivity::class.java).apply {
                    putExtra("ROUTE_ID", route.id)
                }
            )
        }

        return card
    }

    // ── Helpers ───────────────────────────────────────────────────
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun makeRoundedBg(colorHex: String, radiusDp: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(Color.parseColor(colorHex))
            cornerRadius = dpToPx(radiusDp).toFloat()
        }
    }
}