package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class NotificationActivity : AppCompatActivity() {

    // Data mẫu — sau thay bằng Firestore
    data class NotificationItem(
        val icon: String,
        val title: String,
        val message: String,
        val time: String,
        val isRead: Boolean,
        val type: String  // "system" | "trip" | "promo"
    )

    private val notifications = listOf(
        NotificationItem("🔔", "Chuyến xe sắp khởi hành", "Buýt số 01 sẽ khởi hành lúc 17:28 tại Bến Thành", "2 phút trước", false, "trip"),
        NotificationItem("🎁", "Ưu đãi hôm nay", "Giảm 20% cho chuyến đi đầu tiên trong ngày!", "15 phút trước", false, "promo"),
        NotificationItem("⚙️", "Cập nhật hệ thống", "Ứng dụng đã được cập nhật lên phiên bản mới nhất", "1 giờ trước", true, "system"),
        NotificationItem("🚌", "Chuyến đi hoàn thành", "Bạn vừa hoàn thành chuyến Bình Thái → Tham Lương", "3 giờ trước", true, "trip"),
        NotificationItem("🎁", "Điểm thưởng mới", "Bạn vừa nhận được 50 điểm thưởng từ chuyến đi hôm nay", "5 giờ trước", true, "promo"),
        NotificationItem("⚙️", "Bảo trì hệ thống", "Hệ thống sẽ bảo trì lúc 2:00 AM ngày mai", "Hôm qua", true, "system"),
        NotificationItem("🚌", "Nhắc lịch thường ngày", "Đã đến giờ di chuyển về nhà của bạn!", "Hôm qua", true, "trip"),
        NotificationItem("🎁", "Khuyến mãi cuối tuần", "Đi xe buýt miễn phí vào thứ 7 tuần này!", "2 ngày trước", true, "promo"),
    )

    private var currentTab = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        supportActionBar?.hide()

        setupHeader()
        setupTabs()
        setupBottomNav()
        renderNotifications("all")
    }

    private fun setupHeader() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvMarkAllRead).setOnClickListener {
            Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show()
            renderNotifications(currentTab)
        }
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        listOf("Tất cả", "Hệ thống", "Chuyến đi", "Khuyến mãi").forEach {
            tabLayout.addTab(tabLayout.newTab().setText(it))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = when (tab.position) {
                    0 -> "all"
                    1 -> "system"
                    2 -> "trip"
                    3 -> "promo"
                    else -> "all"
                }
                renderNotifications(currentTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun renderNotifications(type: String) {
        val container = findViewById<LinearLayout>(R.id.listNotifications)
        container.removeAllViews()

        val filtered = if (type == "all") notifications
        else notifications.filter { it.type == type }

        if (filtered.isEmpty()) {
            // Empty state
            val tv = TextView(this).apply {
                text = "Không có thông báo"
                textSize = 14f
                setTextColor(0xFF9aa5b4.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 80, 0, 0)
            }
            container.addView(tv)
            return
        }

        filtered.forEach { item ->
            container.addView(buildNotificationCard(item))
        }
    }

    private fun buildNotificationCard(item: NotificationItem): View {
        val card = CardView(this).apply {
            radius = 16f
            cardElevation = if (item.isRead) 2f else 6f
            setCardBackgroundColor(if (item.isRead) 0xFFFFFFFF.toInt() else 0xFFFFF8F3.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
            layoutParams = lp
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Icon
        val iconBg = FrameLayout(this).apply {
            val size = (48 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (12 * resources.displayMetrics.density).toInt()
            }
            background = getDrawable(R.drawable.bg_stat_card)
        }
        val iconTv = TextView(this).apply {
            text = item.icon
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        iconBg.addView(iconTv)

        // Text
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTv = TextView(this).apply {
            text = item.title
            textSize = 13f
            setTextColor(0xFF0b1928.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        val msgTv = TextView(this).apply {
            text = item.message
            textSize = 11f
            setTextColor(0xFF9aa5b4.toInt())
            setPadding(0, 4, 0, 4)
        }
        val timeTv = TextView(this).apply {
            text = item.time
            textSize = 10f
            setTextColor(if (item.isRead) 0xFF9aa5b4.toInt() else 0xFFF97316.toInt())
        }
        textCol.addView(titleTv)
        textCol.addView(msgTv)
        textCol.addView(timeTv)

        // Unread dot
        if (!item.isRead) {
            val dot = View(this).apply {
                val size = (8 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = (8 * resources.displayMetrics.density).toInt()
                }
                background = getDrawable(R.drawable.bg_notification_dot)
            }
            row.addView(iconBg)
            row.addView(textCol)
            row.addView(dot)
        } else {
            row.addView(iconBg)
            row.addView(textCol)
        }

        card.addView(row)
        return card
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_notification

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java)); finish(); true
                }
                R.id.nav_favorite -> {
                    startActivity(Intent(this, FavoriteActivity::class.java)); finish(); true
                }
                R.id.nav_notification -> true
                R.id.nav_account -> {
                    startActivity(Intent(this, AccountActivity::class.java)); finish(); true
                }
                else -> false
            }
        }
    }
}