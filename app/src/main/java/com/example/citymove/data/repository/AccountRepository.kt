package com.example.citymove.data.repository

import com.example.citymove.data.model.Feedback
import com.example.citymove.data.model.RewardItem
import com.example.citymove.data.model.Transaction
import com.example.citymove.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AccountRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(): Result<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Chưa đăng nhập"))

        return try {
            currentUser.getIdToken(true).await()
            val userRef = db.collection("users").document(currentUser.uid)
            val doc = userRef.get().await()

            val nameFromAuth = currentUser.displayName?.takeIf { it.isNotEmpty() }
                ?: currentUser.email?.substringBefore("@")
                ?: "User"

            if (doc.exists()) {
                var points = doc.getLong("points") ?: 0L
                var monthlySpend = doc.getLong("monthlySpend") ?: 0L
                
                // ─── LOGIC FIX ĐIỂM SÂU: QUÉT GIAO DỊCH ───
                // Nếu điểm bằng 0, ta kiểm tra danh sách giao dịch để tính lại
                if (points == 0L) {
                    val transSnapshot = userRef.collection("transactions")
                        .whereEqualTo("type", "PAYMENT")
                        .get().await()
                    
                    if (!transSnapshot.isEmpty) {
                        var totalPaid = 0L
                        for (transDoc in transSnapshot.documents) {
                            val amt = transDoc.getLong("amount") ?: 0L
                            totalPaid += Math.abs(amt) // Lấy giá trị dương của số tiền thanh toán
                        }
                        
                        if (totalPaid > 0) {
                            monthlySpend = totalPaid
                            points = totalPaid / 1000
                            
                            // Cập nhật lại database để đồng bộ
                            val updates = mapOf(
                                "monthlySpend" to monthlySpend,
                                "points" to points
                            )
                            userRef.update(updates)
                        }
                    }
                }

                Result.success(UserProfile(
                    name         = doc.getString("name")?.takeIf { it.isNotEmpty() } ?: nameFromAuth,
                    balance      = doc.getLong("balance")      ?: 0L,
                    monthlySpend = monthlySpend,
                    monthlyTrips = doc.getLong("monthlyTrips") ?: 0L,
                    co2Saved     = doc.getDouble("co2Saved")   ?: 0.0,
                    points       = points,
                    todayTrips   = doc.getLong("todayTrips")   ?: 0L
                ))
            } else {
                Result.success(UserProfile(name = nameFromAuth, balance = 0L, monthlySpend = 0L, monthlyTrips = 0L, co2Saved = 0.0, points = 0L, todayTrips = 0L))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactions(): Result<List<Transaction>> {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Chưa đăng nhập"))
        return try {
            currentUser.getIdToken(true).await()
            val docs = db.collection("users").document(currentUser.uid)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get().await()
            val list = docs.mapNotNull { doc ->
                doc.toObject(Transaction::class.java).copy(
                    id = doc.id,
                    isUsed = doc.getBoolean("isUsed") ?: false,
                    expiryDate = doc.getLong("expiryDate") ?: 0L
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableRewards(): Result<List<RewardItem>> {
        return try {
            val docs = db.collection("rewards").get().await()
            if (docs.isEmpty) {
                Result.success(listOf(
                    RewardItem("1", "Giảm 50% vé xe buýt", "Áp dụng cho mọi tuyến xe", 100),
                    RewardItem("2", "Miễn phí 1 chuyến đi", "Tối đa 10.000đ", 200),
                    RewardItem("3", "Voucher Highlands 20k", "Cho hóa đơn từ 50k", 500)
                ))
            } else {
                Result.success(docs.map { it.toObject(RewardItem::class.java).copy(id = it.id) })
            }
        } catch (_: Exception) {
            Result.success(listOf(
                RewardItem("1", "Giảm 50% vé xe buýt", "Áp dụng cho mọi tuyến xe", 100),
                RewardItem("2", "Miễn phí 1 chuyến đi", "Tối đa 10.000đ", 200),
                RewardItem("3", "Voucher Highlands 20k", "Cho hóa đơn từ 50k", 500)
            ))
        }
    }

    suspend fun sendFeedback(feedback: Feedback): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Chưa đăng nhập"))
        return try {
            val feedbackData = feedback.copy(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
            )
            db.collection("feedback").add(feedbackData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
