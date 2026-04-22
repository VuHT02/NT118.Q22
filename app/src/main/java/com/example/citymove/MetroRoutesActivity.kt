package com.example.citymove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class MetroRoutesActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var container: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metro_routes)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        container    = findViewById(R.id.routeListContainer)
        progressBar  = findViewById(R.id.progressBar)
        tvEmpty      = findViewById(R.id.tvEmpty)

        loadRoutesFromFirestore()
    }

    // ─────────────────────────────────────────────────────────
    // LOAD TỪ FIRESTORE
    // Cấu trúc Firestore:
    // routes/{routeId} → { code, name, from, to, fare, duration, type: "metro" }
    // ─────────────────────────────────────────────────────────
    private fun loadRoutesFromFirestore() {
        progressBar.visibility = View.VISIBLE
        tvEmpty.visibility     = View.GONE
        // container.removeAllViews() // Commented out to keep static cards if needed, or keep if you only want dynamic data.
        // Usually, if we have static cards in XML and also load from Firestore, we might want to keep both or clear.
        // Given the layout has cardMetro1, cardMetro2, cardMetro3, let's just append for now or the user can decide.
        // If we want to support the search/tab functionality properly, we should probably clear.

        db.collection("routes")
            .whereEqualTo("type", "metro")
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE

                if (snapshot.isEmpty) {
                    // If we have static cards, we might not want to show "Empty" if they are visible.
                    // But usually dynamic loading replaces or adds to it.
                    // tvEmpty.visibility = View.VISIBLE
                    // tvEmpty.text       = "Không có tuyến Metro nào"
                    return@addOnSuccessListener
                }

                snapshot.documents.forEach { doc ->
                    val route = RouteData(
                        id       = doc.getLong("id")?.toInt() ?: 0,
                        code     = doc.getString("code") ?: "M",
                        name     = doc.getString("name") ?: "",
                        from     = doc.getString("from") ?: "",
                        to       = doc.getString("to") ?: "",
                        type     = RouteType.METRO,
                        fare     = doc.getString("fare") ?: "0đ",
                        duration = doc.getString("duration") ?: ""
                    )
                    container.addView(buildRouteCard(route))
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                tvEmpty.visibility     = View.VISIBLE
                tvEmpty.text           = "Lỗi tải dữ liệu: ${e.message}"
            }
    }

    // ─────────────────────────────────────────────────────────
    // BUILD CARD
    // ─────────────────────────────────────────────────────────
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
