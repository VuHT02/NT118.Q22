package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.TransportType
import com.example.citymove.adapter.RouteAdapter
import com.google.firebase.firestore.FirebaseFirestore

class RouteListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSPORT_TYPE = "transport_type"

        /** Gọi từ HomeActivity */
        fun start(from: android.content.Context, type: TransportType) {
            val intent = Intent(from, RouteListActivity::class.java)
            intent.putExtra(EXTRA_TRANSPORT_TYPE, type.name)
            from.startActivity(intent)
        }
    }

    // ─── Views ────────────────────────────────────────────────────────────────
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvTotalLines: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var progressBar: View
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var tabAll: TextView
    private lateinit var tabActive: TextView
    private lateinit var tabUpcoming: TextView
    private lateinit var btnBack: View

    // ─── State ────────────────────────────────────────────────────────────────
    private lateinit var transportType: TransportType
    private lateinit var adapter: RouteAdapter
    private lateinit var db: FirebaseFirestore
    private var allRoutes: List<RouteModel> = emptyList()
    private var currentFilter: RouteStatus? = null   // null = Tất cả

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_list)

        db = FirebaseFirestore.getInstance()

        // 1. Nhận type từ Intent (mặc định BUS nếu thiếu)
        val typeName = intent.getStringExtra(EXTRA_TRANSPORT_TYPE) ?: TransportType.BUS.name
        transportType = TransportType.fromString(typeName)

        bindViews()
        setupHeader()
        setupRecyclerView()
        setupSearch()
        setupTabs()
        setupBottomNav()

        // 2. Load data
        loadRoutes()
    }

    // ─── Bind Views ───────────────────────────────────────────────────────────
    private fun bindViews() {
        tvTitle        = findViewById(R.id.tvTitle)
        tvSubtitle     = findViewById(R.id.tvSubtitle)
        tvTotalLines   = findViewById(R.id.tvTotalLines)
        recyclerView   = findViewById(R.id.recyclerView)
        layoutEmpty    = findViewById(R.id.layoutEmpty)
        progressBar    = findViewById(R.id.progressBar)
        etSearch       = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        tabAll         = findViewById(R.id.tabAll)
        tabActive      = findViewById(R.id.tabActive)
        tabUpcoming    = findViewById(R.id.tabUpcoming)
        btnBack        = findViewById(R.id.btnBack)
    }

    // ─── Header: title/subtitle theo type ────────────────────────────────────
    private fun setupHeader() {
        tvTitle.text    = transportType.displayName
        tvSubtitle.text = transportType.subtitle
        btnBack.setOnClickListener { finish() }
        findViewById<View>(R.id.btnMapOverview).setOnClickListener {
            Toast.makeText(this, "Bản đồ ${transportType.displayName}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── RecyclerView + Adapter ───────────────────────────────────────────────
    private fun setupRecyclerView() {
        adapter = RouteAdapter(
            onCardClick = { route -> openRouteDetail(route) },
            onCtaClick  = { route -> handleCTA(route) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    // ─── Search ───────────────────────────────────────────────────────────────
    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                applyFilterAndSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
        }
    }

    // ─── Tab filter ───────────────────────────────────────────────────────────
    private fun setupTabs() {
        tabAll.setOnClickListener {
            currentFilter = null
            updateTabUI(tabAll)
            applyFilterAndSearch(etSearch.text.toString())
        }
        tabActive.setOnClickListener {
            currentFilter = RouteStatus.ACTIVE
            updateTabUI(tabActive)
            applyFilterAndSearch(etSearch.text.toString())
        }
        tabUpcoming.setOnClickListener {
            currentFilter = RouteStatus.UPCOMING
            updateTabUI(tabUpcoming)
            applyFilterAndSearch(etSearch.text.toString())
        }
    }

    private fun updateTabUI(selectedTab: TextView) {
        listOf(tabAll, tabActive, tabUpcoming).forEach { tab ->
            if (tab == selectedTab) {
                tab.setTextColor(android.graphics.Color.WHITE)
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
            } else {
                tab.setTextColor(android.graphics.Color.parseColor("#6B7280"))
                tab.setBackgroundResource(android.R.color.transparent)
            }
        }
    }

    // ─── Filter + Search logic ────────────────────────────────────────────────
    private fun applyFilterAndSearch(query: String) {
        var result = allRoutes

        // 1. Filter theo tab
        currentFilter?.let { status ->
            result = result.filter { it.status == status }
        }

        // 2. Filter theo search query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { route ->
                route.name.lowercase().contains(q) ||
                route.lineCode.lowercase().contains(q) ||
                route.startStation.lowercase().contains(q) ||
                route.endStation.lowercase().contains(q)
            }
        }

        adapter.submitList(result)
        layoutEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }

    // ─── Load data from Firestore ──────────────────────────────────────────
    private fun loadRoutes() {
        progressBar.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE

        db.collection("routes")
            .whereEqualTo("type", transportType.name)
            .get()
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                
                allRoutes = snapshot.documents.mapNotNull { doc ->
                    try {
                        RouteModel(
                            id = doc.id,
                            type = TransportType.fromString(doc.getString("type") ?: ""),
                            lineCode = doc.getString("lineCode") ?: "",
                            name = doc.getString("name") ?: "",
                            startStation = doc.getString("startStation") ?: "",
                            endStation = doc.getString("endStation") ?: "",
                            distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                            stationCount = doc.getLong("stationCount")?.toInt() ?: 0,
                            durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                            status = RouteStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
                            lineColor = doc.getString("lineColor") ?: "#CCCCCC",
                            price = doc.getLong("price")?.toInt(),
                            expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                tvTotalLines.text = allRoutes.size.toString()
                applyFilterAndSearch(etSearch.text.toString())
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    private fun openRouteDetail(route: RouteModel) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        // Dùng id từ Firestore (id kiểu String)
        intent.putExtra("ROUTE_ID_STRING", route.id)
        startActivity(intent)
    }

    private fun handleCTA(route: RouteModel) {
        when (route.status) {
            RouteStatus.ACTIVE  -> {
                val intent = Intent(this, BookTicketActivity::class.java)
                intent.putExtra("ROUTE_ID", route.id)
                startActivity(intent)
            }
            RouteStatus.UPCOMING -> {
                Toast.makeText(this, "Đã theo dõi: ${route.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─── Bottom Nav ───────────────────────────────────────────────────────────
    private fun setupBottomNav() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
            R.id.bottomNav
        )
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home   -> { finish(); true }
                else            -> false
            }
        }
    }
}
