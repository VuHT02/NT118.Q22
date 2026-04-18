package com.example.citymove

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.RewardAdapter
import com.example.citymove.data.model.RewardItem
import com.example.citymove.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RewardsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val viewModel: AccountViewModel by viewModels()
    
    private lateinit var adapter: RewardAdapter
    private lateinit var tvCurrentPoints: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        tvCurrentPoints = findViewById(R.id.tvCurrentPoints)
        
        val rvRewards = findViewById<RecyclerView>(R.id.rvRewards)
        rvRewards.layoutManager = LinearLayoutManager(this)
        
        adapter = RewardAdapter(emptyList()) { reward ->
            redeemReward(reward)
        }
        rvRewards.adapter = adapter

        setupObservers()
        
        viewModel.loadProfile()
        viewModel.loadRewards()
    }

    private fun setupObservers() {
        viewModel.uiState.observe(this) { state ->
            // Update points from profile state
            if (state is com.example.citymove.viewmodel.AccountUiState.Success) {
                tvCurrentPoints.text = String.format("%,d", state.profile.points).replace(",", ".")
            }
        }

        viewModel.rewards.observe(this) { list ->
            adapter.updateData(list)
        }
    }

    private fun redeemReward(reward: RewardItem) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val currentPoints = doc.getLong("points") ?: 0L
            if (currentPoints >= reward.pointsRequired) {
                val newPoints = currentPoints - reward.pointsRequired
                db.collection("users").document(uid).update("points", newPoints)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đổi thành công: ${reward.title}", Toast.LENGTH_SHORT).show()
                        viewModel.loadProfile() // Refresh points
                    }
            } else {
                Toast.makeText(this, "Không đủ điểm", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
