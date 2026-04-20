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
                    val name = doc.getString("name") ?: doc.getString("email") ?: "Bạn"
                    findViewById<TextView>(R.id.tvUserName).text = "$name 👋"

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
                }
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

        // ── Quick chips ───────────────────────────────────────
        findViewById<View>(R.id.chipDestinationBenThanh)?.setOnClickListener {
            destinationInput?.setText("Bến Thành")
            openSearchWithDestination("Bến Thành")
        }
        findViewById<View>(R.id.chipDestinationSuoiTien)?.setOnClickListener {
            destinationInput?.setText("Suối Tiên")
            openSearchWithDestination("Suối Tiên")
        }
        findViewById<View>(R.id.chipDestinationTanSonNhat)?.setOnClickListener {
            destinationInput?.setText("Sân bay Tân Sơn Nhất")
            openSearchWithDestination("Sân bay Tân Sơn Nhất")
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

        // ── Transport mode selector (icon cards) ──────────────
        setupTransportSelector()

        // ── Route tabs (Buýt / Metro / Waterbus) ─────────────
        setupRouteTabs()
    }

    // ─────────────────────────────────────────────────────────
    // TRANSPORT SELECTOR — 3 icon cards dưới search bar
    // ─────────────────────────────────────────────────────────
    private fun setupTransportSelector() {
        val btnBus      = findViewById<LinearLayout>(R.id.btnTransportBus)
        val btnMetro    = findViewById<LinearLayout>(R.id.btnTransportMetro)
        val btnWaterbus = findViewById<LinearLayout>(R.id.btnTransportWaterbus)

        val allTransport = listOf(btnBus, btnMetro, btnWaterbus)

        fun applyTransportSelection(selected: LinearLayout, type: String) {
            selectedTransport = type
            val activeColor   = ContextCompat.getColor(this, R.color.blue_primary)
            val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

            allTransport.forEach { btn ->
                val isSelected = btn == selected
                btn?.setBackgroundResource(
                    if (isSelected) R.drawable.bg_transport_selected
                    else R.drawable.bg_transport_unselected
                )
                // FrameLayout (child 0) chứa icon → đổi background tint
                // TextView (child 1) → đổi text color
                btn?.let { ll ->
                    val iconFrame = ll.getChildAt(0)
                    val label    = ll.getChildAt(1) as? TextView
                    label?.setTextColor(if (isSelected) activeColor else inactiveColor)
                    label?.setTypeface(null,
                        if (isSelected) android.graphics.Typeface.BOLD
                        else android.graphics.Typeface.NORMAL
                    )
                    iconFrame?.backgroundTintList = ColorStateList.valueOf(
                        if (isSelected)
                            ContextCompat.getColor(this, R.color.blue_primary)
                        else
                            ContextCompat.getColor(this, R.color.bg_icon_gray)
                    )
                }
            }

        }

        btnBus?.setOnClickListener {
            applyTransportSelection(btnBus, "bus")
            startActivity(Intent(this, BusRoutesActivity::class.java))
        }
        btnMetro?.setOnClickListener {
            applyTransportSelection(btnMetro, "metro")
            startActivity(Intent(this, MetroRoutesActivity::class.java))
        }
        btnWaterbus?.setOnClickListener {
            applyTransportSelection(btnWaterbus, "waterbus")
            startActivity(Intent(this, WaterbusRoutesActivity::class.java))
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
                tv?.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                // Update indicator logic if needed, usually via a custom view or bottom background
            }
            allPanels.forEachIndexed { i, layout ->
                layout?.visibility = if (i == index) View.VISIBLE else View.GONE
            }
        }

        tabBus?.setOnClickListener      { activateTab(0) }
        tabMetro?.setOnClickListener    { activateTab(1) }
        tabWaterbus?.setOnClickListener { activateTab(2) }

        // Default
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
