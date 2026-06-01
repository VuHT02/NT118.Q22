package com.example.citymove

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.citymove.adapter.RewardAdapter
import com.example.citymove.viewmodel.AccountViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RewardsActivity : AppCompatActivity() {

    private val viewModel: AccountViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: RewardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)
        supportActionBar?.hide()

        setupUI()
        setupObservers()
        
        viewModel.loadRewards()
        loadCurrentPoints()
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        
        val rvRewards = findViewById<RecyclerView>(R.id.rvRewards)
        adapter = RewardAdapter(emptyList()) { reward ->
            handleExchange(reward)
        }
        rvRewards.layoutManager = LinearLayoutManager(this)
        rvRewards.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.rewards.observe(this) { rewards ->
            adapter.updateData(rewards)
        }
    }

    private fun loadCurrentPoints() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            if (doc != null && doc.exists()) {
                val points = doc.getLong("points") ?: 0L
                findViewById<TextView>(R.id.tvCurrentPoints).text = String.format("%,d", points).replace(",", ".")
            }
        }
    }

    private fun handleExchange(reward: com.example.citymove.data.model.RewardItem) {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val currentPoints = doc.getLong("points") ?: 0L
            if (currentPoints >= reward.points) {
                // Thực hiện đổi thưởng
                db.runTransaction { transaction ->
                    val userRef = db.collection("users").document(uid)
                    transaction.update(userRef, "points", currentPoints - reward.points)
                    // Ở đây có thể thêm logic lưu mã voucher vào kho của user
                }.addOnSuccessListener {
                    Toast.makeText(this, "Đổi thưởng thành công: ${reward.title}", Toast.LENGTH_LONG).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bạn không đủ điểm để đổi quà này", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
