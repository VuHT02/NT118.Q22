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
import com.example.citymove.adapter.PlaceSuggestionAdapter
import com.example.citymove.adapter.SuggestedRouteAdapter
import com.example.citymove.databinding.ActivitySearchMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.*
import java.util.Locale

class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // ─── ViewBinding ────────────────────────────────────────────────────────
    private lateinit var binding: ActivitySearchMapBinding

    // ─── Google Maps ────────────────────────────────────────────────────────
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ─── Bottom Sheet ────────────────────────────────────────────────────────
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // ─── Adapters ────────────────────────────────────────────────────────────
    private lateinit var suggestionAdapter: PlaceSuggestionAdapter
    private lateinit var suggestedRouteAdapter: SuggestedRouteAdapter

    // ─── State ───────────────────────────────────────────────────────────────
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var activeInputField: ActiveField = ActiveField.ORIGIN
    private var searchJob: Job? = null

    private enum class ActiveField { ORIGIN, DESTINATION }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        // Mặc định: TP.HCM (Quận 1)
        private val DEFAULT_LOCATION = LatLng(10.7769, 106.7009)
        private const val DEFAULT_ZOOM = 13f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initMap()
        initAdapters()
        initBottomSheet()
        setupSearchInputs()
        setupButtons()
    }

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

        googleMap.setOnMapClickListener { latLng ->
            handleMapClick(latLng)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
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
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                )
                if (originLatLng == null) {
                    originLatLng = currentLatLng
                    binding.etOrigin.setText("Vị trí của tôi")
                    placeOriginMarker(currentLatLng)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        }
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
            MarkerOptions()
                .position(latLng)
                .title("Điểm xuất phát")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
    }

    private fun placeDestinationMarker(latLng: LatLng) {
        destinationMarker?.remove()
        destinationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Điểm đến")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
    }

    private fun drawRouteLine(origin: LatLng, destination: LatLng) {
        routePolyline?.remove()
        routePolyline = googleMap.addPolyline(
            PolylineOptions()
                .add(origin, destination)
                .width(8f)
                .color(ContextCompat.getColor(this, R.color.green_500))
                .geodesic(true)
        )
        val bounds = LatLngBounds.builder()
            .include(origin)
            .include(destination)
            .build()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        )
    }

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
            intent.putExtra("ROUTE_ID", route.routeId.toIntOrNull() ?: 1)
            startActivity(intent)
        }
        binding.recyclerSuggestedRoutes.apply {
            layoutManager = LinearLayoutManager(this@SearchMapActivity)
            adapter = suggestedRouteAdapter
        }
    }

    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetRoutes)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0
    }

    private fun showBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek)
    }

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

    private fun setupButtons() {
        binding.btnMyLocation.setOnClickListener { moveToCurrentLocation() }
        binding.btnLocate.setOnClickListener { moveToCurrentLocation() }
        binding.btnHome.setOnClickListener { finish() }

        binding.btnSwapLocations.setOnClickListener {
            val tmpText = binding.etOrigin.text.toString()
            val tmpLatLng = originLatLng

            binding.etOrigin.setText(binding.etDestination.text.toString())
            originLatLng = destinationLatLng
            originMarker?.remove()
            originLatLng?.let { placeOriginMarker(it) }

            binding.etDestination.setText(tmpText)
            destinationLatLng = tmpLatLng
            destinationMarker?.remove()
            destinationLatLng?.let { placeDestinationMarker(it) }
        }

        binding.btnClearDest.setOnClickListener {
            binding.etDestination.setText("")
            destinationLatLng = null
            destinationMarker?.remove()
            destinationMarker = null
            routePolyline?.remove()
            routePolyline = null
            binding.btnClearDest.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.btnSearchRoute.setOnClickListener { performRouteSearch() }

        binding.btnCompass.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder(googleMap.cameraPosition).bearing(0f).build()
            ))
        }
    }

    private fun searchPlaceSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            hideSuggestions()
            return
        }
        searchJob = lifecycleScope.launch {
            delay(400)
            val results = fetchPlaceSuggestions(query)
            if (results.isEmpty()) {
                hideSuggestions()
            } else {
                suggestionAdapter.submitList(results)
                binding.cardSuggestions.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun fetchPlaceSuggestions(query: String): List<PlaceSuggestion> {
        delay(200)
        return listOf(
            PlaceSuggestion("Bến xe Miền Đông", "292 Đinh Bộ Lĩnh, Bình Thạnh", LatLng(10.8142, 106.7125)),
            PlaceSuggestion("Bến xe Miền Tây", "395 Kinh Dương Vương, Bình Tân", LatLng(10.7388, 106.6089)),
            PlaceSuggestion("Chợ Bến Thành", "Quận 1, TP.HCM", LatLng(10.7719, 106.6983)),
        ).filter { it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) }
    }

    private fun hideSuggestions() {
        binding.cardSuggestions.visibility = View.GONE
    }

    private fun performRouteSearch() {
        val origin = originLatLng
        val destination = destinationLatLng

        if (origin == null) {
            binding.etOrigin.error = "Chọn điểm xuất phát"
            return
        }
        if (destination == null) {
            binding.etDestination.error = "Nhập điểm đến"
            return
        }

        hideKeyboard()
        hideSuggestions()
        showLoading(true)

        lifecycleScope.launch {
            try {
                val routes = searchBusRoutes(origin, destination)
                drawRouteLine(origin, destination)
                updateRouteSummary(routes)
                suggestedRouteAdapter.submitList(routes)
                showBottomSheet()
            } catch (e: Exception) {
                showError("Không tìm được tuyến xe. Vui lòng thử lại.")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun searchBusRoutes(origin: LatLng, destination: LatLng): List<SuggestedRoute> {
        delay(1500)
        return listOf(
            SuggestedRoute(
                routeId = "101",
                routeNumber = "M1",
                routeName = "Bến Thành → Suối Tiên",
                schedule = "05:00 - 22:00",
                frequency = "10p/chuyến",
                fare = "15.000đ",
                boardAt = "Ga Bến Thành",
                alightAt = "Ga Suối Tiên",
                nextArrivalMin = 5
            ),
            SuggestedRoute(
                routeId = "3",
                routeNumber = "W01",
                routeName = "Bạch Đằng → Linh Đông",
                schedule = "06:00 - 19:00",
                frequency = "30p/chuyến",
                fare = "15.000đ",
                boardAt = "Bến Bạch Đằng",
                alightAt = "Bến Linh Đông",
                nextArrivalMin = 12
            )
        )
    }

    private fun updateRouteSummary(routes: List<SuggestedRoute>) {
        binding.tvRouteCount.text = "${routes.size} tuyến"
        binding.tvTotalDistance.text = "12.5 km"
        binding.tvTotalTime.text = "~35 phút"
        binding.tvTotalFare.text = routes.firstOrNull()?.fare ?: "—"
    }

    private fun showLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            .show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    data class PlaceSuggestion(
        val name: String,
        val address: String,
        val latLng: LatLng
    )

    data class SuggestedRoute(
        val routeId: String,
        val routeNumber: String,
        val routeName: String,
        val schedule: String,
        val frequency: String,
        val fare: String,
        val boardAt: String,
        val alightAt: String,
        val nextArrivalMin: Int
    )
}
