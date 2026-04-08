package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        // Chưa login → kick về Login
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        loadUserData()
        setupBottomNav()
        setupClickListeners()
    }

    // ─────────────────────────────────────────────────────────
    // LOAD DATA TỪ FIRESTORE
    // ─────────────────────────────────────────────────────────
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {

                    // ── Tên user ──────────────────────────────
                    val name = doc.getString("name") ?: doc.getString("email") ?: "Bạn"
                    findViewById<TextView>(R.id.tvUserName).text = "$name 👋"

                    // ── Số dư ─────────────────────────────────
                    val balance = doc.getLong("balance") ?: 0L
                    val trips   = balance / 10000  // ước tính mỗi lượt 10k
                    findViewById<TextView>(R.id.tvBalance).text =
                        String.format("%,dđ", balance).replace(",", ".")
                    // ví dụ: 125.000đ

                    // ── Thống kê tháng ────────────────────────
                    val monthlySpend = doc.getLong("monthlySpend") ?: 0L
                    val monthlyTrips = doc.getLong("monthlyTrips") ?: 0L
                    val co2Saved     = doc.getDouble("co2Saved") ?: 0.0
                    val points       = doc.getLong("points") ?: 0L

                    findViewById<TextView>(R.id.tvMonthlySpend).text =
                        String.format("%,dđ", monthlySpend).replace(",", ".")
                    findViewById<TextView>(R.id.tvMonthlyTrips).text = "$monthlyTrips chuyến"
                    findViewById<TextView>(R.id.tvCO2Saved).text     = "$co2Saved Kg"
                    findViewById<TextView>(R.id.tvPoints).text       = "$points điểm"

                    // ── Số chuyến hôm nay ─────────────────────
                    val todayTrips = doc.getLong("todayTrips") ?: 0L
                    findViewById<TextView>(R.id.tvTripCount).text = "$todayTrips chuyến"
                }
            }
            .addOnFailureListener {
                // Firestore lỗi → giữ giá trị mặc định trong XML
            }

        // ── Thống kê tuần ─────────────────────────────────────
        db.collection("users").document(uid)
            .collection("weeklyStats")
            .document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val weekTrips = doc.getLong("trips") ?: 0L
                    val weekCost  = doc.getLong("cost") ?: 0L
                    val weekCO2   = doc.getDouble("co2") ?: 0.0

                    findViewById<TextView>(R.id.tvWeekTrips).text = "$weekTrips"
                    findViewById<TextView>(R.id.tvWeekCost).text  =
                        String.format("%,dđ", weekCost).replace(",", ".")
                    findViewById<TextView>(R.id.tvWeekCO2).text   = "$weekCO2 Kg"
                }
            }
    }

    // ─────────────────────────────────────────────────────────
    // BOTTOM NAV
    // ─────────────────────────────────────────────────────────
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home         -> true
                R.id.nav_favorite     -> {
                    startActivity(Intent(this, FavoriteActivity::class.java)); true
                }
                R.id.nav_notification -> {
                    startActivity(Intent(this, NotificationActivity::class.java)); true
                }
                R.id.nav_account      -> {
                    startActivity(Intent(this, AccountActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // CLICK LISTENERS
    // ─────────────────────────────────────────────────────────
    private fun setupClickListeners() {
        findViewById<View>(R.id.searchBar)?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.btnNotification)?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
        findViewById<View>(R.id.tvCardDetail)?.setOnClickListener {
            startActivity(Intent(this, CardDetailActivity::class.java))
        }
        findViewById<View>(R.id.btnTopUp)?.setOnClickListener {
            startActivity(Intent(this, TopUpActivity::class.java))
        }
        findViewById<View>(R.id.routeItem1)?.setOnClickListener { openRouteDetail(1) }
        findViewById<View>(R.id.routeItem2)?.setOnClickListener { openRouteDetail(2) }
        findViewById<View>(R.id.routeItem3)?.setOnClickListener { openRouteDetail(3) }
        findViewById<View>(R.id.tvSeeAllRoutes)?.setOnClickListener {
            startActivity(Intent(this, AllRoutesActivity::class.java))
        }
        findViewById<View>(R.id.tvSeeHistory)?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun openRouteDetail(routeId: Int) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra("ROUTE_ID", routeId)
        startActivity(intent)
    }
}