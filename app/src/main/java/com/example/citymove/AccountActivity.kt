package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        supportActionBar?.hide()

        loadUserData()
        setupClickListeners()
        setupBottomNav()
    }

    // ── Load data từ Firestore ─────────────────────────────────────
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc == null || !doc.exists()) return@addOnSuccessListener

                val name   = doc.getString("name") ?: "Bạn"
                val email  = doc.getString("email") ?: auth.currentUser?.email ?: ""
                val points = doc.getLong("points") ?: 0L

                // Avatar initials
                val initials = name.trim().split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                findViewById<TextView>(R.id.tvAvatarInitials).text = initials

                // User info
                findViewById<TextView>(R.id.tvUserName).text  = name
                findViewById<TextView>(R.id.tvUserEmail).text = email

                // Rank
                val (rankName, rankTarget) = getRank(points)
                findViewById<TextView>(R.id.tvRank).text = rankName

                // Points progress
                val progress = ((points.toFloat() / rankTarget) * 100).toInt().coerceIn(0, 100)
                findViewById<ProgressBar>(R.id.progressPoints).progress = progress
                findViewById<TextView>(R.id.tvPointsProgress).text = "${formatNumber(points)} / ${formatNumber(rankTarget)} điểm"
                val remaining = rankTarget - points
                findViewById<TextView>(R.id.tvPointsHint).text =
                    if (remaining > 0) "Còn ${formatNumber(remaining)} điểm để lên hạng tiếp theo"
                    else "Bạn đã đạt hạng cao nhất!"

                // Points sub
                findViewById<TextView>(R.id.tvPointsSub).text =
                    "${formatNumber(points)} điểm · $rankName"
            }
    }

    // ── Rank helper ───────────────────────────────────────────────
    private fun getRank(points: Long): Pair<String, Long> = when {
        points < 500  -> "Hạng Đồng"  to 500L
        points < 2000 -> "Hạng Bạc"   to 2000L
        points < 5000 -> "Hạng Vàng"  to 5000L
        else          -> "Hạng Kim Cương" to 10000L
    }

    private fun formatNumber(n: Long): String =
        String.format("%,d", n).replace(",", ".")

    // ── Click listeners ───────────────────────────────────────────
    private fun setupClickListeners() {
        // Thông tin cá nhân
        findViewById<View>(R.id.menuPersonalInfo).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        // Đổi mật khẩu
        findViewById<View>(R.id.menuChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        // Lịch sử giao dịch
        findViewById<View>(R.id.menuHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Điểm thưởng
        findViewById<View>(R.id.menuRewards).setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }

        // Đăng xuất
        findViewById<View>(R.id.menuLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

    // ── Logout dialog ─────────────────────────────────────────────
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất không?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                auth.signOut()
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    // ── Bottom nav ────────────────────────────────────────────────
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_account

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home         -> { startActivity(Intent(this, HomeActivity::class.java)); true }
                R.id.nav_favorite     -> { startActivity(Intent(this, FavoriteActivity::class.java)); true }
                R.id.nav_notification -> { startActivity(Intent(this, NotificationActivity::class.java)); true }
                R.id.nav_account      -> true
                else -> false
            }
        }
    }
}