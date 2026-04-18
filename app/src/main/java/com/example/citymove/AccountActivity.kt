package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.citymove.viewmodel.AccountUiState
import com.example.citymove.viewmodel.AccountViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class AccountActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val viewModel: AccountViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        supportActionBar?.hide()

        setupObservers()
        setupClickListeners()
        setupBottomNav()

        viewModel.loadProfile()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is AccountUiState.Loading -> {
                    // Show progress if needed
                }
                is AccountUiState.Success -> {
                    val profile = state.profile
                    
                    // Avatar initials
                    val initials = profile.name.trim().split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                    findViewById<TextView>(R.id.tvAvatarInitials).text = if (initials.isNotEmpty()) initials else "U"

                    // User info
                    findViewById<TextView>(R.id.tvUserName).text  = profile.name
                    findViewById<TextView>(R.id.tvUserEmail).text = auth.currentUser?.email ?: ""

                    // Rank
                    val (rankName, rankTarget) = getRank(profile.points)
                    findViewById<TextView>(R.id.tvRank).text = rankName

                    // Points progress
                    val progress = ((profile.points.toFloat() / rankTarget) * 100).toInt().coerceIn(0, 100)
                    findViewById<ProgressBar>(R.id.progressPoints).progress = progress
                    findViewById<TextView>(R.id.tvPointsProgress).text = "${formatNumber(profile.points)} / ${formatNumber(rankTarget)} điểm"
                    
                    val remaining = rankTarget - profile.points
                    findViewById<TextView>(R.id.tvPointsHint).text =
                        if (remaining > 0) "Còn ${formatNumber(remaining)} điểm để lên hạng tiếp theo"
                        else "Bạn đã đạt hạng cao nhất!"

                    findViewById<TextView>(R.id.tvPointsSub).text =
                        "${formatNumber(profile.points)} điểm · $rankName"
                }
                is AccountUiState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getRank(points: Long): Pair<String, Long> = when {
        points < 500  -> "Hạng Đồng"  to 500L
        points < 2000 -> "Hạng Bạc"   to 2000L
        points < 5000 -> "Hạng Vàng"  to 5000L
        else          -> "Hạng Kim Cương" to 10000L
    }

    private fun formatNumber(n: Long): String =
        String.format("%,d", n).replace(",", ".")

    private fun setupClickListeners() {
        findViewById<View>(R.id.menuPersonalInfo).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }

        findViewById<View>(R.id.menuChangePassword).setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        findViewById<View>(R.id.menuHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        findViewById<View>(R.id.menuRewards).setOnClickListener {
            startActivity(Intent(this, RewardsActivity::class.java))
        }

        findViewById<View>(R.id.menuLogout).setOnClickListener {
            showLogoutDialog()
        }
    }

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

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }
}
