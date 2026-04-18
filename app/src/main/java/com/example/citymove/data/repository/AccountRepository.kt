package com.example.citymove.data.repository

import com.example.citymove.data.model.RewardItem
import com.example.citymove.data.model.Transaction
import com.example.citymove.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AccountRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                Result.success(UserProfile(
                    name = doc.getString("name") ?: "User",
                    balance = doc.getLong("balance") ?: 0L,
                    monthlySpend = doc.getLong("monthlySpend") ?: 0L,
                    monthlyTrips = doc.getLong("monthlyTrips") ?: 0L,
                    co2Saved = doc.getDouble("co2Saved") ?: 0.0,
                    points = doc.getLong("points") ?: 0L,
                    todayTrips = doc.getLong("todayTrips") ?: 0L
                ))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactions(): Result<List<Transaction>> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
            val docs = db.collection("users").document(uid).collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            val list = docs.map { it.toObject(Transaction::class.java).copy(id = it.id) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableRewards(): Result<List<RewardItem>> {
        // Normally fetch from Firestore, here we can mock some or fetch from a global 'rewards' collection
        return try {
            val docs = db.collection("rewards").get().await()
            if (docs.isEmpty) {
                // Return default/mock if empty
                Result.success(listOf(
                    RewardItem("1", "Giảm 50% vé xe buýt", "Áp dụng cho mọi tuyến xe", 100),
                    RewardItem("2", "Miễn phí 1 chuyến đi", "Tối đa 10.000đ", 200),
                    RewardItem("3", "Voucher Highlands 20k", "Cho hóa đơn từ 50k", 500)
                ))
            } else {
                Result.success(docs.map { it.toObject(RewardItem::class.java).copy(id = it.id) })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
