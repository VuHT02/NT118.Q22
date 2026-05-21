package com.example.citymove.data.model

import com.google.android.gms.maps.model.LatLng

enum class TransportType(
    val displayName: String,
    val subtitle: String,
    val sectionLabel: String,
    val badgePrefix: String
) {
    BUS("Bus", "Hệ thống xe buýt TP.HCM", "TUYẾN XE BUÝT", "B"),
    METRO("Metro", "Hệ thống tàu điện ngầm TP.HCM", "TUYẾN METRO", "M"),
    WATER_BUS("Water Bus", "Hệ thống xe buýt đường sông TP.HCM", "TUYẾN BUÝT SÔNG", "WB");

    companion object {
        fun fromString(value: String) = entries.firstOrNull { it.name == value } ?: BUS
    }
}

enum class RouteStatus { ACTIVE, UPCOMING }

enum class StopType(val label: String) {
    BUS("Trạm xe buýt"),
    METRO("Ga metro"),
    WATER_BUS("Bến waterbus");

    companion object {
        fun fromString(value: String) = entries.firstOrNull { it.name == value } ?: BUS
    }
}

data class RouteStop(
    val name: String,
    val position: LatLng,
    val type: StopType
)

data class RouteModel(
    val id: String,
    val type: TransportType,
    val lineCode: String,
    val name: String,
    val startStation: String,
    val endStation: String,
    val distanceKm: Double,
    val stationCount: Int,
    val durationMinutes: Int,
    val status: RouteStatus,
    val lineColor: String,
    val price: Int?,
    val expectedOpenYear: Int?,
    val summary: String? = null,
    val path: List<LatLng>? = null,
    val stops: List<RouteStop>? = null
) {
    val metaText get() = "$distanceKm km · $stationCount ${
        when (type) {
            TransportType.BUS        -> "trạm"
            TransportType.METRO      -> "ga"
            TransportType.WATER_BUS  -> "bến"
        }
    }"

    val durationText get() = if (status == RouteStatus.ACTIVE) "$durationMinutes phút" else "~$durationMinutes phút"

    // FIX: "%,d₫".format(price) không hợp lệ trong Kotlin — phải dùng String.format()
    val priceOrDateText get() = when {
        status == RouteStatus.ACTIVE && price != null -> String.format("%,d₫", price)
        expectedOpenYear != null                      -> "Dự kiến $expectedOpenYear"
        else                                          -> ""
    }

    val showPricePrefix get() = status == RouteStatus.ACTIVE

    val ctaText get() = if (status == RouteStatus.ACTIVE) "Mua vé" else "Theo dõi"
}