package com.example.citymove

import android.Manifest
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

class SearchMapActivity : AppCompatActivity(), OnMapReadyCallback {

    // ─── ViewBinding ────────────────────────────────────────────────────────
    private lateinit var binding: ActivitySearchMapBinding

    // ─── Google Maps ────────────────────────────────────────────────────────
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ─── Bottom Sheet ────────────────────────────────────────────────────────
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

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
        // Mặc định: Hà Nội
        private val DEFAULT_LOCATION = LatLng(21.0285, 105.8542)
        private const val DEFAULT_ZOOM = 13f
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ════════════════════════════════════════════════════════════════════════

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

    // ════════════════════════════════════════════════════════════════════════
    //  Khởi tạo bản đồ
    // ════════════════════════════════════════════════════════════════════════

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

        // Tap lên bản đồ để chọn điểm
        googleMap.setOnMapClickListener { latLng ->
            handleMapClick(latLng)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Quyền vị trí
    // ════════════════════════════════════════════════════════════════════════

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
                // Tự động đặt vị trí hiện tại làm điểm đi
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

    // ════════════════════════════════════════════════════════════════════════
    //  Tap bản đồ → chọn điểm đi / đến
    // ════════════════════════════════════════════════════════════════════════

    private fun handleMapClick(latLng: LatLng) {
        when (activeInputField) {
            ActiveField.ORIGIN -> {
                originLatLng = latLng
                binding.etOrigin.setText("${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}")
                placeOriginMarker(latLng)
            }
            ActiveField.DESTINATION -> {
                destinationLatLng = latLng
                binding.etDestination.setText("${latLng.latitude.format(4)}, ${latLng.longitude.format(4)}")
                placeDestinationMarker(latLng)
                binding.btnClearDest.visibility = View.VISIBLE
            }
        }
        hideSuggestions()
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Marker trên bản đồ
    // ════════════════════════════════════════════════════════════════════════

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
        // Zoom camera vừa 2 điểm
        val bounds = LatLngBounds.builder()
            .include(origin)
            .include(destination)
            .build()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 120)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Adapters
    // ════════════════════════════════════════════════════════════════════════

    private fun initAdapters() {
        // Gợi ý địa điểm
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

        // Tuyến gợi ý
        suggestedRouteAdapter = SuggestedRouteAdapter { route ->
            // Click xem chi tiết tuyến
            BusRouteDetailActivity.start(this, route.routeId)
        }
        binding.recyclerSuggestedRoutes.apply {
            layoutManager = LinearLayoutManager(this@SearchMapActivity)
            adapter = suggestedRouteAdapter
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Bottom Sheet
    // ════════════════════════════════════════════════════════════════════════

    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetRoutes)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.peekHeight = 0
    }

    private fun showBottomSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Setup ô nhập liệu tìm kiếm
    // ════════════════════════════════════════════════════════════════════════

    private fun setupSearchInputs() {
        // --- Điểm đi ---
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

        // --- Điểm đến ---
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

        // IME Search action trên điểm đến → tìm tuyến luôn
        binding.etDestination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performRouteSearch()
                true
            } else false
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Setup buttons
    // ════════════════════════════════════════════════════════════════════════

    private fun setupButtons() {
        // Vị trí hiện tại
        binding.btnMyLocation.setOnClickListener { moveToCurrentLocation() }
        binding.btnLocate.setOnClickListener { moveToCurrentLocation() }

        // Đổi chiều A ↔ B
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

        // Xóa điểm đến
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

        // Nút tìm tuyến xe
        binding.btnSearchRoute.setOnClickListener { performRouteSearch() }

        // La bàn → về hướng Bắc
        binding.btnCompass.setOnClickListener {
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder(googleMap.cameraPosition).bearing(0f).build()
            ))
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tìm kiếm gợi ý địa điểm (debounce 400ms)
    // ════════════════════════════════════════════════════════════════════════

    private fun searchPlaceSuggestions(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            hideSuggestions()
            return
        }
        searchJob = lifecycleScope.launch {
            delay(400) // debounce
            // TODO: Gọi Places API hoặc API nội bộ
            val results = fetchPlaceSuggestions(query)
            if (results.isEmpty()) {
                hideSuggestions()
            } else {
                suggestionAdapter.submitList(results)
                binding.cardSuggestions.visibility = View.VISIBLE
            }
        }
    }

    /**
     * Gọi Places Autocomplete API – thay bằng implementation thật.
     * Hiện tại trả về data mẫu.
     */
    private suspend fun fetchPlaceSuggestions(query: String): List<PlaceSuggestion> {
        delay(200) // giả lập network
        return listOf(
            PlaceSuggestion("Bến xe Mỹ Đình", "20 Phạm Hùng, Nam Từ Liêm, Hà Nội", LatLng(21.0278, 105.7828)),
            PlaceSuggestion("Bến xe Giáp Bát", "Km8 Giải Phóng, Hoàng Mai, Hà Nội", LatLng(20.9889, 105.8445)),
            PlaceSuggestion("Long Biên", "Điểm đỗ xe buýt Yên Phụ, Ba Đình", LatLng(21.0444, 105.8566)),
        ).filter { it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) }
    }

    private fun hideSuggestions() {
        binding.cardSuggestions.visibility = View.GONE
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Tìm tuyến xe
    // ════════════════════════════════════════════════════════════════════════

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

    /**
     * Gọi API tìm tuyến xe từ A đến B.
     * Thay thế bằng call API thật (Retrofit/OkHttp).
     */
    private suspend fun searchBusRoutes(origin: LatLng, destination: LatLng): List<SuggestedRoute> {
        delay(1500) // giả lập network
        return listOf(
            SuggestedRoute(
                routeId = "E01",
                routeNumber = "E01",
                routeName = "Bến xe Mỹ Đình → KĐT Ocean Park",
                schedule = "05:00 - 21:00",
                frequency = "12p/chuyến",
                fare = "7.000đ",
                boardAt = "Long Biên (Điểm E3.3)",
                alightAt = "KĐT Ocean Park, Gia Lâm",
                nextArrivalMin = 5
            ),
            SuggestedRoute(
                routeId = "43",
                routeNumber = "43",
                routeName = "Kim Mã → Thị trấn Đông Anh",
                schedule = "05:00 - 21:00",
                frequency = "25p/chuyến",
                fare = "9.000đ",
                boardAt = "Bến xe Kim Mã",
                alightAt = "Thị trấn Đông Anh",
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

    // ════════════════════════════════════════════════════════════════════════
    //  Utilities
    // ════════════════════════════════════════════════════════════════════════

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

    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    // ════════════════════════════════════════════════════════════════════════
    //  Data classes
    // ════════════════════════════════════════════════════════════════════════

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
