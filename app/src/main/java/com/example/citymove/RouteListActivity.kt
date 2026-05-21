package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.RouteAdapter
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.TransportType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class RouteListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRANSPORT_TYPE = "transport_type"

        fun start(from: android.content.Context, type: TransportType = TransportType.BUS) {
            val intent = Intent(from, RouteListActivity::class.java)
            intent.putExtra(EXTRA_TRANSPORT_TYPE, type.name)
            from.startActivity(intent)
        }
    }

    private lateinit var tvTitle:        TextView
    private lateinit var tvSubtitle:     TextView
    private lateinit var tvTotalLines:   TextView
    private lateinit var recyclerView:   RecyclerView
    private lateinit var layoutEmpty:    View
    private lateinit var progressBar:    View
    private lateinit var etSearch:       EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var tabAll:         TextView
    private lateinit var tabActive:      TextView
    private lateinit var tabUpcoming:    TextView
    private lateinit var btnBack:        View

    private lateinit var btnTypeBus:      LinearLayout
    private lateinit var btnTypeMetro:    LinearLayout
    private lateinit var btnTypeWaterbus: LinearLayout

    private lateinit var transportType: TransportType
    private lateinit var adapter:       RouteAdapter
    private lateinit var db:            FirebaseFirestore
    private var allRoutes:     List<RouteModel> = emptyList()
    private var currentFilter: RouteStatus?     = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_list)

        db = FirebaseFirestore.getInstance()

        val typeName = intent.getStringExtra(EXTRA_TRANSPORT_TYPE) ?: TransportType.BUS.name
        transportType = TransportType.fromString(typeName)

        bindViews()
        setupHeader()
        setupTransportTypeButtons()
        setupRecyclerView()
        setupSearch()
        setupTabs()
        setupBottomNav()
        loadRoutes()
    }

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

        btnTypeBus      = findViewById(R.id.btnTypeBus)
        btnTypeMetro    = findViewById(R.id.btnTypeMetro)
        btnTypeWaterbus = findViewById(R.id.btnTypeWaterbus)
    }

    private fun setupHeader() {
        updateHeader()
        btnBack.setOnClickListener { finish() }
        findViewById<View>(R.id.btnMapOverview).setOnClickListener {
            Toast.makeText(this, "Bản đồ ${transportType.displayName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTransportTypeButtons() {
        btnTypeBus.setOnClickListener      { switchType(TransportType.BUS) }
        btnTypeMetro.setOnClickListener    { switchType(TransportType.METRO) }
        btnTypeWaterbus.setOnClickListener { switchType(TransportType.WATER_BUS) }
        applyTypeButtonUI()
    }

    private fun switchType(type: TransportType) {
        if (transportType == type) return
        transportType = type
        currentFilter = null
        updateTabUI(tabAll)
        updateHeader()
        applyTypeButtonUI()
        etSearch.setText("")
        loadRoutes()
    }

    private fun updateHeader() {
        tvTitle.text    = transportType.displayName
        tvSubtitle.text = transportType.subtitle
    }

    private fun applyTypeButtonUI() {
        data class Cfg(
            val btn:       LinearLayout,
            val icon:      ImageView,
            val label:     TextView,
            val type:      TransportType,
            val activeColor: Int
        )

        val orange = android.graphics.Color.parseColor("#F97316")
        val blue   = android.graphics.Color.parseColor("#2563EB")
        val green  = android.graphics.Color.parseColor("#10B981")
        val gray   = android.graphics.Color.parseColor("#6B7280")

        val cfgs = listOf(
            Cfg(btnTypeBus,      findViewById(R.id.iconBus),      findViewById(R.id.labelBus),      TransportType.BUS,       orange),
            Cfg(btnTypeMetro,    findViewById(R.id.iconMetro),    findViewById(R.id.labelMetro),    TransportType.METRO,     blue),
            Cfg(btnTypeWaterbus, findViewById(R.id.iconWaterbus), findViewById(R.id.labelWaterbus), TransportType.WATER_BUS, green)
        )

        cfgs.forEach { cfg ->
            val selected = cfg.type == transportType
            cfg.btn.setBackgroundResource(
                if (selected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
            )
            val tintColor = if (selected) cfg.activeColor else gray
            cfg.icon.imageTintList  = android.content.res.ColorStateList.valueOf(tintColor)
            cfg.label.setTextColor(tintColor)
        }
    }

    private fun setupRecyclerView() {
        adapter = RouteAdapter(
            onCardClick = { route -> openRouteDetail(route) },
            onCtaClick  = { route -> handleCTA(route) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

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

    private fun applyFilterAndSearch(query: String) {
        var result = allRoutes
        currentFilter?.let { status -> result = result.filter { it.status == status } }
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

    private fun loadRoutes() {
        progressBar.visibility = View.VISIBLE
        layoutEmpty.visibility = View.GONE
        allRoutes = emptyList()
        adapter.submitList(emptyList())
        tvTotalLines.text = "0"

        db.collection("routes")
            .whereEqualTo("type", transportType.name)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->
                progressBar.visibility = View.GONE
                allRoutes = snapshot.documents.mapNotNull { doc ->
                    try {
                        RouteModel(
                            id              = doc.id,
                            type            = TransportType.fromString(doc.getString("type") ?: "BUS"),
                            lineCode        = doc.getString("lineCode") ?: "",
                            name            = doc.getString("name") ?: "",
                            startStation    = doc.getString("startStation") ?: "",
                            endStation      = doc.getString("endStation") ?: "",
                            distanceKm      = doc.getDouble("distanceKm")
                                              ?: doc.getLong("distanceKm")?.toDouble()
                                              ?: 0.0,
                            stationCount    = doc.getLong("stationCount")?.toInt()
                                              ?: doc.getDouble("stationCount")?.toInt()
                                              ?: 0,
                            durationMinutes = doc.getLong("durationMinutes")?.toInt()
                                              ?: doc.getDouble("durationMinutes")?.toInt()
                                              ?: 0,
                            status          = try {
                                RouteStatus.valueOf(
                                    doc.getString("status")?.uppercase() ?: "ACTIVE"
                                )
                            } catch (e: Exception) { RouteStatus.ACTIVE },
                            lineColor       = doc.getString("lineColor") ?: "#F97316",
                            price           = doc.getLong("price")?.toInt()
                                              ?: doc.getDouble("price")?.toInt(),
                            expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                tvTotalLines.text = allRoutes.size.toString()
                applyFilterAndSearch(etSearch.text.toString())

                if (allRoutes.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvEmpty).text = "Chưa có tuyến ${transportType.displayName}"
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                layoutEmpty.visibility = View.VISIBLE
                Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun openRouteDetail(route: RouteModel) {
        val intent = Intent(this, RouteDetailActivity::class.java)
        intent.putExtra("ROUTE_ID_STRING", route.id)
        startActivity(intent)
    }

    private fun handleCTA(route: RouteModel) {
        when (route.status) {
            RouteStatus.ACTIVE -> {
                val intent = Intent(this, BookTicketActivity::class.java)
                intent.putExtra("ROUTE_ID", route.id)
                startActivity(intent)
            }
            RouteStatus.UPCOMING ->
                Toast.makeText(this, "Đã theo dõi: ${route.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { finish(); true }
                else          -> false
            }
        }
    }
}
