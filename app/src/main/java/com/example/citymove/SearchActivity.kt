package com.example.citymove

import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var etOrigin: EditText
    private lateinit var etDestination: EditText
    private lateinit var tvRouteCount: TextView
    private lateinit var tvTotalDistance: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvTotalFare: TextView

    private var googleMap: GoogleMap? = null
    private val DEFAULT_CITY_CENTER = LatLng(10.762622, 106.660172)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_map_activity)
        supportActionBar?.hide()

        // Khởi tạo các View dựa trên ID thực tế trong activity_search.xml
        etOrigin = findViewById(R.id.etOrigin)
        etDestination = findViewById(R.id.etDestination)
        tvRouteCount = findViewById(R.id.tvRouteCount)
        tvTotalDistance = findViewById(R.id.tvTotalDistance)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        tvTotalFare = findViewById(R.id.tvTotalFare)

        setupActions()
        setupMap()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CITY_CENTER, 12f))
    }

    private fun setupActions() {
        // XML sử dụng btnHome thay vì btnBack
        findViewById<View>(R.id.btnHome).setOnClickListener { finish() }

        // XML sử dụng btnSwapLocations thay vì btnSwapPoints
        findViewById<View>(R.id.btnSwapLocations).setOnClickListener {
            val originText = etOrigin.text.toString()
            val destText = etDestination.text.toString()
            etOrigin.setText(destText)
            etDestination.setText(originText)
        }

        // Fix lỗi btnRouteNow -> btnSearchRoute cho đúng với XML
        findViewById<View>(R.id.btnSearchRoute).setOnClickListener {
            val origin = etOrigin.text.toString().trim()
            val destination = etDestination.text.toString().trim()
            
            if (origin.isNotEmpty() && destination.isNotEmpty()) {
                startRouteLookup(origin, destination)
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        etDestination.setOnEditorActionListener { _, actionId, event ->
            val isSubmit = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)

            if (isSubmit) {
                startRouteLookup(etOrigin.text.toString(), etDestination.text.toString())
                true
            } else false
        }

        // Nút định vị
        findViewById<View>(R.id.btnLocate).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CITY_CENTER, 15f))
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun startRouteLookup(origin: String, destination: String) {
        geocodeAddress(origin) { originLatLng ->
            if (originLatLng == null) {
                Toast.makeText(this, "Không tìm thấy điểm đi", Toast.LENGTH_SHORT).show()
                return@geocodeAddress
            }
            geocodeAddress(destination) { destLatLng ->
                if (destLatLng == null) {
                    Toast.makeText(this, "Không tìm thấy điểm đến", Toast.LENGTH_SHORT).show()
                } else {
                    displayRoute(origin, destination, originLatLng, destLatLng)
                }
            }
        }
    }

    private fun geocodeAddress(query: String, onResult: (LatLng?) -> Unit) {
        val geocoder = Geocoder(this, Locale.getDefault())
        thread {
            try {
                @Suppress("DEPRECATION")
                val addr = geocoder.getFromLocationName(query, 1)
                runOnUiThread { onResult(addr?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }) }
            } catch (e: Exception) {
                runOnUiThread { onResult(null) }
            }
        }
    }

    private fun displayRoute(oTitle: String, dTitle: String, o: LatLng, d: LatLng) {
        googleMap?.apply {
            clear()
            addMarker(MarkerOptions().position(o).title(oTitle))
            addMarker(MarkerOptions().position(d).title(dTitle))
            addPolyline(PolylineOptions().add(o, d).color(0xFFF97316.toInt()).width(10f))
            
            val bounds = LatLngBounds.Builder().include(o).include(d).build()
            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }

        // Cập nhật thông số giả lập
        val results = FloatArray(1)
        Location.distanceBetween(o.latitude, o.longitude, d.latitude, d.longitude, results)
        val dist = results[0] / 1000

        tvTotalDistance.text = String.format("%.1f km", dist)
        tvRouteCount.text = "1"
        tvTotalTime.text = "${(dist * 3).toInt()} phút"
        tvTotalFare.text = "7.000đ"
    }
}
