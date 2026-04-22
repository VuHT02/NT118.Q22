package com.example.citymove

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class NotificationItem(
    val id          : String  = "",
    val title       : String  = "",
    val description : String  = "",
    val type        : String  = "system",   // "trip" | "promo" | "system" | "reward"
    val isRead      : Boolean = false,
    val timestamp   : Long    = 0L,
)

class NotificationActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private var allNotifications = listOf<NotificationItem>()
    private var currentTab = 0  // 0=All 1=Trip 2=Promo 3=System

    private val tabTypes = listOf(null, "trip", "promo", "system")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)
        supportActionBar?.hide()

        setupTabs()
        setupClickListeners()
        loadNotifications()
    }

    // ── Tabs ──────────────────────────────────────────────────────
    private fun setupTabs() {
        val tabs = listOf(
            findViewById<TextView>(R.id.tabAll),
            findViewById<TextView>(R.id.tabTrip),
            findViewById<TextView>(R.id.tabPromo),
            findViewById<TextView>(R.id.tabSystem)
        )

        tabs.forEachIndexed { index, textView ->
            textView?.setOnClickListener {
                currentTab = index
                updateTabUI(tabs)
                renderList(filterByTab(allNotifications))
            }
        }
        updateTabUI(tabs)
    }

    private fun updateTabUI(tabs: List<TextView?>) {
        tabs.forEachIndexed { index, textView ->
            val isSelected = (index == currentTab)
            textView?.let {
                it.setBackgroundResource(if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
                it.setTextColor(if (isSelected) Color.WHITE else "#6B7280".toColorInt())
                it.setTypeface(null, if (isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }
        }
    }

    // ── Load từ Firestore ─────────────────────────────────────────
    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                allNotifications = snap.documents.map { doc ->
                    NotificationItem(
                        id          = doc.id,
                        title       = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        type        = doc.getString("type") ?: "system",
                        isRead      = doc.getBoolean("isRead") ?: false,
                        timestamp   = doc.getLong("timestamp") ?: 0L,
                    )
                }
                updateUnreadBadge()
                renderList(filterByTab(allNotifications))
            }
    }

    // ── Filter ────────────────────────────────────────────────────
    private fun filterByTab(list: List<NotificationItem>): List<NotificationItem> {
        val type = tabTypes[currentTab] ?: return list
        return list.filter { it.type == type }
    }

    // ── Render ────────────────────────────────────────────────────
    private fun renderList(items: List<NotificationItem>) {
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val today     = items.filter { now - it.timestamp < dayMs }
        val yesterday = items.filter { it.timestamp in (now - 2 * dayMs)..(now - dayMs) }
        val older     = items.filter { now - it.timestamp >= 2 * dayMs }

        val listToday     = findViewById<LinearLayout>(R.id.listToday)
        val listYesterday = findViewById<LinearLayout>(R.id.listYesterday)
        val listOlder     = findViewById<LinearLayout>(R.id.listOlder)
        val labelToday    = findViewById<TextView>(R.id.labelToday)
        val labelYesterday= findViewById<TextView>(R.id.labelYesterday)
        val labelOlder    = findViewById<TextView>(R.id.labelOlder)
        val layoutEmpty   = findViewById<LinearLayout>(R.id.layoutEmpty)

        listToday.removeAllViews()
        listYesterday.removeAllViews()
        listOlder.removeAllViews()

        labelToday.visibility     = if (today.isNotEmpty()) View.VISIBLE else View.GONE
        labelYesterday.visibility = if (yesterday.isNotEmpty()) View.VISIBLE else View.GONE
        labelOlder.visibility     = if (older.isNotEmpty()) View.VISIBLE else View.GONE
        layoutEmpty.visibility    = if (items.isEmpty()) View.VISIBLE else View.GONE

        today.forEach     { addNotifCard(listToday, it) }
        yesterday.forEach { addNotifCard(listYesterday, it) }
        older.forEach     { addNotifCard(listOlder, it) }
    }

    // ── Inflate card ──────────────────────────────────────────────
    private fun addNotifCard(container: LinearLayout, item: NotificationItem) {
        val view = layoutInflater.inflate(R.layout.item_notification, container, false)

        view.findViewById<TextView>(R.id.tvTitle).text       = item.title
        view.findViewById<TextView>(R.id.tvDescription).text = item.description
        view.findViewById<TextView>(R.id.tvTime).text        = formatTime(item.timestamp)

        // Unread state
        if (!item.isRead) {
            view.findViewById<View>(R.id.unreadDot).visibility = View.VISIBLE
            view.findViewById<View>(R.id.unreadBar).visibility = View.VISIBLE
        }

        // Icon theo type
        val iconView = view.findViewById<android.widget.ImageView>(R.id.ivIcon)
        val iconBg   = view.findViewById<android.widget.FrameLayout>(R.id.iconContainer)
        when (item.type) {
            "trip"   -> {
                iconView.setImageResource(R.drawable.ic_transgo_logo)
                iconBg.setBackgroundResource(R.drawable.bg_transport_icon_orange)
            }
            "promo"  -> {
                iconView.setImageResource(R.drawable.ic_star)
                iconBg.setBackgroundResource(R.drawable.bg_transport_icon_blue)
            }
            "reward" -> {
                iconView.setImageResource(R.drawable.ic_star)
                iconBg.setBackgroundResource(R.drawable.bg_icon_circle_orange)
            }
            else     -> {
                iconView.setImageResource(R.drawable.ic_bell)
                iconBg.setBackgroundResource(R.drawable.bg_transport_icon_teal)
            }
        }

        // Click → đánh dấu đã đọc
        view.setOnClickListener {
            markAsRead(item.id)
            view.findViewById<View>(R.id.unreadDot).visibility = View.GONE
            view.findViewById<View>(R.id.unreadBar).visibility = View.GONE
        }

        container.addView(view)
    }

    // ── Mark as read ──────────────────────────────────────────────
    private fun markAsRead(notifId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("notifications").document(notifId)
            .update("isRead", true)

        allNotifications = allNotifications.map {
            if (it.id == notifId) it.copy(isRead = true) else it
        }
        updateUnreadBadge()
    }

    private fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        val batch = db.batch()
        allNotifications.filter { !it.isRead }.forEach {
            val ref = db.collection("users").document(uid)
                .collection("notifications").document(it.id)
            batch.update(ref, "isRead", true)
        }
        batch.commit()
        allNotifications = allNotifications.map { it.copy(isRead = true) }
        updateUnreadBadge()
        renderList(filterByTab(allNotifications))
    }

    // ── Unread badge ─────────────────────────────────────────────
    private fun updateUnreadBadge() {
        val count = allNotifications.count { !it.isRead }
        val badge = findViewById<TextView>(R.id.tvUnreadCount)
        if (count > 0) {
            badge.text = "$count"
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    // ── Format time ───────────────────────────────────────────────
    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000       -> "Vừa xong"
            diff < 3_600_000    -> "${diff / 60_000} phút trước"
            diff < 86_400_000   -> "${diff / 3_600_000} giờ trước"
            diff < 172_800_000  -> "Hôm qua"
            else -> {
                val d = java.util.Date(timestamp)
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(d)
            }
        }
    }

    // ── Click listeners ───────────────────────────────────────────
    private fun setupClickListeners() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvMarkAllRead).setOnClickListener { markAllRead() }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_notification
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home         -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_favorite     -> { startActivity(Intent(this, FavoriteActivity::class.java)); true }
                R.id.nav_notification -> true
                R.id.nav_account      -> { startActivity(Intent(this, AccountActivity::class.java)); true }
                else -> false
            }
        }
    }
}
