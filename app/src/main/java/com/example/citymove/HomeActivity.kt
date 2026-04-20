package com.example.citymove

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Transport selector state
    private var selectedTransport = "bus"

    // Transport buttons — khai báo ở class level để dùng được trong applyTransportSelection
    private lateinit var btnBus: LinearLayout
    private lateinit var btnMetro: LinearLayout
    private lateinit var btnWaterbus: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupTransportSelector() // phải gọi trước loadUserData để btnBus/Metro/Waterbus đã được init
        loadUserData()
        setupBottomNav()
        setupClickListeners()
    }

    // ─────────────────────────────────────────────────────────
    // LOAD DATA TỪ FIRESTORE
    // ─────────────────────────────────────────────────────────
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        // Set ngày ngay lập tức, không cần chờ Firestore
        val sdf = java.text.SimpleDateFormat("dd/MM", java.util.Locale("vi"))
        findViewById<TextView>(R.id.tvTodayDate).text = sdf.format(java.util.Date())

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    // ── Tên user ──────────────────────────────────────
                    val name = doc.getString("name") ?: doc.getString("email") ?: "Bạn"
                    findViewById<TextView>(R.id.tvUserName).text = "$name 👋"

                    // ── Số dư & thống kê ──────────────────────────────
                    val balance = doc.getLong("balance") ?: 0L
                    findViewById<TextView>(R.id.tvBalance).text =
                        String.format("%,dđ", balance).replace(",", ".")

                    val monthlySpend = doc.getLong("monthlySpend") ?: 0L
                    val monthlyTrips = doc.getLong("monthlyTrips") ?: 0L
                    val co2Saved     = doc.getDouble("co2Saved") ?: 0.0
                    val points       = doc.getLong("points") ?: 0L

                    findViewById<TextView>(R.id.tvMonthlySpend).text =
                        String.format("%,dđ", monthlySpend).replace(",", ".")
                    findViewById<TextView>(R.id.tvMonthlyTrips).text = "$monthlyTrips chuyến"
                    findViewById<TextView>(R.id.tvCO2Saved).text     = "$co2Saved Kg"
                    findViewById<TextView>(R.id.tvPoints).text       = "$points điểm"

                    val todayTrips = doc.getLong("todayTrips") ?: 0L
                    findViewById<TextView>(R.id.tvTripCount).text = "$todayTrips chuyến"

                    // ── Load transport preference từ Firestore ─────────
                    val preferred = doc.getString("preferredTransport") ?: "bus"
                    applyTransportFromData(preferred)

                    // ── Load quick chips từ Firestore ──────────────────
                    @Suppress("UNCHECKED_CAST")
                    val destinations = doc.get("quickDestinations") as? List<String>
                    if (!destinations.isNullOrEmpty()) {
                        updateQuickChips(destinations)
                    }
                }
            }
            .addOnFailureListener {
                // Firestore lỗi → vẫn hiện tên từ FirebaseAuth nếu có
                val displayName = auth.currentUser?.displayName
                    ?: auth.currentUser?.email
                    ?: "Bạn"
                findViewById<TextView>(R.id.tvUserName).text = "$displayName 👋"
            }

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
        val destinationInput = findViewById<EditText>(R.id.etDestinationInput)

        // ── Search ────────────────────────────────────────────
        findViewById<View>(R.id.searchBar)?.setOnClickListener {
            openSearchWithDestination(destinationInput?.text?.toString())
        }
        findViewById<View>(R.id.btnGoNow)?.setOnClickListener {
            val destination = destinationInput?.text?.toString().orEmpty().trim()
            if (destination.isEmpty()) {
                Toast.makeText(this, getString(R.string.search_map_empty_destination), Toast.LENGTH_SHORT).show()
            } else {
                openSearchWithDestination(destination)
            }
        }

        destinationInput?.setOnEditorActionListener { _, actionId, event ->
            val isKeyboardSubmit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)

            if (isKeyboardSubmit) {
                val destination = destinationInput.text?.toString().orEmpty().trim()
                if (destination.isNotEmpty()) openSearchWithDestination(destination)
                true
            } else false
        }

        // ── Quick chips — listener mặc định (sẽ bị override nếu Firestore có data) ──
        findViewById<View>(R.id.chipDestinationBenThanh)?.setOnClickListener {
            openSearchWithDestination(
                (it as? TextView)?.text?.toString() ?: "Bến Thành"
            )
        }
        findViewById<View>(R.id.chipDestinationSuoiTien)?.setOnClickListener {
            openSearchWithDestination(
                (it as? TextView)?.text?.toString() ?: "Suối Tiên"
            )
        }
        findViewById<View>(R.id.chipDestinationTanSonNhat)?.setOnClickListener {
            openSearchWithDestination(
                (it as? TextView)?.text?.toString() ?: "Sân bay Tân Sơn Nhất"
            )
        }

        // ── Header buttons ────────────────────────────────────
        findViewById<View>(R.id.btnRoutePlanner)?.setOnClickListener {
            startActivity(Intent(this, AllRoutesActivity::class.java))
        }
        findViewById<View>(R.id.btnLocation)?.setOnClickListener { openRouteDetail(1) }
        findViewById<View>(R.id.btnNotification)?.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }

        // ── TransPass card ────────────────────────────────────
        findViewById<View>(R.id.tvCardDetail)?.setOnClickListener {
            startActivity(Intent(this, CardDetailActivity::class.java))
        }
        findViewById<View>(R.id.btnTopUp)?.setOnClickListener {
            startActivity(Intent(this, TopUpActivity::class.java))
        }

        // ── Route items ───────────────────────────────────────
        findViewById<View>(R.id.busRouteItem1)?.setOnClickListener { openRouteDetail(1) }
        findViewById<View>(R.id.busRouteItem2)?.setOnClickListener { openRouteDetail(2) }
        findViewById<View>(R.id.busRouteItem3)?.setOnClickListener { openRouteDetail(3) }
        findViewById<View>(R.id.tvSeeAllRoutes)?.setOnClickListener {
            startActivity(Intent(this, AllRoutesActivity::class.java))
        }
        findViewById<View>(R.id.tvSeeHistory)?.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // ── Route tabs (Buýt / Metro / Waterbus) ─────────────
        setupRouteTabs()
    }

    // ─────────────────────────────────────────────────────────
    // TRANSPORT SELECTOR — 3 icon cards dưới search bar
    // ─────────────────────────────────────────────────────────
    private fun setupTransportSelector() {
        btnBus      = findViewById(R.id.btnTransportBus)
        btnMetro    = findViewById(R.id.btnTransportMetro)
        btnWaterbus = findViewById(R.id.btnTransportWaterbus)

        // Mặc định chưa chọn gì — sẽ được set sau khi Firestore trả về
        btnBus.setOnClickListener {
            applyTransportSelection(btnBus, "bus")
            saveTransportPreference("bus")
            startActivity(Intent(this, BusRoutesActivity::class.java))
        }
        btnMetro.setOnClickListener {
            applyTransportSelection(btnMetro, "metro")
            saveTransportPreference("metro")
            startActivity(Intent(this, MetroRoutesActivity::class.java))
        }
        btnWaterbus.setOnClickListener {
            applyTransportSelection(btnWaterbus, "waterbus")
            saveTransportPreference("waterbus")
            startActivity(Intent(this, WaterbusRoutesActivity::class.java))
        }
    }

    // Gọi từ loadUserData() sau khi lấy được preference từ Firestore
    private fun applyTransportFromData(type: String) {
        val selected = when (type) {
            "metro"    -> btnMetro
            "waterbus" -> btnWaterbus
            else       -> btnBus
        }
        applyTransportSelection(selected, type)
    }

    private fun applyTransportSelection(selected: LinearLayout, type: String) {
        selectedTransport = type
        val activeColor   = ContextCompat.getColor(this, R.color.blue_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        listOf(btnBus, btnMetro, btnWaterbus).forEach { btn ->
            val isSelected = btn == selected
            btn.setBackgroundResource(
                if (isSelected) R.drawable.bg_transport_selected
                else R.drawable.bg_transport_unselected
            )
            val iconFrame = btn.getChildAt(0)
            val label     = btn.getChildAt(1) as? TextView
            label?.setTextColor(if (isSelected) activeColor else inactiveColor)
            label?.setTypeface(null,
                if (isSelected) android.graphics.Typeface.BOLD
                else android.graphics.Typeface.NORMAL
            )
            iconFrame?.backgroundTintList = ColorStateList.valueOf(
                if (isSelected) ContextCompat.getColor(this, R.color.blue_primary)
                else ContextCompat.getColor(this, R.color.bg_icon_gray)
            )
        }
    }

    // Lưu preference khi user click vào transport
    private fun saveTransportPreference(type: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .update("preferredTransport", type)
    }

    // ─────────────────────────────────────────────────────────
    // QUICK CHIPS — load động từ Firestore
    // Firestore field: quickDestinations: ["Bến Thành", "Suối Tiên", "Sân bay Tân Sơn Nhất"]
    // ─────────────────────────────────────────────────────────
    private fun updateQuickChips(destinations: List<String>) {
        val chips = listOf(
            findViewById<TextView>(R.id.chipDestinationBenThanh),
            findViewById<TextView>(R.id.chipDestinationSuoiTien),
            findViewById<TextView>(R.id.chipDestinationTanSonNhat)
        )

        destinations.forEachIndexed { index, dest ->
            if (index < chips.size) {
                chips[index]?.text = dest
                chips[index]?.setOnClickListener {
                    openSearchWithDestination(dest)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // ROUTE TABS — Buýt / Metro / Waterbus tab switcher
    // ─────────────────────────────────────────────────────────
    private fun setupRouteTabs() {
        val tabBus      = findViewById<TextView>(R.id.tabBus)
        val tabMetro    = findViewById<TextView>(R.id.tabMetro)
        val tabWaterbus = findViewById<TextView>(R.id.tabWaterbus)

        val panelBus      = findViewById<LinearLayout>(R.id.panelBus)
        val panelMetro    = findViewById<LinearLayout>(R.id.panelMetro)
        val panelWaterbus = findViewById<LinearLayout>(R.id.panelWaterbus)

        val allTabs   = listOf(tabBus, tabMetro, tabWaterbus)
        val allPanels = listOf(panelBus, panelMetro, panelWaterbus)

        fun activateTab(index: Int) {
            allTabs.forEachIndexed { i, tv ->
                val isSelected = (i == index)
                tv?.setTextColor(
                    if (isSelected) ContextCompat.getColor(this, R.color.blue_primary)
                    else ContextCompat.getColor(this, R.color.text_secondary)
                )
                tv?.setTypeface(null,
                    if (isSelected) android.graphics.Typeface.BOLD
                    else android.graphics.Typeface.NORMAL
                )
            }
            allPanels.forEachIndexed { i, layout ->
                layout?.visibility = if (i == index) View.VISIBLE else View.GONE
            }
        }

        tabBus?.setOnClickListener      { activateTab(0) }
        tabMetro?.setOnClickListener    { activateTab(1) }
        tabWaterbus?.setOnClickListener { activateTab(2) }

        activateTab(0)
    }

    private fun openSearchWithDestination(destination: String?) {
        val intent = Intent(this, SearchActivity::class.java)
        if (!destination.isNullOrEmpty()) {
            intent.putExtra("DESTINATION_NAME", destination)
        }
        startActivity(intent)
    }

    private fun openRouteDetail(routeId: Int) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra("ROUTE_ID", routeId)
        startActivity(intent)
    }
}