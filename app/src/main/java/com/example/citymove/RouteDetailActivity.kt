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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class RouteDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var currentRoute: RouteInfo
    private lateinit var routeDetailRoot: ConstraintLayout
    private lateinit var mapContainer: View
    private lateinit var topBar: View
    private lateinit var panelSplitGuide: Guideline
    private lateinit var routeInfoCard: CardView
    private lateinit var panelHandleTapArea: View
    private var dragStartY = 0f
    private var dragStartGuidePercent = 0f
    private var routeId: Int = DEFAULT_ROUTE_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        supportActionBar?.hide()

        routeId = intent.getIntExtra(EXTRA_ROUTE_ID, DEFAULT_ROUTE_ID)
        currentRoute = routeForId(routeId)
        
        routeDetailRoot = findViewById(R.id.routeDetailRoot)
        mapContainer = findViewById(R.id.mapContainer)
        topBar = findViewById(R.id.topBar)
        panelSplitGuide = findViewById(R.id.panelSplitGuide)
        routeInfoCard = findViewById(R.id.routeInfoCard)
        panelHandleTapArea = findViewById(R.id.panelHandleTapArea)
        bindRouteInfo()
        bindStops()
        setupDraggablePanel()
        setupActions()

        if (hasUsableMapsKey()) {
            setupMap()
        } else {
            showMapFallback()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false

        val boundsBuilder = LatLngBounds.Builder()
        currentRoute.path.forEach { boundsBuilder.include(it) }
        currentRoute.stops.forEach { boundsBuilder.include(it.position) }

        googleMap.addMarker(
            MarkerOptions()
                .position(currentRoute.origin)
                .title(currentRoute.title)
                .snippet("Điểm khởi hành")
                .icon(BitmapDescriptorFactory.defaultMarker(currentRoute.markerHue))
        )
        googleMap.addMarker(
            MarkerOptions()
                .position(currentRoute.destination)
                .title(currentRoute.destinationLabel)
                .snippet("Điểm đến")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        googleMap.addPolyline(
            PolylineOptions()
                .addAll(currentRoute.path)
                .color(currentRoute.routeColor)
                .width(12f)
        )

        currentRoute.stops.forEach { stop ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(stop.position)
                    .title(stop.name)
                    .snippet(stop.type.label)
                    .icon(BitmapDescriptorFactory.defaultMarker(stop.type.markerHue))
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
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentRoute.origin, 12f))
            }
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
        findViewById<TextView>(R.id.tvRouteMetaTop).text = currentRoute.title
        findViewById<TextView>(R.id.tvRouteCode).text = currentRoute.code
        findViewById<TextView>(R.id.tvRouteTitle).text = currentRoute.title
        findViewById<TextView>(R.id.tvRouteSummary).text = currentRoute.summary
        findViewById<TextView>(R.id.tvRouteDuration).text = currentRoute.duration
        findViewById<TextView>(R.id.tvRouteFare).text = currentRoute.fare
        findViewById<TextView>(R.id.tvRouteBadge).text = currentRoute.badge
    }

    private fun bindStops() {
        val stopContainer = findViewById<LinearLayout>(R.id.stopListContainer)
        stopContainer.removeAllViews()

        currentRoute.stops.forEachIndexed { index, stop ->
            val stopText = TextView(this).apply {
                text = getString(
                    R.string.route_map_stop_item,
                    (index + 1).toString(),
                    stop.name,
                    stop.type.label
                )
                setTextColor(Color.parseColor("#334155"))
                textSize = 12f
                setPadding(0, if (index == 0) 0 else 8, 0, 0)
            }
            stopContainer.addView(stopText)
        }
    }

    private fun setupActions() {
        findViewById<TextView>(R.id.btnCloseRoute).setOnClickListener {
            finish()
        }

        findViewById<TextView>(R.id.btnOpenMaps).setOnClickListener {
            openExternalMaps()
        }
        findViewById<TextView>(R.id.btnBookTicket).setOnClickListener {
            val intent = Intent(this, BookTicketActivity::class.java)
            intent.putExtra("ROUTE_ID", routeId)
            startActivity(intent)
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
            if (!hasUsableMapsKey()) {
                Toast.makeText(this, R.string.route_map_fallback_hint, Toast.LENGTH_SHORT).show()
            }

            val destination = currentRoute.destination
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

    private fun routeForId(routeId: Int): RouteInfo {
        return when (routeId) {
            101 -> RouteInfo(
                code = "MRT1",
                title = "Bến Thành → Suối Tiên",
                summary = "Lộ trình Metro số 1 đi dọc trục TP.HCM - Thủ Đức với các ga chính trên tuyến.",
                duration = "30 phút",
                fare = "8.000đ - 20.000đ",
                badge = "Metro số 1",
                origin = LatLng(10.7715, 106.6985), // Vị trí ga ngầm Bến Thành
                destination = LatLng(10.8757, 106.8166), // Vị trí ga Bến xe Suối Tiên mới
                path = listOf(
                    LatLng(10.7715, 106.6985), // Ga Bến Thành
                    LatLng(10.7761, 106.7032), // Ga Nhà hát TP
                    LatLng(10.7839, 106.7061), // Ga Ba Son
                    LatLng(10.7936, 106.7214), // Ga Văn Thánh
                    LatLng(10.7984, 106.7262), // Ga Tân Cảng (Chân cầu SG)
                    LatLng(10.8037, 106.7369), // Ga Thảo Điền
                    LatLng(10.8078, 106.7445), // Ga An Phú
                    LatLng(10.8242, 106.7621), // Ga Rạch Chiếc
                    LatLng(10.8358, 106.7725), // Ga Phước Long
                    LatLng(10.8447, 106.7801), // Ga Bình Thái
                    LatLng(10.8524, 106.7876), // Ga Thủ Đức
                    LatLng(10.8604, 106.7972), // Ga Khu Công nghệ cao
                    LatLng(10.8696, 106.8052), // Ga ĐHQG
                    LatLng(10.8757, 106.8166)  // Ga Bến xe Suối Tiên
                ),
                stops = listOf(
                    RouteStop("Ga Bến Thành", LatLng(10.7715, 106.6985), StopType.METRO),
                    RouteStop("Ga Nhà hát Thành phố", LatLng(10.7761, 106.7032), StopType.METRO),
                    RouteStop("Ga Ba Son", LatLng(10.7839, 106.7061), StopType.METRO),
                    RouteStop("Ga Văn Thánh", LatLng(10.7936, 106.7214), StopType.METRO),
                    RouteStop("Ga Tân Cảng", LatLng(10.7984, 106.7262), StopType.METRO),
                    RouteStop("Ga Thảo Điền", LatLng(10.8037, 106.7369), StopType.METRO),
                    RouteStop("Ga An Phú", LatLng(10.8078, 106.7445), StopType.METRO),
                    RouteStop("Ga Rạch Chiếc", LatLng(10.8242, 106.7621), StopType.METRO),
                    RouteStop("Ga Phước Long", LatLng(10.8358, 106.7725), StopType.METRO),
                    RouteStop("Ga Bình Thái", LatLng(10.8447, 106.7801), StopType.METRO),
                    RouteStop("Ga Thủ Đức", LatLng(10.8524, 106.7876), StopType.METRO),
                    RouteStop("Ga Khu Công nghệ cao", LatLng(10.8604, 106.7972), StopType.METRO),
                    RouteStop("Ga Đại học Quốc gia", LatLng(10.8696, 106.8052), StopType.METRO),
                    RouteStop("Ga Bến xe Suối Tiên", LatLng(10.8757, 106.8166), StopType.METRO)
                ),
                markerHue = BitmapDescriptorFactory.HUE_AZURE,
                routeColor = 0xFF0E4DB5.toInt(),
                destinationLabel = "Suối Tiên"
            )

            2 -> RouteInfo(
                code = "M1",
                title = "Bình Thái → Tham Lương",
                summary = "Metro số 1 kết nối các ga chính với lộ trình rõ ràng trên bản đồ.",
                duration = "18 phút",
                fare = "12.000đ",
                badge = "Metro số 1",
                origin = LatLng(10.8046, 106.7473),
                destination = LatLng(10.8559, 106.6287),
                path = listOf(
                    LatLng(10.8046, 106.7473),
                    LatLng(10.8141, 106.7289),
                    LatLng(10.8345, 106.6932),
                    LatLng(10.8559, 106.6287)
                ),
                stops = listOf(
                    RouteStop("Ga Bình Thái", LatLng(10.8046, 106.7473), StopType.METRO),
                    RouteStop("Ga Thảo Điền", LatLng(10.8141, 106.7289), StopType.METRO),
                    RouteStop("Ga Tân Cảng", LatLng(10.8345, 106.6932), StopType.METRO),
                    RouteStop("Ga Tham Lương", LatLng(10.8559, 106.6287), StopType.METRO)
                ),
                markerHue = BitmapDescriptorFactory.HUE_AZURE,
                routeColor = 0xFF1565C0.toInt(),
                destinationLabel = "Tham Lương"
            )

            3 -> RouteInfo(
                code = "W01",
                title = "Bạch Đằng → Linh Đông",
                summary = "Waterbus W01 đi dọc sông Sài Gòn, hiển thị lộ trình trực quan trên bản đồ.",
                duration = "35 phút",
                fare = "15.000đ",
                badge = "Waterbus",
                origin = LatLng(10.7702, 106.7058),
                destination = LatLng(10.8618, 106.7415),
                path = listOf(
                    LatLng(10.7702, 106.7058),
                    LatLng(10.7890, 106.7109),
                    LatLng(10.8231, 106.7214),
                    LatLng(10.8618, 106.7415)
                ),
                stops = listOf(
                    RouteStop("Bến Bạch Đằng", LatLng(10.7702, 106.7058), StopType.WATERBUS),
                    RouteStop("Bến Bình An", LatLng(10.7890, 106.7109), StopType.WATERBUS),
                    RouteStop("Bến Thanh Đa", LatLng(10.8231, 106.7214), StopType.WATERBUS),
                    RouteStop("Bến Linh Đông", LatLng(10.8618, 106.7415), StopType.WATERBUS)
                ),
                markerHue = BitmapDescriptorFactory.HUE_CYAN,
                routeColor = 0xFF0F766E.toInt(),
                destinationLabel = "Linh Đông"
            )

            else -> RouteInfo(
                code = "01",
                title = "Công trường Mê Linh → Bến xe Chợ Lớn",
                summary = "Tuyến buýt trục chính kết nối Trung tâm Quận 1 với khu vực giao thương Chợ Lớn.",
                duration = "45 phút",
                fare = "7.000đ",
                badge = "Buýt số 01",
                origin = LatLng(10.7749, 106.7061), // Công trường Mê Linh
                destination = LatLng(10.7516, 106.6534), // Bến xe Chợ Lớn
                path = listOf(
                    LatLng(10.7749, 106.7061), // CT Mê Linh
                    LatLng(10.7725, 106.7042), // Bến Bạch Đằng
                    LatLng(10.7701, 106.6998), // Hàm Nghi
                    LatLng(10.7675, 106.6942), // Trần Hưng Đạo
                    LatLng(10.7592, 106.6811), // Chợ Nânxi
                    LatLng(10.7554, 106.6705), // Trần Bình Trọng
                    LatLng(10.7521, 106.6621), // Ngô Quyền
                    LatLng(10.7516, 106.6534)  // BX Chợ Lớn
                ),
                stops = listOf(
                    RouteStop("Công trường Mê Linh", LatLng(10.7749, 106.7061), StopType.BUS),
                    RouteStop("Bến Bạch Đằng", LatLng(10.7725, 106.7042), StopType.BUS),
                    RouteStop("Cục Hải Quan Thành Phố", LatLng(10.7711, 106.7055), StopType.BUS),
                    RouteStop("Chợ Cũ", LatLng(10.7705, 106.7018), StopType.BUS),
                    RouteStop("Trường Cao Thắng", LatLng(10.7701, 106.6998), StopType.BUS),
                    RouteStop("Trạm trung chuyển Hàm Nghi", LatLng(10.7695, 106.6975), StopType.BUS),
                    RouteStop("Ký Con", LatLng(10.7682, 106.6958), StopType.BUS),
                    RouteStop("Trần Đình Xu", LatLng(10.7635, 106.6879), StopType.BUS),
                    RouteStop("Tổng Cty Samco", LatLng(10.7618, 106.6852), StopType.BUS),
                    RouteStop("Chợ Nânxi", LatLng(10.7592, 106.6811), StopType.BUS),
                    RouteStop("Bệnh viện Chấn thương Chỉnh hình", LatLng(10.7562, 106.6741), StopType.BUS),
                    RouteStop("Rạp Đồng Tháp", LatLng(10.7545, 106.6685), StopType.BUS),
                    RouteStop("Nhà Văn hóa Quận 5", LatLng(10.7528, 106.6635), StopType.BUS),
                    RouteStop("Tản Đà", LatLng(10.7519, 106.6601), StopType.BUS),
                    RouteStop("Hải Thượng Lãn Ông", LatLng(10.7512, 106.6575), StopType.BUS),
                    RouteStop("Bến xe buýt Chợ Lớn", LatLng(10.7516, 106.6534), StopType.BUS)
                ),
                markerHue = BitmapDescriptorFactory.HUE_ORANGE,
                routeColor = 0xFF10B981.toInt(), // Chỉnh thành màu xanh lá (Emerald) giống nhận diện BusMap
                destinationLabel = "Chợ Lớn"

            )
        }
    }

    private data class RouteInfo(
        val code: String,
        val title: String,
        val summary: String,
        val duration: String,
        val fare: String,
        val badge: String,
        val origin: LatLng,
        val destination: LatLng,
        val path: List<LatLng>,
        val stops: List<RouteStop>,
        val markerHue: Float,
        val routeColor: Int,
        val destinationLabel: String
    )

    private data class RouteStop(
        val name: String,
        val position: LatLng,
        val type: StopType
    )

    private enum class StopType(val label: String, val markerHue: Float) {
        BUS("Trạm xe buýt", BitmapDescriptorFactory.HUE_ORANGE),
        METRO("Ga metro", BitmapDescriptorFactory.HUE_AZURE),
        WATERBUS("Bến waterbus", BitmapDescriptorFactory.HUE_CYAN)
    }

    companion object {
        private const val EXTRA_ROUTE_ID = "ROUTE_ID"
        private const val DEFAULT_ROUTE_ID = 1
        private const val PANEL_EXPANDED_PERCENT = 0.52f
        private const val PANEL_COLLAPSED_PERCENT = 0.72f
    }
}