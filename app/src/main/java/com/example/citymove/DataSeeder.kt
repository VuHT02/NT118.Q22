package com.example.citymove

import android.content.Context
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore

object DataSeeder {

    fun seedRoutes(context: Context) {
        val db = FirebaseFirestore.getInstance()
        val routes = buildRouteList()
        var done = 0
        routes.forEach { route ->
            db.collection("routes").add(route)
                .addOnSuccessListener {
                    done++
                    if (done == routes.size)
                        Toast.makeText(context, "Seed xong ${routes.size} tuyến!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi seed routes: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun seedStops(context: Context) {
        val db = FirebaseFirestore.getInstance()
        val stops = buildStopList()
        var done = 0
        stops.forEach { stop ->
            db.collection("stops").add(stop)
                .addOnSuccessListener {
                    done++
                    if (done == stops.size)
                        Toast.makeText(context, "Seed xong ${stops.size} trạm!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi seed stops: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  ROUTES
    // ─────────────────────────────────────────────────────────────
    private fun buildRouteList(): List<Map<String, Any?>> = listOf(

        // ── XE BUÝT ──────────────────────────────────────────────
        route("BUS", "01",  "Bến Thành – Chợ Lớn",
            "Bến Thành", "Ga Chợ Lớn", 8.5, 20, 35, "ACTIVE", "#F97316", 7000),
        route("BUS", "02",  "Bến Thành – Thủ Đức",
            "Bến Thành", "Bến xe Thủ Đức", 18.0, 32, 60, "ACTIVE", "#F97316", 7000),
        route("BUS", "03",  "KDC Bình Phú – Bến Thành",
            "KDC Bình Phú", "Bến Thành", 10.2, 22, 40, "ACTIVE", "#F97316", 7000),
        route("BUS", "04",  "Nguyễn Văn Cừ – Hàng Xanh",
            "Nguyễn Văn Cừ", "Hàng Xanh", 12.5, 26, 45, "ACTIVE", "#F97316", 7000),
        route("BUS", "05",  "Cầu Sài Gòn – Cầu Nhị Thiên Đường",
            "Cầu Sài Gòn", "Cầu Nhị Thiên Đường", 14.3, 28, 50, "ACTIVE", "#F97316", 7000),
        route("BUS", "06",  "ĐH Quốc Gia – Bến Thành",
            "ĐH Quốc Gia TP.HCM", "Bến Thành", 20.0, 36, 70, "ACTIVE", "#F97316", 7000),
        route("BUS", "08",  "KCN Tân Bình – Bến xe Miền Đông mới",
            "KCN Tân Bình", "Bến xe Miền Đông mới", 22.0, 38, 75, "ACTIVE", "#F97316", 7000),
        route("BUS", "09",  "Bến Thành – KCX Tân Thuận",
            "Bến Thành", "KCX Tân Thuận", 9.8, 18, 35, "ACTIVE", "#F97316", 7000),
        route("BUS", "36",  "ĐH Quốc Gia – Bến Thành (nhanh)",
            "ĐH Quốc Gia TP.HCM", "Bến Thành", 19.5, 12, 55, "ACTIVE", "#F97316", 7000),
        route("BUS", "65",  "Bến xe Miền Đông – KCX Linh Trung",
            "Bến xe Miền Đông", "KCX Linh Trung", 15.0, 24, 50, "ACTIVE", "#F97316", 7000),
        route("BUS", "93",  "Bến Thành – ĐH Quốc Gia (đêm)",
            "Bến Thành", "ĐH Quốc Gia TP.HCM", 20.0, 30, 65, "ACTIVE", "#F97316", 7000),
        route("BUS", "104", "ĐH Nông Lâm – Bến Thành",
            "ĐH Nông Lâm", "Bến Thành", 24.0, 40, 80, "ACTIVE", "#F97316", 7000),
        route("BUS", "150", "Chợ Lớn – Ngã 3 Tân Vạn",
            "Ga Chợ Lớn", "Ngã 3 Tân Vạn", 25.0, 45, 80, "ACTIVE", "#F97316", 7000),
        route("BUS", "D4",  "Phạm Văn Đồng – Bến Thành (BRT)",
            "Đại học Thể dục Thể thao", "Bến Thành", 26.0, 28, 55, "ACTIVE", "#F97316", 8000),

        // ── METRO ────────────────────────────────────────────────
        routeWithPath(
            "METRO", "M1", "Bến Thành – Suối Tiên",
            "Ga Bến Thành", "Ga Bến xe Miền Đông mới", 19.7, 14, 34, "ACTIVE", "#2563EB", 6000,
            pathPoints = listOf(
                pt(10.7720, 106.6984), // Bến Thành
                pt(10.7769, 106.7029), // Nhà hát TP
                pt(10.7884, 106.7104), // Ba Son
                pt(10.7994, 106.7215), // Văn Thánh 2
                pt(10.8017, 106.7265), // Tân Cảng
                pt(10.8037, 106.7370), // Thảo Điền
                pt(10.8029, 106.7457), // An Phú
                pt(10.8126, 106.7621), // Rạch Chiếc
                pt(10.8154, 106.7680), // Phước Long
                pt(10.8237, 106.7750), // Bình Thái
                pt(10.8510, 106.7716), // Thủ Đức
                pt(10.8569, 106.8028), // Khu CNC
                pt(10.8705, 106.8046), // ĐH Quốc Gia
                pt(10.8878, 106.7946)  // Bến xe Miền Đông mới / Suối Tiên
            )
        ),
        route("METRO", "M2", "Bến Thành – Tham Lương",
            "Bến Thành", "Tham Lương", 11.3, 9, 20, "UPCOMING", "#7C3AED", null, 2028),
        route("METRO", "M3a", "Bến Thành – Bến xe Miền Tây",
            "Bến Thành", "Bến xe Miền Tây", 19.8, 18, 36, "UPCOMING", "#7C3AED", null, 2030),
        route("METRO", "M3b", "Bến Thành – Hiệp Bình Phước",
            "Bến Thành", "Hiệp Bình Phước", 12.1, 10, 22, "UPCOMING", "#7C3AED", null, 2030),
        route("METRO", "M4", "Thạnh Xuân – Khu đô thị Himlam",
            "Thạnh Xuân", "Khu đô thị Himlam", 36.0, 20, 60, "UPCOMING", "#7C3AED", null, 2032),
        route("METRO", "M5", "Cầu Sài Gòn – Bến xe Cần Giuộc mới",
            "Cầu Sài Gòn", "Bến xe Cần Giuộc mới", 23.4, 14, 42, "UPCOMING", "#7C3AED", null, 2032),

        // ── WATER BUS ────────────────────────────────────────────
        routeWithPath(
            "WATER_BUS", "WB01", "Bạch Đằng – Linh Đông",
            "Bến Bạch Đằng", "Bến Linh Đông", 10.8, 6, 30, "ACTIVE", "#10B981", 15000,
            pathPoints = listOf(
                pt(10.7793, 106.7043), // Bến Bạch Đằng
                pt(10.7901, 106.7245), // Thủ Thiêm
                pt(10.8010, 106.7467), // Bình An
                pt(10.8145, 106.7350), // Thanh Đa
                pt(10.8278, 106.7373)  // Linh Đông
            )
        ),
        routeWithPath(
            "WATER_BUS", "WB02", "Bạch Đằng – Lò Gốm",
            "Bến Bạch Đằng", "Bến Lò Gốm", 9.6, 6, 35, "ACTIVE", "#0EA5E9", 15000,
            pathPoints = listOf(
                pt(10.7793, 106.7043), // Bạch Đằng
                pt(10.7650, 106.6870), // Kênh Tẻ
                pt(10.7520, 106.6620), // Tàu Hủ
                pt(10.7480, 106.6520)  // Lò Gốm
            )
        ),
        route("WATER_BUS", "WB03", "Bạch Đằng – Bình An",
            "Bến Bạch Đằng", "Bến Bình An", 12.0, 4, 40, "ACTIVE", "#10B981", 15000),
        route("WATER_BUS", "WB04", "Bạch Đằng – Lương Đình Của",
            "Bến Bạch Đằng", "Bến Lương Đình Của", 8.0, 4, 25, "ACTIVE", "#0EA5E9", 15000),
        route("WATER_BUS", "WB05", "Bạch Đằng – Hiệp Bình Phước",
            "Bến Bạch Đằng", "Bến Hiệp Bình Phước", 15.0, 7, 50, "ACTIVE", "#10B981", 15000),
    )

    // ─────────────────────────────────────────────────────────────
    //  STOPS  (dùng cho tính năng search → hiển thị polyline)
    // ─────────────────────────────────────────────────────────────
    private fun buildStopList(): List<Map<String, Any?>> = listOf(

        // Metro M1 – 14 ga
        stop("Ga Bến Thành",              10.7720, 106.6984, "M1", "METRO", 1),
        stop("Ga Nhà hát Thành phố",      10.7769, 106.7029, "M1", "METRO", 2),
        stop("Ga Ba Son",                  10.7884, 106.7104, "M1", "METRO", 3),
        stop("Ga Văn Thánh 2",            10.7994, 106.7215, "M1", "METRO", 4),
        stop("Ga Tân Cảng",               10.8017, 106.7265, "M1", "METRO", 5),
        stop("Ga Thảo Điền",              10.8037, 106.7370, "M1", "METRO", 6),
        stop("Ga An Phú",                 10.8029, 106.7457, "M1", "METRO", 7),
        stop("Ga Rạch Chiếc",             10.8126, 106.7621, "M1", "METRO", 8),
        stop("Ga Phước Long",             10.8154, 106.7680, "M1", "METRO", 9),
        stop("Ga Bình Thái",              10.8237, 106.7750, "M1", "METRO", 10),
        stop("Ga Thủ Đức",                10.8510, 106.7716, "M1", "METRO", 11),
        stop("Ga Khu Công nghệ Cao",      10.8569, 106.8028, "M1", "METRO", 12),
        stop("Ga Đại học Quốc gia TP.HCM",10.8705, 106.8046, "M1", "METRO", 13),
        stop("Ga Bến xe Miền Đông mới",   10.8878, 106.7946, "M1", "METRO", 14),

        // Water Bus WB01 – 5 bến
        stop("Bến Bạch Đằng",   10.7793, 106.7043, "WB01", "WATER_BUS", 1),
        stop("Bến Thủ Thiêm",   10.7901, 106.7245, "WB01", "WATER_BUS", 2),
        stop("Bến Bình An",     10.8010, 106.7467, "WB01", "WATER_BUS", 3),
        stop("Bến Thanh Đa",    10.8145, 106.7350, "WB01", "WATER_BUS", 4),
        stop("Bến Linh Đông",   10.8278, 106.7373, "WB01", "WATER_BUS", 5),

        // Water Bus WB02 – 4 bến
        stop("Bến Kênh Tẻ",    10.7650, 106.6870, "WB02", "WATER_BUS", 2),
        stop("Bến Tàu Hủ",     10.7520, 106.6620, "WB02", "WATER_BUS", 3),
        stop("Bến Lò Gốm",     10.7480, 106.6520, "WB02", "WATER_BUS", 4),

        // Xe buýt 01 – vài trạm tiêu biểu
        stop("Bến Thành",      10.7719, 106.6983, "01", "BUS", 1),
        stop("Ga Chợ Lớn",    10.7519, 106.6535, "01", "BUS", 20),
    )

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────
    private fun pt(lat: Double, lng: Double): Map<String, Double> =
        mapOf("lat" to lat, "lng" to lng)

    private fun stop(
        name: String, lat: Double, lng: Double,
        routeLineCode: String, routeType: String, sequence: Int
    ): Map<String, Any?> = mapOf(
        "name"          to name,
        "lat"           to lat,
        "lng"           to lng,
        "routeLineCode" to routeLineCode,
        "routeType"     to routeType,
        "sequence"      to sequence
    )

    private fun route(
        type: String, lineCode: String, name: String,
        startStation: String, endStation: String,
        distanceKm: Double, stationCount: Int, durationMinutes: Int,
        status: String, lineColor: String, price: Int?,
        expectedOpenYear: Int? = null
    ): Map<String, Any?> = mapOf(
        "type"             to type,
        "lineCode"         to lineCode,
        "name"             to name,
        "startStation"     to startStation,
        "endStation"       to endStation,
        "distanceKm"       to distanceKm,
        "stationCount"     to stationCount,
        "durationMinutes"  to durationMinutes,
        "status"           to status,
        "lineColor"        to lineColor,
        "price"            to price,
        "expectedOpenYear" to expectedOpenYear
    )

    private fun routeWithPath(
        type: String, lineCode: String, name: String,
        startStation: String, endStation: String,
        distanceKm: Double, stationCount: Int, durationMinutes: Int,
        status: String, lineColor: String, price: Int?,
        expectedOpenYear: Int? = null,
        pathPoints: List<Map<String, Double>> = emptyList()
    ): Map<String, Any?> = route(
        type, lineCode, name, startStation, endStation,
        distanceKm, stationCount, durationMinutes,
        status, lineColor, price, expectedOpenYear
    ) + mapOf("pathPoints" to pathPoints)
}
