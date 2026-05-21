package com.example.citymove

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.example.citymove.data.model.RouteModel
import com.example.citymove.data.model.RouteStatus
import com.example.citymove.data.model.StopType
import com.example.citymove.data.model.TransportType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class RouteDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var currentRoute: RouteModel? = null
    private lateinit var routeDetailRoot: ConstraintLayout
    private lateinit var mapContainer: View
    private lateinit var topBar: View
    private lateinit var panelSplitGuide: Guideline
    private lateinit var routeInfoCard: CardView
    private lateinit var panelHandleTapArea: View
    private var dragStartY = 0f
    private var dragStartGuidePercent = 0f
    private var routeId: String? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        supportActionBar?.hide()

        routeId = intent.getStringExtra("ROUTE_ID_STRING")
        
        routeDetailRoot = findViewById(R.id.routeDetailRoot)
        mapContainer = findViewById(R.id.mapContainer)
        topBar = findViewById(R.id.topBar)
        panelSplitGuide = findViewById(R.id.panelSplitGuide)
        routeInfoCard = findViewById(R.id.routeInfoCard)
        panelHandleTapArea = findViewById(R.id.panelHandleTapArea)

        setupDraggablePanel()
        setupActions()

        if (routeId != null) {
            loadRouteData(routeId!!)
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin tuyến", Toast.LENGTH_SHORT).show()
            finish()
        }

        if (hasUsableMapsKey()) {
            setupMap()
        } else {
            showMapFallback()
        }
    }

    private fun loadRouteData(id: String) {
        db.collection("routes").document(id).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val type = TransportType.fromString(doc.getString("type") ?: "")
                    
                    // Parse path (List<GeoPoint> to List<LatLng>)
                    val geoPath = doc.get("path") as? List<GeoPoint>
                    val latLngPath = geoPath?.map { LatLng(it.latitude, it.longitude) } ?: emptyList()

                    // Parse stops
                    val stopsData = doc.get("stops") as? List<Map<String, Any>>
                    val routeStops = stopsData?.map { stopMap ->
                        val pos = stopMap["position"] as? GeoPoint
                        com.example.citymove.data.model.RouteStop(
                            name = stopMap["name"] as? String ?: "",
                            position = if (pos != null) LatLng(pos.latitude, pos.longitude) else LatLng(0.0, 0.0),
                            type = StopType.fromString(stopMap["type"] as? String ?: "")
                        )
                    } ?: emptyList()

                    currentRoute = RouteModel(
                        id = doc.id,
                        type = type,
                        lineCode = doc.getString("lineCode") ?: "",
                        name = doc.getString("name") ?: "",
                        startStation = doc.getString("startStation") ?: "",
                        endStation = doc.getString("endStation") ?: "",
                        distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                        stationCount = doc.getLong("stationCount")?.toInt() ?: 0,
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        status = RouteStatus.valueOf(doc.getString("status") ?: "ACTIVE"),
                        lineColor = doc.getString("lineColor") ?: "#2196F3",
                        price = doc.getLong("price")?.toInt(),
                        expectedOpenYear = doc.getLong("expectedOpenYear")?.toInt(),
                        summary = doc.getString("summary"),
                        path = latLngPath,
                        stops = routeStops
                    )
                    
                    bindRouteInfo()
                    bindStops()
                    if (::googleMap.isInitialized) {
                        updateMap()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
        
        currentRoute?.let { updateMap() }
    }

    private fun updateMap() {
        val route = currentRoute ?: return
        googleMap.clear()

        val boundsBuilder = LatLngBounds.Builder()
        
        route.path?.forEach { boundsBuilder.include(it) }
        route.stops?.forEach { boundsBuilder.include(it.position) }

        if (route.path.isNullOrEmpty() && route.stops.isNullOrEmpty()) return

        // Origin and Destination
        val origin = route.path?.firstOrNull() ?: route.stops?.firstOrNull()?.position
        val destination = route.path?.lastOrNull() ?: route.stops?.lastOrNull()?.position

        origin?.let {
            googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(route.name)
                    .snippet("Điểm khởi hành")
                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerHue(route.type)))
            )
        }

        destination?.let {
            googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(route.endStation)
                    .snippet("Điểm đến")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        route.path?.let {
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(it)
                    .color(Color.parseColor(route.lineColor))
                    .width(12f)
            )
        }

        route.stops?.forEach { stop ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(stop.position)
                    .title(stop.name)
                    .snippet(stop.type.label)
                    .icon(BitmapDescriptorFactory.defaultMarker(getStopMarkerHue(stop.type)))
            )
        }

        googleMap.setOnMapLoadedCallback {
            runCatching {
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(
                        boundsBuilder.build(),
                        80
                    )
                )
            }.onFailure {
                origin?.let { googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f)) }
            }
        }
    }

    private fun getMarkerHue(type: TransportType): Float {
        return when (type) {
            TransportType.BUS -> BitmapDescriptorFactory.HUE_ORANGE
            TransportType.METRO -> BitmapDescriptorFactory.HUE_AZURE
            TransportType.WATER_BUS -> BitmapDescriptorFactory.HUE_CYAN
        }
    }

    private fun getStopMarkerHue(type: StopType): Float {
        return when (type) {
            StopType.BUS -> BitmapDescriptorFactory.HUE_ORANGE
            StopType.METRO -> BitmapDescriptorFactory.HUE_AZURE
            StopType.WATER_BUS -> BitmapDescriptorFactory.HUE_CYAN
        }
    }

    private fun setupMap() {
        runCatching {
            val mapFragment = (supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment)
                ?: SupportMapFragment.newInstance().also { fragment ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mapContainer, fragment)
                        .commitNow()
                }

            mapFragment.getMapAsync(this)
        }.onFailure {
            showMapFallback()
        }
    }

    private fun hasUsableMapsKey(): Boolean {
        val key = getString(R.string.google_maps_key).trim()
        return key.isNotEmpty() && !key.equals("YOUR_GOOGLE_MAPS_API_KEY", ignoreCase = true)
    }

    private fun showMapFallback() {
        findViewById<TextView>(R.id.tvMapFallback).visibility = View.VISIBLE
    }

    private fun bindRouteInfo() {
        val route = currentRoute ?: return
        findViewById<TextView>(R.id.tvRouteMetaTop).text = route.name
        findViewById<TextView>(R.id.tvRouteCode).text = route.lineCode
        findViewById<TextView>(R.id.tvRouteTitle).text = route.name
        findViewById<TextView>(R.id.tvRouteSummary).text = route.summary ?: "Thông tin tuyến đường đang được cập nhật."
        findViewById<TextView>(R.id.tvRouteDuration).text = route.durationText
        findViewById<TextView>(R.id.tvRouteFare).text = route.priceOrDateText
        findViewById<TextView>(R.id.tvRouteBadge).text = "${route.type.displayName} ${route.lineCode}"
    }

    private fun bindStops() {
        val route = currentRoute ?: return
        val stopContainer = findViewById<LinearLayout>(R.id.stopListContainer)
        stopContainer.removeAllViews()

        route.stops?.forEachIndexed { index, stop ->
            val stopText = TextView(this).apply {
                text = "${index + 1}. ${stop.name} (${stop.type.label})"
                setTextColor(Color.parseColor("#334155"))
                textSize = 12f
                setPadding(0, if (index == 0) 0 else 8, 0, 0)
            }
            stopContainer.addView(stopText)
        }
    }

    private fun setupActions() {
        findViewById<TextView>(R.id.btnOpenMaps).setOnClickListener {
            openExternalMaps()
        }
        findViewById<TextView>(R.id.btnBookTicket).setOnClickListener {
            val route = currentRoute ?: return@setOnClickListener
            if (route.status == RouteStatus.ACTIVE) {
                val intent = Intent(this, BookTicketActivity::class.java)
                intent.putExtra("ROUTE_ID", route.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Tuyến đường chưa hoạt động", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupDraggablePanel() {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        panelHandleTapArea.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartY = event.rawY
                    dragStartGuidePercent = currentGuidePercent()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val rootHeight = routeDetailRoot.height
                    if (rootHeight <= 0) return@setOnTouchListener false

                    val deltaY = event.rawY - dragStartY
                    val draggedPercent = dragStartGuidePercent + (deltaY / rootHeight)
                    setGuidePercent(draggedPercent.coerceIn(minGuidePercent(), maxGuidePercent()))
                    updateHandleAccessibility()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val wasTap = kotlin.math.abs(event.rawY - dragStartY) < touchSlop
                    if (wasTap) {
                        togglePanelPreset()
                        view.performClick()
                    }
                    true
                }

                else -> false
            }
        }

        routeDetailRoot.post {
            setGuidePercent(currentGuidePercent().coerceIn(minGuidePercent(), maxGuidePercent()))
            updateHandleAccessibility()
        }
    }

    private fun togglePanelPreset() {
        val midpoint = (PANEL_EXPANDED_PERCENT + PANEL_COLLAPSED_PERCENT) / 2f
        val target = if (currentGuidePercent() < midpoint) {
            PANEL_COLLAPSED_PERCENT
        } else {
            PANEL_EXPANDED_PERCENT
        }
        setGuidePercent(target.coerceIn(minGuidePercent(), maxGuidePercent()))
        updateHandleAccessibility()
    }

    private fun updateHandleAccessibility() {
        val midpoint = (PANEL_EXPANDED_PERCENT + PANEL_COLLAPSED_PERCENT) / 2f
        panelHandleTapArea.contentDescription = getString(
            if (currentGuidePercent() < midpoint) R.string.route_panel_collapse else R.string.route_panel_expand
        )
    }

    private fun currentGuidePercent(): Float {
        val layoutParams = panelSplitGuide.layoutParams as ConstraintLayout.LayoutParams
        return layoutParams.guidePercent
    }

    private fun setGuidePercent(percent: Float) {
        val layoutParams = panelSplitGuide.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.guidePercent = percent
        panelSplitGuide.layoutParams = layoutParams
    }

    private fun minGuidePercent(): Float {
        val rootHeight = routeDetailRoot.height.coerceAtLeast(1)
        val topBarBottom = topBar.bottom.toFloat().coerceAtLeast(0f)
        val minMapHeight = resources.displayMetrics.density * 120f
        return ((topBarBottom + minMapHeight) / rootHeight).coerceIn(0.30f, 0.75f)
    }

    private fun maxGuidePercent(): Float {
        val rootHeight = routeDetailRoot.height.coerceAtLeast(1)
        val minPanelHeight = resources.displayMetrics.density * 220f
        return (1f - (minPanelHeight / rootHeight)).coerceIn(0.45f, 0.85f)
    }

    private fun openExternalMaps() {
        val route = currentRoute ?: return
        val destination = route.path?.lastOrNull() ?: route.stops?.lastOrNull()?.position ?: return
        
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}&travelmode=transit"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    companion object {
        private const val PANEL_EXPANDED_PERCENT = 0.52f
        private const val PANEL_COLLAPSED_PERCENT = 0.72f
    }
}
