package com.example.citymove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.TransportType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookTicketActivity : AppCompatActivity() {

    private lateinit var progressBar:  ProgressBar
    private lateinit var cardRoute:    CardView
    private lateinit var cardQuantity: CardView
    private lateinit var cardSummary:  CardView
    private lateinit var layoutBottom: LinearLayout

    private lateinit var tvLineCode:   TextView
    private lateinit var tvRouteName:  TextView
    private lateinit var tvStart:      TextView
    private lateinit var tvEnd:        TextView
    private lateinit var tvUnitPrice:  TextView

    private lateinit var btnMinus:     FrameLayout
    private lateinit var btnPlus:      FrameLayout
    private lateinit var tvQuantity:   TextView

    private lateinit var tvSummaryQty:  TextView
    private lateinit var tvTotalPrice:  TextView
    private lateinit var btnBuyTicket:  Button

    private lateinit var db:   FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var route: RouteModel? = null
    private var quantity = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_ticket)
        supportActionBar?.hide()

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        bindViews()
        setupBack()
        setupQuantityButtons()
        setupBuyButton()

        val routeId = intent.getStringExtra("ROUTE_ID") ?: intent.getIntExtra("ROUTE_ID", -1).let {
            if (it == -1) null else it.toString()
        }

        if (routeId == null) {
            Toast.makeText(this, "Không tìm thấy tuyến đường", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadRoute(routeId)
    }

    private fun bindViews() {
        progressBar  = findViewById(R.id.progressBar)
        cardRoute    = findViewById(R.id.cardRoute)
        cardQuantity = findViewById(R.id.cardQuantity)
        cardSummary  = findViewById(R.id.cardSummary)
        layoutBottom = findViewById(R.id.layoutBottom)

        tvLineCode   = findViewById(R.id.tvLineCode)
        tvRouteName  = findViewById(R.id.tvRouteName)
        tvStart      = findViewById(R.id.tvStart)
        tvEnd        = findViewById(R.id.tvEnd)
        tvUnitPrice  = findViewById(R.id.tvUnitPrice)

        btnMinus     = findViewById(R.id.btnMinus)
        btnPlus      = findViewById(R.id.btnPlus)
        tvQuantity   = findViewById(R.id.tvQuantity)

        tvSummaryQty = findViewById(R.id.tvSummaryQty)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        btnBuyTicket = findViewById(R.id.btnBuyTicket)
    }

    private fun setupBack() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupQuantityButtons() {
        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityUI()
            }
        }
        btnPlus.setOnClickListener {
            if (quantity < 10) {
                quantity++
                updateQuantityUI()
            }
        }
    }

    private fun updateQuantityUI() {
        tvQuantity.text = quantity.toString()
        val price = route?.price ?: 0
        tvSummaryQty.text = "$quantity vé"
        tvTotalPrice.text = formatPrice(price * quantity)
    }

    private fun setupBuyButton() {
        btnBuyTicket.setOnClickListener { confirmPurchase() }
    }

    private fun loadRoute(routeId: String) {
        progressBar.visibility = View.VISIBLE

        db.collection("routes").document(routeId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Không tìm thấy tuyến đường", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val r = RouteModel(
                    id              = doc.id,
                    type            = TransportType.fromString(doc.getString("type") ?: "BUS"),
                    lineCode        = doc.getString("lineCode") ?: "",
                    name            = doc.getString("name") ?: "",
                    startStation    = doc.getString("startStation") ?: "",
                    endStation      = doc.getString("endStation") ?: "",
                    distanceKm      = doc.getDouble("distanceKm")
                                      ?: doc.getLong("distanceKm")?.toDouble() ?: 0.0,
                    stationCount    = doc.getLong("stationCount")?.toInt() ?: 0,
                    durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                    status          = try {
                        RouteStatus.valueOf(doc.getString("status")?.uppercase() ?: "ACTIVE")
                    } catch (e: Exception) { RouteStatus.ACTIVE },
                    lineColor       = doc.getString("lineColor") ?: "#F97316",
                    price           = doc.getLong("price")?.toInt()
                                      ?: doc.getDouble("price")?.toInt(),
                    expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt()
                )

                if (r.status != RouteStatus.ACTIVE || r.price == null) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Tuyến này chưa mở bán vé", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                route = r
                progressBar.visibility = View.GONE
                showRouteUI(r)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun showRouteUI(r: RouteModel) {
        val lineColor = try { Color.parseColor(r.lineColor) } catch (e: Exception) { Color.parseColor("#F97316") }

        tvLineCode.text  = r.lineCode
        tvRouteName.text = r.name
        tvStart.text     = r.startStation
        tvEnd.text       = r.endStation
        tvUnitPrice.text = formatPrice(r.price ?: 0)

        val badge = findViewById<FrameLayout>(R.id.frameBadge)
        badge.backgroundTintList = android.content.res.ColorStateList.valueOf(lineColor)
        tvLineCode.setTextColor(android.graphics.Color.WHITE)

        val accent = findViewById<View>(R.id.viewAccent)
        accent.setBackgroundColor(lineColor)

        updateQuantityUI()

        cardRoute.visibility    = View.VISIBLE
        cardQuantity.visibility = View.VISIBLE
        cardSummary.visibility  = View.VISIBLE
        layoutBottom.visibility = View.VISIBLE
    }

    private fun confirmPurchase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        val r = route ?: return
        val price = r.price ?: return
        val total = price * quantity

        btnBuyTicket.isEnabled = false
        btnBuyTicket.text = "Đang xử lý..."

        val ticketCode = generateTicketCode()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(now))

        val transaction = mapOf(
            "title"     to "Vé ${r.type.displayName} ${r.lineCode}",
            "amount"    to -total.toLong(),
            "type"      to "PAYMENT",
            "timestamp" to now,
            "date"      to dateStr,
            "routeId"   to r.id,
            "routeName" to r.name,
            "quantity"  to quantity,
            "ticketCode" to ticketCode
        )

        db.collection("users").document(currentUser.uid)
            .collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                btnBuyTicket.isEnabled = true
                btnBuyTicket.text = "Xác nhận mua vé"

                val intent = Intent(this, TicketQrActivity::class.java).apply {
                    putExtra(TicketQrActivity.EXTRA_TICKET_CODE, ticketCode)
                    putExtra(TicketQrActivity.EXTRA_ROUTE_NAME,  "${r.lineCode} · ${r.name}")
                    putExtra(TicketQrActivity.EXTRA_QUANTITY,    quantity)
                    putExtra(TicketQrActivity.EXTRA_TOTAL_PRICE, total)
                    putExtra(TicketQrActivity.EXTRA_DATE,        dateStr)
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                btnBuyTicket.isEnabled = true
                btnBuyTicket.text = "Xác nhận mua vé"
                Toast.makeText(this, "Lỗi thanh toán: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun generateTicketCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10).map { chars.random() }.joinToString("")
    }

    private fun formatPrice(amount: Int): String = String.format("%,dđ", amount).replace(",", ".")
}
