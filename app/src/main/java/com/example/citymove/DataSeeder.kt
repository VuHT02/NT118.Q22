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
        route("BUS", "10",  "ĐH Quốc Gia – Bến xe Miền Tây",
            "ĐH Quốc Gia TP.HCM", "Bến xe Miền Tây", 24.5, 42, 80, "ACTIVE", "#F97316", 7000),
        route("BUS", "14",  "Bến xe Miền Đông – Bến xe Miền Tây",
            "Bến xe Miền Đông", "Bến xe Miền Tây", 16.2, 32, 55, "ACTIVE", "#F97316", 7000),
        route("BUS", "19",  "Bến Thành – KCX Linh Trung – ĐH Quốc Gia",
            "Bến Thành", "ĐH Quốc Gia TP.HCM", 21.8, 45, 75, "ACTIVE", "#F97316", 7000),
        route("BUS", "20",  "Bến Thành – Nhà Bè",
            "Bến Thành", "Nhà Bè", 14.8, 30, 55, "ACTIVE", "#F97316", 7000),
        route("BUS", "27",  "Bến xe An Sương – Bến Thành",
            "Bến xe An Sương", "Bến Thành", 15.5, 28, 50, "ACTIVE", "#F97316", 7000),
        route("BUS", "30",  "Chợ Tân Hương – ĐH Quốc Gia",
            "Chợ Tân Hương", "ĐH Quốc Gia TP.HCM", 26.5, 48, 85, "ACTIVE", "#F97316", 7000),
        route("BUS", "31",  "ĐH Văn Lang – ĐH Tôn Đức Thắng",
            "ĐH Văn Lang", "ĐH Tôn Đức Thắng", 19.2, 38, 65, "ACTIVE", "#F97316", 7000),
        route("BUS", "36",  "ĐH Quốc Gia – Bến Thành (nhanh)",
            "ĐH Quốc Gia TP.HCM", "Bến Thành", 19.5, 12, 55, "ACTIVE", "#F97316", 7000),
        route("BUS", "45",  "Bến xe Quận 8 – Bến Thành – Bến xe Miền Đông",
            "Bến xe Quận 8", "Bến xe Miền Đông", 16.8, 34, 60, "ACTIVE", "#F97316", 7000),
        route("BUS", "53",  "Lê Hồng Phong – ĐH Quốc Gia",
            "Lê Hồng Phong", "ĐH Quốc Gia TP.HCM", 23.2, 40, 75, "ACTIVE", "#F97316", 7000),
        route("BUS", "56",  "Chợ Lớn – ĐH Giao thông Vận tải",
            "Ga Chợ Lớn", "ĐH Giao thông Vận tải", 18.5, 35, 60, "ACTIVE", "#F97316", 7000),
        route("BUS", "65",  "Bến xe Miền Đông – KCX Linh Trung",
            "Bến xe Miền Đông", "KCX Linh Trung", 15.0, 24, 50, "ACTIVE", "#F97316", 7000),
        route("BUS", "72",  "Công viên 23/9 – Hiệp Phước",
            "Công viên 23/9", "Hiệp Phước", 28.0, 45, 90, "ACTIVE", "#F97316", 7000),
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
                pt(10.7720, 106.6984), pt(10.7769, 106.7029), pt(10.7884, 106.7104),
                pt(10.7994, 106.7215), pt(10.8017, 106.7265), pt(10.8037, 106.7370),
                pt(10.8029, 106.7457), pt(10.8126, 106.7621), pt(10.8154, 106.7680),
                pt(10.8237, 106.7750), pt(10.8510, 106.7716), pt(10.8569, 106.8028),
                pt(10.8705, 106.8046), pt(10.8878, 106.7946)
            )
        ),
        route("METRO", "M2", "Bến Thành – Tham Lương",
            "Bến Thành", "Tham Lương", 11.3, 9, 20, "UPCOMING", "#7C3AED", null, 2028),
        route("METRO", "M3a", "Bến Thành – Bến xe Miền Tây",
            "Bến Thành", "Bến xe Miền Tây", 19.8, 18, 36, "UPCOMING", "#7C3AED", null, 2030),

        // ── WATER BUS ────────────────────────────────────────────
        routeWithPath(
            "WATER_BUS", "WB01", "Bạch Đằng – Linh Đông",
            "Bến Bạch Đằng", "Bến Linh Đông", 10.8, 6, 30, "ACTIVE", "#10B981", 15000,
            pathPoints = listOf(
                pt(10.7793, 106.7043), pt(10.7901, 106.7245), pt(10.8010, 106.7467),
                pt(10.8145, 106.7350), pt(10.8278, 106.7373)
            )
        ),
        routeWithPath(
            "WATER_BUS", "WB02", "Bạch Đằng – Lò Gốm",
            "Bến Bạch Đằng", "Bến Lò Gốm", 9.6, 6, 35, "ACTIVE", "#0EA5E9", 15000,
            pathPoints = listOf(
                pt(10.7793, 106.7043), pt(10.7650, 106.6870), pt(10.7520, 106.6620), pt(10.7480, 106.6520)
            )
        ),
    )

    // ─────────────────────────────────────────────────────────────
    //  STOPS
    // ─────────────────────────────────────────────────────────────
    private fun buildStopList(): List<Map<String, Any?>> = listOf(
        // Metro M1
        stop("Ga Bến Thành",              10.7720, 106.6984, "M1", "METRO", 1),
        stop("Ga Nhà hát Thành phố",      10.7769, 106.7029, "M1", "METRO", 2),
        stop("Ga Ba Son",                  10.7884, 106.7104, "M1", "METRO", 3),
        stop("Ga Tân Cảng",               10.8017, 106.7265, "M1", "METRO", 5),
        stop("Ga Thảo Điền",              10.8037, 106.7370, "M1", "METRO", 6),
        stop("Ga Bình Thái",              10.8237, 106.7750, "M1", "METRO", 10),
        stop("Ga Thủ Đức",                10.8510, 106.7716, "M1", "METRO", 11),
        stop("Ga Khu Công nghệ Cao",      10.8569, 106.8028, "M1", "METRO", 12),
        stop("Ga Đại học Quốc gia",       10.8705, 106.8046, "M1", "METRO", 13),
        stop("Ga Bến xe Miền Đông mới",   10.8878, 106.7946, "M1", "METRO", 14),

        // Water Bus WB01
        stop("Bến Bạch Đằng",   10.7793, 106.7043, "WB01", "WATER_BUS", 1),
        stop("Bến Thủ Thiêm",   10.7901, 106.7245, "WB01", "WATER_BUS", 2),
        stop("Bến Bình An",     10.8010, 106.7467, "WB01", "WATER_BUS", 3),
        stop("Bến Thanh Đa",    10.8145, 106.7350, "WB01", "WATER_BUS", 4),
        stop("Bến Linh Đông",   10.8278, 106.7373, "WB01", "WATER_BUS", 5),

        // Bus 01
        stop("Bến Thành",      10.7719, 106.6983, "01", "BUS", 1),
        stop("Hàm Nghi",       10.7705, 106.7005, "01", "BUS", 2),
        stop("Trần Hưng Đạo",  10.7600, 106.6850, "01", "BUS", 10),
        stop("Ga Chợ Lớn",     10.7519, 106.6535, "01", "BUS", 20),

        // Bus 19
        stop("Bến Thành",      10.7719, 106.6983, "19", "BUS", 1),
        stop("Đinh Tiên Hoàng",10.7850, 106.6950, "19", "BUS", 5),
        stop("Hàng Xanh",      10.8015, 106.7115, "19", "BUS", 12),
        stop("Ngã tư Thủ Đức", 10.8465, 106.7750, "19", "BUS", 30),
        stop("ĐH Quốc Gia",    10.8715, 106.8025, "19", "BUS", 45),

        // Bus 150
        stop("Ga Chợ Lớn",     10.7519, 106.6535, "150", "BUS", 1),
        stop("Hùng Vương",     10.7580, 106.6650, "150", "BUS", 5),
        stop("Điện Biên Phủ",  10.7950, 106.7050, "150", "BUS", 15),
        stop("Suối Tiên",      10.8660, 106.8040, "150", "BUS", 40),
        stop("Ngã 3 Tân Vạn",  10.8950, 106.8250, "150", "BUS", 45),

        // Thêm trạm cho Bus 10
        stop("Bến xe Miền Tây", 10.7523, 106.6200, "10", "BUS", 42),
        stop("Kinh Dương Vương", 10.7480, 106.6350, "10", "BUS", 35),
        
        // Thêm trạm cho Bus 27
        stop("Bến xe An Sương", 10.8510, 106.6210, "27", "BUS", 1),
        stop("Trường Chinh",   10.8250, 106.6450, "27", "BUS", 10),
        stop("Lý Thường Kiệt", 10.7750, 106.6600, "27", "BUS", 20),
    )

    // ─────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────
    private fun pt(lat: Double, lng: Double): Map<String, Double> = mapOf("lat" to lat, "lng" to lng)

    private fun stop(name: String, lat: Double, lng: Double, routeLineCode: String, routeType: String, sequence: Int): Map<String, Any?> = mapOf(
        "name" to name, "lat" to lat, "lng" to lng, "routeLineCode" to routeLineCode, "routeType" to routeType, "sequence" to sequence
    )

    private fun route(type: String, lineCode: String, name: String, startStation: String, endStation: String, distanceKm: Double, stationCount: Int, durationMinutes: Int, status: String, lineColor: String, price: Int?, expectedOpenYear: Int? = null): Map<String, Any?> = mapOf(
        "type" to type, "lineCode" to lineCode, "name" to name, "startStation" to startStation, "endStation" to endStation, "distanceKm" to distanceKm, "stationCount" to stationCount, "durationMinutes" to durationMinutes, "status" to status, "lineColor" to lineColor, "price" to price, "expectedOpenYear" to expectedOpenYear
    )

    private fun routeWithPath(type: String, lineCode: String, name: String, startStation: String, endStation: String, distanceKm: Double, stationCount: Int, durationMinutes: Int, status: String, lineColor: String, price: Int?, expectedOpenYear: Int? = null, pathPoints: List<Map<String, Double>> = emptyList()): Map<String, Any?> = 
        route(type, lineCode, name, startStation, endStation, distanceKm, stationCount, durationMinutes, status, lineColor, price, expectedOpenYear) + mapOf("pathPoints" to pathPoints)
}
