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
                    if (done == routes.size) {
                        Toast.makeText(context, "Seed xong ${routes.size} tuyến!", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun buildRouteList(): List<Map<String, Any?>> = listOf(

        // ════════════════════════════════════════
        //  XE BUÝT
        // ════════════════════════════════════════
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

        // ════════════════════════════════════════
        //  METRO
        // ════════════════════════════════════════
        route("METRO", "M1", "Bến Thành – Suối Tiên",
            "Bến Thành", "Suối Tiên", 19.7, 14, 34, "ACTIVE", "#2563EB", 6000),

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

        // ════════════════════════════════════════
        //  WATER BUS
        // ════════════════════════════════════════
        route("WATER_BUS", "WB01", "Bạch Đằng – Linh Đông",
            "Bến Bạch Đằng", "Bến Linh Đông", 10.8, 6, 30, "ACTIVE", "#10B981", 15000),

        route("WATER_BUS", "WB02", "Bạch Đằng – Lò Gốm",
            "Bến Bạch Đằng", "Bến Lò Gốm", 9.6, 6, 35, "ACTIVE", "#0EA5E9", 15000),

        route("WATER_BUS", "WB03", "Bạch Đằng – Bình An",
            "Bến Bạch Đằng", "Bến Bình An", 12.0, 4, 40, "ACTIVE", "#10B981", 15000),

        route("WATER_BUS", "WB04", "Bạch Đằng – Lương Đình Của",
            "Bến Bạch Đằng", "Bến Lương Đình Của", 8.0, 4, 25, "ACTIVE", "#0EA5E9", 15000),

        route("WATER_BUS", "WB05", "Bạch Đằng – Hiệp Bình Phước",
            "Bến Bạch Đằng", "Bến Hiệp Bình Phước", 15.0, 7, 50, "ACTIVE", "#10B981", 15000),
    )

    private fun route(
        type: String,
        lineCode: String,
        name: String,
        startStation: String,
        endStation: String,
        distanceKm: Double,
        stationCount: Int,
        durationMinutes: Int,
        status: String,
        lineColor: String,
        price: Int?,
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
}
