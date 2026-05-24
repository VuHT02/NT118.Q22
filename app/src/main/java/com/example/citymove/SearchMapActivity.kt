package com.example.citymove

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.citymove.adapter.PlaceSuggestion
import com.example.citymove.adapter.PlaceSuggestionAdapter
import com.example.citymove.adapter.SuggestedRoute
import com.example.citymove.adapter.SuggestedRouteAdapter
import com.example.citymove.databinding.SearchMapActivityBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Locale

class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: SearchMapActivityBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var db: FirebaseFirestore

    private lateinit var suggestionAdapter: PlaceSuggestionAdapter
    private lateinit var suggestedRouteAdapter: SuggestedRouteAdapter

    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var stopMarkers: MutableList<Marker> = mutableListOf()
    private var activeInputField: ActiveField = ActiveField.ORIGIN
    private var searchJob: Job? = null

    // Stops cache loaded once from Firestore
    private var allStops: List<StopDocument> = emptyList()
    private var destinationStop: StopDocument? = null

    private data class StopDocument(
        val name: String,
        val lat: Double,
        val lng: Double,
        val routeLineCode: String,
        val routeType: String,
        val sequence: Int = 0
    )

    private data class RouteSearchResult(
        val routes: List<SuggestedRoute>,
        val pathPoints: List<LatLng>,
        val distanceKm: Double = 0.0,
        val durationMinutes: Int = 0
    )

    private enum class ActiveField { ORIGIN, DESTINATION }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private val DEFAULT_LOCATION = LatLng(10.7769, 106.7009)
        private const val DEFAULT_ZOOM = 13f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SearchMapActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadAllStops()
        initMap()
        initAdapters()
        initBottomSheet()
        setupSearchInputs()
        setupButtons()
    }

    // ─── Firestore stops cache ────────────────────────────────────────────────

    private fun loadAllStops() {
        db.collection("stops").get()
            .addOnSuccessListener { snapshot ->
                allStops = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val lat  = doc.getDouble("lat")  ?: return@mapNotNull null
                    val lng  = doc.getDouble("lng")  ?: return@mapNotNull null
                    StopDocument(
                        name           = name,
                        lat            = lat,
                        lng            = lng,
                        routeLineCode  = doc.getString("routeLineCode") ?: "",
                        routeType      = doc.getString("routeType") ?: "",
                        sequence       = doc.getLong("sequence")?.toInt() ?: 0
                    )
                }
            }
    }

    // ─── Map ─────────────────────────────────────────────────────────────────

    private fun initMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isCompassEnabled = false
            uiSettings.isMyLocationButtonEnabled = false
            moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM))
        }
        checkLocationPermission()
        googleMap.setOnMapClickListener { latLng -> handleMapClick(latLng) }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) enableMyLocation()
        else ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            moveToCurrentLocation()
        }
    }

    private fun moveToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                if (originLatLng == null) {
                    originLatLng = latLng
                    binding.etOrigin.setText("Vị trí của tôi")
                    placeOriginMarker(latLng)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) enableMyLocation()
    }

    private fun handleMapClick(latLng: LatLng) {
        val posStr = String.format(Locale.getDefault(), "%.4f, %.4f", latLng.latitude, latLng.longitude)
        when (activeInputField) {
            ActiveField.ORIGIN -> {
                originLatLng = latLng
                binding.etOrigin.setText(posStr)
                placeOriginMarker(latLng)
            }
            ActiveField.DESTINATION -> {
                destinationLatLng = latLng
                destinationStop = null
                binding.etDestination.setText(posStr)
                placeDestinationMarker(latLng)
                binding.btnClearDest.visibility = View.VISIBLE
            }
        }
        hideSuggestions()
    }

    private fun placeOriginMarker(latLng: LatLng) {
        originMarker?.remove()
        originMarker = googleMap.addMarker(
            MarkerOptions().position(latLng).title("Điểm xuất phát")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    private fun placeDestinationMarker(latLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = googleMap.addMarker(
            MarkerOptions().position(latLng).title("Điểm đến")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    private fun drawRoutePolyline(points: List<LatLng>) {
        routePolyline?.remove()
        stopMarkers.forEach { it.remove() }
        stopMarkers.clear()

        routePolyline = googleMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(ContextCompat.getColor(this, R.color.blue_primary))
                .geodesic(true)
        )

        // Đặt marker nhỏ cho từng trạm trên tuyến
        points.forEachIndexed { index, pt ->
            val isTerminus = index == 0 || index == points.lastIndex
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(pt)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                        if (isTerminus) BitmapDescriptorFactory.HUE_BLUE
                        else BitmapDescriptorFactory.HUE_CYAN
                    ))
                    .anchor(0.5f, 0.5f)
            ) ?: return@forEachIndexed
            stopMarkers.add(marker)
        }

        val boundsBuilder = LatLngBounds.builder()
        points.forEach { boundsBuilder.include(it) }
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
        } catch (e: Exception) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), 13f))
        }
    }

    private fun drawStraightLine(origin: LatLng, destination: LatLng) {
        routePolyline?.remove()
        stopMarkers.forEach { it.remove() }
        stopMarkers.clear()
        routePolyline = googleMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(8f)
                .color(ContextCompat.getColor(this, R.color.green_500))
                .geodesic(true)
        )
        val bounds = LatLngBounds.builder().include(origin).include(destination).build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
    }

    // ─── Adapters ─────────────────────────────────────────────────────────────

    private fun initAdapters() {
        suggestionAdapter = PlaceSuggestionAdapter { place ->
            when (activeInputField) {
                ActiveField.ORIGIN -> {
                    binding.etOrigin.setText(place.name)
                    originLatLng = place.latLng
                    placeOriginMarker(place.latLng)
                }
                ActiveField.DESTINATION -> {
                    binding.etDestination.setText(place.name)
                    destinationLatLng = place.latLng
                    placeDestinationMarker(place.latLng)
                    binding.btnClearDest.visibility = View.VISIBLE
                    // Lưu lại thông tin trạm để tra cứu tuyến
                    destinationStop = allStops.firstOrNull { it.name == place.name }
                }
            }
            hideSuggestions()
            hideKeyboard()
        }
        binding.recyclerSuggestions.apply {
            layoutManager = LinearLayoutManager(this@SearchMapActivity)
            adapter = suggestionAdapter
        }

        suggestedRouteAdapter = SuggestedRouteAdapter { route ->
            val intent = Intent(this, RouteDetailActivity::class.java)
            intent.putExtra("ROUTE_ID_STRING", route.routeId)
            startActivity(intent)
        }
        binding.recyclerSuggestedRoutes.apply {
            layoutManager = LinearLayoutManager(this@SearchMapActivity)
            adapter = suggestedRouteAdapter
        }
    }

    // ─── Bottom sheet ─────────────────────────────────────────────────────────

    private fun initBottomSheet() {
        binding.bottomSheetRoutes.visibility = View.GONE
    }

    private fun showBottomSheet() {
        binding.bottomSheetRoutes.visibility = View.VISIBLE
    }

    // ─── Search inputs ────────────────────────────────────────────────────────

    private fun setupSearchInputs() {
        binding.etOrigin.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) activeInputField = ActiveField.ORIGIN
        }
        binding.etOrigin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchPlaceSuggestions(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etDestination.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) activeInputField = ActiveField.DESTINATION
        }
        binding.etDestination.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString() ?: ""
                binding.btnClearDest.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                searchPlaceSuggestions(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etDestination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performRouteSearch()
                true
            } else false
        }
    }

    // ─── Buttons ──────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.btnMyLocation.setOnClickListener { moveToCurrentLocation() }
        binding.btnLocate.setOnClickListener { moveToCurrentLocation() }
        binding.btnHome.setOnClickListener { finish() }

        binding.btnSwapLocations.setOnClickListener {
            val tmpText   = binding.etOrigin.text.toString()
            val tmpLatLng = originLatLng

            binding.etOrigin.setText(binding.etDestination.text.toString())
            originLatLng = destinationLatLng
            originMarker?.remove()
            originLatLng?.let { placeOriginMarker(it) }

            binding.etDestination.setText(tmpText)
            destinationLatLng = tmpLatLng
            destinationMarker?.remove()
            destinationLatLng?.let { placeDestinationMarker(it) }

            // Swap stop info
            val tmp = destinationStop
            destinationStop = allStops.firstOrNull { it.name == binding.etDestination.text.toString() }
            tmp?.let { /* origin stop not tracked currently */ }
        }

        binding.btnClearDest.setOnClickListener {
            binding.etDestination.setText("")
            destinationLatLng = null
            destinationStop = null
            destinationMarker?.remove()
            destinationMarker = null
            routePolyline?.remove()
            routePolyline = null
            stopMarkers.forEach { it.remove() }
            stopMarkers.clear()
            binding.btnClearDest.visibility = View.GONE
            binding.bottomSheetRoutes.visibility = View.GONE
        }

        binding.btnSearchRoute.setOnClickListener { performRouteSearch() }

        binding.btnCompass.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder(googleMap.cameraPosition).bearing(0f).build()
            ))
        }
    }

    // ─── Suggestion search (client-side filter từ cache) ─────────────────────

    private fun searchPlaceSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 2) { hideSuggestions(); return }
        searchJob = lifecycleScope.launch {
            delay(300)
            val results = filterStopSuggestions(query)
            if (results.isEmpty()) hideSuggestions()
            else {
                suggestionAdapter.submitList(results)
                binding.cardSuggestions.visibility = View.VISIBLE
            }
        }
    }

    private fun filterStopSuggestions(query: String): List<PlaceSuggestion> {
        val q = query.trim().lowercase()
        return allStops
            .filter { it.name.lowercase().contains(q) }
            .sortedBy { it.sequence }
            .take(6)
            .map { stop ->
                val routeLabel = when (stop.routeType) {
                    "METRO"     -> "Metro ${stop.routeLineCode}"
                    "WATER_BUS" -> "Buýt sông ${stop.routeLineCode}"
                    else        -> "Xe buýt ${stop.routeLineCode}"
                }
                PlaceSuggestion(stop.name, routeLabel, LatLng(stop.lat, stop.lng))
            }
    }

    private fun hideSuggestions() {
        binding.cardSuggestions.visibility = View.GONE
    }

    // ─── Route search (Firestore) ─────────────────────────────────────────────

    private fun performRouteSearch() {
        val origin      = originLatLng
        val destination = destinationLatLng

        if (origin == null) { binding.etOrigin.error = "Chọn điểm xuất phát"; return }
        if (destination == null) { binding.etDestination.error = "Nhập điểm đến"; return }

        hideKeyboard()
        hideSuggestions()
        showLoading(true)

        lifecycleScope.launch {
            try {
                val result = searchBusRoutes(destination)
                if (result.pathPoints.size >= 2) {
                    drawRoutePolyline(result.pathPoints)
                } else {
                    drawStraightLine(origin, destination)
                }
                updateRouteSummary(result)
                suggestedRouteAdapter.submitList(result.routes)
                showBottomSheet()
            } catch (e: Exception) {
                showError("Không tìm được tuyến xe. Vui lòng thử lại.")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun searchBusRoutes(destination: LatLng): RouteSearchResult {
        val destStop = destinationStop
            ?: return RouteSearchResult(emptyList(), emptyList())

        val snapshot = db.collection("routes")
            .whereEqualTo("lineCode", destStop.routeLineCode)
            .get()
            .await()

        val doc = snapshot.documents.firstOrNull()
            ?: return RouteSearchResult(emptyList(), emptyList())

        // Đọc pathPoints
        @Suppress("UNCHECKED_CAST")
        val rawPath = doc.get("pathPoints") as? List<Map<String, Any>>
        val pathPoints = rawPath?.mapNotNull { point ->
            val lat = (point["lat"] as? Double) ?: return@mapNotNull null
            val lng = (point["lng"] as? Double) ?: return@mapNotNull null
            LatLng(lat, lng)
        } ?: emptyList()

        val price    = doc.getLong("price")?.toInt() ?: 0
        val duration = doc.getLong("durationMinutes")?.toInt() ?: 0
        val distance = doc.getDouble("distanceKm") ?: 0.0
        val schedule = when (destStop.routeType) {
            "METRO"     -> "05:30 – 22:30"
            "WATER_BUS" -> "06:00 – 19:00"
            else        -> "05:00 – 22:00"
        }
        val frequency = when (destStop.routeType) {
            "METRO"     -> "10p/chuyến"
            "WATER_BUS" -> "30p/chuyến"
            else        -> "15p/chuyến"
        }

        val route = SuggestedRoute(
            routeId        = doc.id,
            routeNumber    = destStop.routeLineCode,
            routeName      = doc.getString("name") ?: "",
            schedule       = schedule,
            frequency      = frequency,
            fare           = if (price > 0) String.format("%,d₫", price) else "Miễn phí",
            boardAt        = doc.getString("startStation") ?: "",
            alightAt       = destStop.name,
            nextArrivalMin = (3..15).random(),
            distance       = "${distance} km",
            duration       = "${duration} phút"
        )

        return RouteSearchResult(
            routes          = listOf(route),
            pathPoints      = pathPoints,
            distanceKm      = distance,
            durationMinutes = duration
        )
    }

    private fun updateRouteSummary(result: RouteSearchResult) {
        binding.tvRouteCount.text    = "${result.routes.size} tuyến"
        binding.tvTotalDistance.text = if (result.distanceKm > 0) "${result.distanceKm} km" else "—"
        binding.tvTotalTime.text     = if (result.durationMinutes > 0) "~${result.durationMinutes} phút" else "—"
        binding.tvTotalFare.text     = result.routes.firstOrNull()?.fare ?: "—"
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
