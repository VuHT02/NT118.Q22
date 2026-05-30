package com.example.citymove

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.citymove.adapter.RouteAdapter
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.TransportType
import com.example.citymove.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityHomeBinding
    private lateinit var routeAdapter: RouteAdapter
    private var userListener: ListenerRegistration? = null
    private var prefsLoaded = false

    private var selectedTransport = TRANSPORT_BUS

    companion object {
        const val TRANSPORT_BUS        = "bus"
        const val TRANSPORT_METRO      = "metro"
        const val TRANSPORT_WATERBUS   = "waterbus"
        const val FIELD_PREF_TRANSPORT = "preferredTransport"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupTransportSelector()
        setupFilterChips()
        setupPopularRoutes()
        setupHeaderButtons()
        setupSearchCard()
        binding.tvTodayDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        binding.btnSeeAllRoutes.setOnClickListener {
            RouteListActivity.start(this)
        }

        // TODO: XÓA SAU KHI SEED XONG — nhấn giữ avatar để seed data
        binding.btnProfile.setOnLongClickListener {
            DataSeeder.seedRoutes(this)
            DataSeeder.seedStops(this)
            true
        }

        setupBottomNav()
    }

    override fun onStart() {
        super.onStart()
        startUserListener()
    }

    override fun onStop() {
        super.onStop()
        userListener?.remove()
        userListener = null
        prefsLoaded = false
    }

    private fun setupHeaderButtons() {
        binding.btnNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, AccountActivity::class.java))
        }
        binding.fabAI.setOnClickListener {
            startActivity(Intent(this, SearchMapActivity::class.java))
        }
        binding.btnTopUp.setOnClickListener {
            startActivity(Intent(this, CardDetailActivity::class.java))
        }
    }

    private fun setupSearchCard() {
        binding.btnRoutePlanner.setOnClickListener {
            RouteListActivity.start(this)
        }
        binding.btnNearbyStops.setOnClickListener {
            startActivity(Intent(this, SearchMapActivity::class.java))
        }
        binding.btnSearchNow.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchRoute.windowToken, 0)
            startActivity(Intent(this, SearchMapActivity::class.java))
        }
        binding.etSearchRoute.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                startActivity(Intent(this, SearchMapActivity::class.java))
                true
            } else false
        }
    }

    private fun setupTransportSelector() {
        binding.btnTransportBus.setOnClickListener {
            applyTransportSelection(TRANSPORT_BUS)
            saveTransportPreference(TRANSPORT_BUS)
            loadPopularRoutesFromFirestore(TransportType.BUS)
        }
        binding.btnTransportMetro.setOnClickListener {
            applyTransportSelection(TRANSPORT_METRO)
            saveTransportPreference(TRANSPORT_METRO)
            loadPopularRoutesFromFirestore(TransportType.METRO)
        }
        binding.btnTransportWaterbus.setOnClickListener {
            applyTransportSelection(TRANSPORT_WATERBUS)
            saveTransportPreference(TRANSPORT_WATERBUS)
            loadPopularRoutesFromFirestore(TransportType.WATER_BUS)
        }
        applyTransportSelection(TRANSPORT_BUS)
    }

    private fun applyTransportSelection(type: String) {
        selectedTransport = type

        val orange = ContextCompat.getColor(this, R.color.orange_primary)
        val blue   = ContextCompat.getColor(this, R.color.blue_primary)
        val green  = android.graphics.Color.parseColor("#10B981")
        val gray   = ContextCompat.getColor(this, R.color.text_secondary)

        data class TabCfg(val btn: LinearLayout, val key: String, val color: Int, val iconBg: Int)
        val tabs = listOf(
            TabCfg(binding.btnTransportBus,      TRANSPORT_BUS,      orange, android.graphics.Color.parseColor("#FFF3E0")),
            TabCfg(binding.btnTransportMetro,    TRANSPORT_METRO,    blue,   android.graphics.Color.parseColor("#EFF6FF")),
            TabCfg(binding.btnTransportWaterbus, TRANSPORT_WATERBUS, green,  android.graphics.Color.parseColor("#ECFDF5"))
        )

        tabs.forEach { tab ->
            val sel = tab.key == type
            tab.btn.setBackgroundResource(if (sel) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)

            val iconFrame = tab.btn.getChildAt(0) as? FrameLayout
            val labelTv   = tab.btn.getChildAt(1) as? TextView
            val priceTv   = tab.btn.getChildAt(2) as? TextView

            iconFrame?.backgroundTintList = ColorStateList.valueOf(
                if (sel) tab.iconBg else ContextCompat.getColor(this, R.color.bg_icon_gray)
            )
            (iconFrame?.getChildAt(0) as? android.widget.ImageView)?.imageTintList =
                ColorStateList.valueOf(if (sel) tab.color else gray)

            labelTv?.setTextColor(if (sel) tab.color else gray)
            labelTv?.setTypeface(null, if (sel) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            priceTv?.setTextColor(if (sel) tab.color else ContextCompat.getColor(this, R.color.text_hint))
        }
    }

    private fun setupFilterChips() {
        binding.chipFastest.setOnClickListener { /* TODO sort fastest */ }
        binding.chipCheapest.setOnClickListener { /* TODO sort cheapest */ }
        binding.chipEcoFriendly.setOnClickListener { /* TODO filter eco */ }
    }

    private fun setupPopularRoutes() {
        routeAdapter = RouteAdapter(
            onCardClick = { route ->
                val intent = Intent(this, RouteDetailActivity::class.java)
                intent.putExtra("ROUTE_ID", route.id)
                startActivity(intent)
            },
            onCtaClick = {
                route ->
                val intent = Intent(this, BookTicketActivity::class.java)
                intent.putExtra("ROUTE_ID", route.id)
                startActivity(intent)
            }
        )
        binding.recyclerPopularRoutes.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = routeAdapter
            isNestedScrollingEnabled = false
        }
        loadPopularRoutesFromFirestore(TransportType.BUS)
    }

    private fun loadPopularRoutesFromFirestore(type: TransportType = TransportType.BUS) {
        val typeFilters = listOf(type.name, type.name.lowercase(), type.name.lowercase().replace("_", ""))

        db.collection("routes")
            .whereIn("type", typeFilters)
            .limit(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val routes = snapshot.documents.mapNotNull { doc ->
                    try {
                        val lineCode = doc.getString("lineCode") ?: doc.getString("code") ?: ""
                        val priceStr = doc.get("fare")?.toString()?.replace(Regex("[^0-9]"), "")
                        val price = doc.getLong("price")?.toInt() ?: priceStr?.toIntOrNull()
                        val durationMinutes = (doc.getLong("durationMinutes") 
                                              ?: doc.getString("duration")?.replace(Regex("[^0-9]"), "")?.toLongOrNull() 
                                              ?: 0).toInt()

                        RouteModel(
                            id               = doc.id,
                            type             = type,
                            lineCode         = lineCode,
                            name             = doc.getString("name") ?: "",
                            startStation     = doc.getString("startStation") ?: doc.getString("from") ?: "",
                            endStation       = doc.getString("endStation") ?: doc.getString("to") ?: "",
                            distanceKm       = doc.getDouble("distanceKm") ?: 0.0,
                            stationCount     = (doc.getLong("stationCount") ?: 0).toInt(),
                            durationMinutes  = durationMinutes,
                            status           = if (doc.getString("status")?.uppercase() == "UPCOMING") RouteStatus.UPCOMING else RouteStatus.ACTIVE,
                            lineColor        = doc.getString("lineColor") ?: "#F97316",
                            price            = price,
                            expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt()
                        )
                    } catch (e: Exception) { null }
                }.distinctBy { it.lineCode } // Fix lặp data ở Trang chủ

                routeAdapter.submitList(routes.ifEmpty { fallbackRoutes(type) })
            }
            .addOnFailureListener { routeAdapter.submitList(fallbackRoutes(type)) }
    }

    private fun fallbackRoutes(type: TransportType = TransportType.BUS) = when (type) {
        TransportType.BUS -> listOf(
            RouteModel("1", TransportType.BUS, "01", "Bến Thành - Chợ Lớn",
                "Hàm Nghi", "Ga Chợ Lớn", 8.5, 12, 35, RouteStatus.ACTIVE, "#F97316", 7000, null),
            RouteModel("2", TransportType.BUS, "150", "Chợ Lớn - Ngã 3 Tân Vạn",
                "Ga Chợ Lớn", "Tân Vạn", 25.0, 45, 80, RouteStatus.ACTIVE, "#2563EB", 7000, null)
        )
        TransportType.METRO -> listOf(
            RouteModel("m1", TransportType.METRO, "M1", "Bến Thành - Suối Tiên",
                "Bến Thành", "Suối Tiên", 19.7, 14, 34, RouteStatus.ACTIVE, "#2563EB", 6000, null),
            RouteModel("m2", TransportType.METRO, "M2", "Bến Thành - Tham Lương",
                "Bến Thành", "Tham Lương", 11.3, 9, 20, RouteStatus.UPCOMING, "#7C3AED", null, 2028)
        )
        TransportType.WATER_BUS -> listOf(
            RouteModel("w1", TransportType.WATER_BUS, "WB01", "Bạch Đằng - Linh Đông",
                "Bến Bạch Đằng", "Linh Đông", 10.8, 6, 30, RouteStatus.ACTIVE, "#10B981", 15000, null),
            RouteModel("w2", TransportType.WATER_BUS, "WB02", "Bạch Đằng - Lò Gốm",
                "Bến Bạch Đằng", "Linh Đông", 9.6, 6, 35, RouteStatus.ACTIVE, "#0EA5E9", 15000, null)
        )
    }

    private fun startUserListener() {
        val uid = auth.currentUser?.uid ?: return
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name")?.takeIf { it.isNotEmpty() }
                        ?: doc.getString("email")?.substringBefore("@")?.takeIf { it.isNotEmpty() }
                        ?: auth.currentUser?.displayName?.takeIf { it.isNotEmpty() }
                        ?: "Người dùng"
                    binding.tvUserName.text = name

                    val balance = doc.getLong("balance") ?: 0L
                    val monthlySpend = doc.getLong("monthlySpend") ?: 0L
                    binding.tvBalance.text = formatAmount(balance)
                    binding.tvMonthlySpend.text = formatAmount(monthlySpend)

                    if (!prefsLoaded) {
                        val pref = doc.getString(FIELD_PREF_TRANSPORT) ?: TRANSPORT_BUS
                        applyTransportSelection(pref)
                        loadPopularRoutesFromFirestore(when (pref) {
                            TRANSPORT_METRO    -> TransportType.METRO
                            TRANSPORT_WATERBUS -> TransportType.WATER_BUS
                            else               -> TransportType.BUS
                        })
                        prefsLoaded = true
                    }
                } else {
                    binding.tvUserName.text =
                        auth.currentUser?.displayName?.takeIf { it.isNotEmpty() } ?: "Người dùng"
                    binding.tvBalance.text = formatAmount(0L)
                    binding.tvMonthlySpend.text = formatAmount(0L)
                }
            }
    }

    private fun saveTransportPreference(type: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update(FIELD_PREF_TRANSPORT, type)
    }

    private fun formatAmount(amount: Long): String =
        String.format("%,d₫", amount).replace(",", ".")

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_notification -> { startActivity(Intent(this, NotificationActivity::class.java)); true }
                R.id.nav_account      -> { startActivity(Intent(this, AccountActivity::class.java)); true }
                else -> false
            }
        }
    }
}
