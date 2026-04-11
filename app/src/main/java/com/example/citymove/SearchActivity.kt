package com.example.citymove

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale
import kotlin.concurrent.thread

class SearchActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var searchRoot: ConstraintLayout
    private lateinit var searchCard: View
    private lateinit var panelSplitGuide: Guideline
    private lateinit var panelHandleTapArea: View
    private lateinit var etOrigin: EditText
    private lateinit var etDestination: EditText
    private lateinit var tvSelectedOrigin: TextView
    private lateinit var tvSelectedDestination: TextView
    private lateinit var tvMapStatus: TextView
    private lateinit var tvMapFallback: TextView

    private var googleMap: GoogleMap? = null
    private var currentOrigin: LatLng? = null
    private var currentDestination: LatLng? = null
    private var pendingRouteRequest: Pair<String, String>? = null
    private var pendingDestinationQuery: String? = null
    private var dragStartY = 0f
    private var dragStartGuidePercent = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        supportActionBar?.hide()

        searchRoot = findViewById(R.id.searchRoot)
        searchCard = findViewById(R.id.searchCard)
        panelSplitGuide = findViewById(R.id.panelSplitGuide)
        panelHandleTapArea = findViewById(R.id.panelHandleTapArea)
        etOrigin = findViewById(R.id.etOrigin)
        etDestination = findViewById(R.id.etDestination)
        tvSelectedOrigin = findViewById(R.id.tvSelectedOrigin)
        tvSelectedDestination = findViewById(R.id.tvSelectedDestination)
        tvMapStatus = findViewById(R.id.tvMapStatus)
        tvMapFallback = findViewById(R.id.tvMapFallback)

        setupActions()
        setupDraggablePanel()

        if (hasUsableMapsKey()) {
            setupMap()
        } else {
            tvMapFallback.visibility = View.VISIBLE
            tvMapStatus.text = getString(R.string.route_map_fallback_hint)
        }

        val prefilledOrigin = intent.getStringExtra(EXTRA_ORIGIN_QUERY).orEmpty().trim()
        if (prefilledOrigin.isNotEmpty()) {
            etOrigin.setText(prefilledOrigin)
            tvSelectedOrigin.text = prefilledOrigin
        }

        val prefilledDestination = intent.getStringExtra(EXTRA_DESTINATION_QUERY).orEmpty().trim()
        if (prefilledDestination.isNotEmpty()) {
            etDestination.setText(prefilledDestination)
            submitDestinationOnly(prefilledDestination)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = false
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CITY_CENTER, 12f))

        pendingRouteRequest?.let { (origin, destination) ->
            pendingRouteRequest = null
            startRouteLookup(origin, destination)
            return
        }

        pendingDestinationQuery?.let { query ->
            pendingDestinationQuery = null
            startDestinationLookup(query)
        }
    }

    private fun setupActions() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnSwapPoints).setOnClickListener {
            swapOriginDestination()
        }

        findViewById<View>(R.id.btnPreviewDestination).setOnClickListener {
            submitDestinationOnly(etDestination.text?.toString().orEmpty())
        }

        findViewById<View>(R.id.btnRouteNow).setOnClickListener {
            submitRoute(etOrigin.text?.toString().orEmpty(), etDestination.text?.toString().orEmpty())
        }

        etDestination.setOnEditorActionListener { _, actionId, event ->
            val isSubmit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)

            if (isSubmit) {
                submitRoute(etOrigin.text?.toString().orEmpty(), etDestination.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        findViewById<View>(R.id.btnOpenGoogleMaps).setOnClickListener {
            openExternalMaps()
        }
    }

    private fun swapOriginDestination() {
        val originText = etOrigin.text?.toString().orEmpty().trim()
        val destinationText = etDestination.text?.toString().orEmpty().trim()

        etOrigin.setText(destinationText)
        etDestination.setText(originText)

        val originDisplay = tvSelectedOrigin.text.toString()
        tvSelectedOrigin.text = tvSelectedDestination.text
        tvSelectedDestination.text = originDisplay

        val previousOrigin = currentOrigin
        currentOrigin = currentDestination
        currentDestination = previousOrigin

        if (destinationText.isNotEmpty() && originText.isNotEmpty()) {
            submitRoute(destinationText, originText)
        } else if (originText.isNotEmpty()) {
            submitDestinationOnly(originText)
        } else {
            tvMapStatus.text = getString(R.string.search_map_status_default)
        }
    }

    private fun setupMap() {
        runCatching {
            val fragment = (supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment)
                ?: SupportMapFragment.newInstance().also { mapFragment ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.mapContainer, mapFragment)
                        .commitNow()
                }
            fragment.getMapAsync(this)
        }.onFailure {
            tvMapFallback.visibility = View.VISIBLE
        }
    }

    private fun submitDestinationOnly(rawValue: String) {
        val query = rawValue.trim()
        if (query.isEmpty()) {
            Toast.makeText(this, R.string.search_map_empty_destination, Toast.LENGTH_SHORT).show()
            return
        }

        tvSelectedDestination.text = query
        tvMapStatus.text = getString(R.string.search_map_status_searching_destination)
        startDestinationLookup(query)
    }

    private fun submitRoute(rawOrigin: String, rawDestination: String) {
        val origin = rawOrigin.trim()
        val destination = rawDestination.trim()

        if (origin.isEmpty()) {
            Toast.makeText(this, R.string.search_map_empty_origin, Toast.LENGTH_SHORT).show()
            return
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, R.string.search_map_empty_destination, Toast.LENGTH_SHORT).show()
            return
        }

        tvSelectedOrigin.text = origin
        tvSelectedDestination.text = destination
        tvMapStatus.text = getString(R.string.search_map_status_searching_route)
        startRouteLookup(origin, destination)
    }

    private fun startDestinationLookup(query: String) {
        if (googleMap == null) {
            pendingDestinationQuery = query
            return
        }

        geocodeAddress(query) { destinationLatLng ->
            if (destinationLatLng == null) {
                showNotFoundState(query)
            } else {
                showFoundDestination(query, destinationLatLng)
            }
        }
    }

    private fun startRouteLookup(originQuery: String, destinationQuery: String) {
        if (googleMap == null) {
            pendingRouteRequest = originQuery to destinationQuery
            return
        }

        geocodeAddress(originQuery) { originLatLng ->
            if (originLatLng == null) {
                showRouteLookupFailed(originQuery)
                return@geocodeAddress
            }

            geocodeAddress(destinationQuery) { destinationLatLng ->
                if (destinationLatLng == null) {
                    showNotFoundState(destinationQuery)
                } else {
                    showRoute(originQuery, destinationQuery, originLatLng, destinationLatLng)
                }
            }
        }
    }

    private fun geocodeAddress(query: String, onResult: (LatLng?) -> Unit) {
        if (!Geocoder.isPresent()) {
            onResult(null)
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 1) { addresses ->
                runOnUiThread {
                    val first = addresses.firstOrNull()
                    onResult(first?.let { LatLng(it.latitude, it.longitude) })
                }
            }
            return
        }

        thread {
            @Suppress("DEPRECATION")
            val first = runCatching { geocoder.getFromLocationName(query, 1)?.firstOrNull() }
                .getOrNull()

            runOnUiThread {
                onResult(first?.let { LatLng(it.latitude, it.longitude) })
            }
        }
    }

    private fun showFoundDestination(query: String, latLng: LatLng) {
        val map = googleMap ?: return
        currentOrigin = null
        currentDestination = latLng
        map.clear()
        map.addMarker(MarkerOptions().position(latLng).title(query))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        tvMapStatus.text = getString(R.string.search_map_status_found, query)
    }

    private fun showRoute(
        originQuery: String,
        destinationQuery: String,
        origin: LatLng,
        destination: LatLng
    ) {
        val map = googleMap ?: return

        currentOrigin = origin
        currentDestination = destination
        map.clear()

        map.addMarker(MarkerOptions().position(origin).title(originQuery))
        map.addMarker(MarkerOptions().position(destination).title(destinationQuery))
        map.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .color(0xFF0E4DB5.toInt())
                .width(10f)
        )

        val bounds = LatLngBounds.Builder()
            .include(origin)
            .include(destination)
            .build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))

        val distanceResult = FloatArray(1)
        Location.distanceBetween(
            origin.latitude,
            origin.longitude,
            destination.latitude,
            destination.longitude,
            distanceResult
        )
        val distanceKm = distanceResult[0] / 1000f
        tvMapStatus.text = getString(R.string.search_map_status_route_ready, distanceKm)
    }

    private fun showRouteLookupFailed(originQuery: String) {
        currentOrigin = null
        tvMapStatus.text = getString(R.string.search_map_status_origin_not_found, originQuery)
    }

    private fun showNotFoundState(query: String) {
        currentOrigin = null
        currentDestination = null
        googleMap?.apply {
            clear()
            moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CITY_CENTER, 12f))
        }
        tvMapStatus.text = getString(R.string.search_map_status_not_found, query)
    }

    private fun openExternalMaps() {
        val originText = etOrigin.text?.toString().orEmpty().trim()
        val destinationText = etDestination.text?.toString().orEmpty().trim()

        val destinationUri = if (originText.isNotEmpty() && destinationText.isNotEmpty()) {
            val originValue = currentOrigin?.let { "${it.latitude},${it.longitude}" } ?: Uri.encode(originText)
            val destinationValue = currentDestination?.let { "${it.latitude},${it.longitude}" } ?: Uri.encode(destinationText)
            "https://www.google.com/maps/dir/?api=1&origin=$originValue&destination=$destinationValue&travelmode=transit"
        } else {
            val query = destinationText.ifEmpty { DEFAULT_CITY_QUERY }
            "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(destinationUri)).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(destinationUri)))
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
                    val rootHeight = searchRoot.height
                    if (rootHeight <= 0) return@setOnTouchListener false

                    val draggedPercent = dragStartGuidePercent + ((event.rawY - dragStartY) / rootHeight)
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

        searchRoot.post {
            setGuidePercent(currentGuidePercent().coerceIn(minGuidePercent(), maxGuidePercent()))
            updateHandleAccessibility()
        }
    }

    private fun togglePanelPreset() {
        val midpoint = (PANEL_EXPANDED_PERCENT + PANEL_COLLAPSED_PERCENT) / 2f
        val target = if (currentGuidePercent() < midpoint) PANEL_COLLAPSED_PERCENT else PANEL_EXPANDED_PERCENT
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
        val rootHeight = searchRoot.height.coerceAtLeast(1)
        val minimumMapHeight = resources.displayMetrics.density * 140f
        val fixedTopBottom = (searchCard.bottom + minimumMapHeight)
        return (fixedTopBottom / rootHeight).coerceIn(0.42f, 0.78f)
    }

    private fun maxGuidePercent(): Float {
        val rootHeight = searchRoot.height.coerceAtLeast(1)
        val minimumPanelHeight = resources.displayMetrics.density * 200f
        return (1f - (minimumPanelHeight / rootHeight)).coerceIn(0.50f, 0.86f)
    }

    private fun hasUsableMapsKey(): Boolean {
        val key = getString(R.string.google_maps_key).trim()
        return key.isNotEmpty() && !key.equals("YOUR_GOOGLE_MAPS_API_KEY", ignoreCase = true)
    }

    companion object {
        const val EXTRA_DESTINATION_QUERY = "EXTRA_DESTINATION_QUERY"
        const val EXTRA_ORIGIN_QUERY = "EXTRA_ORIGIN_QUERY"
        private val DEFAULT_CITY_CENTER = LatLng(10.7769, 106.7009)
        private const val DEFAULT_CITY_QUERY = "TP. Hồ Chí Minh"
        private const val PANEL_EXPANDED_PERCENT = 0.56f
        private const val PANEL_COLLAPSED_PERCENT = 0.72f
    }
}
