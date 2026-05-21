package com.example.citymove.data.repository

import com.example.citymove.data.model.RewardItem
import com.example.citymove.data.model.Transaction
import com.example.citymove.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AccountRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    suspend fun getUserProfile(): Result<UserProfile> {
        val currentUser = auth.currentUser
            ?: return Result.failure(Exception("Chưa đăng nhập"))

        return try {
            // Force-refresh token để tránh lỗi token hết hạn
            currentUser.getIdToken(true).await()

            val doc = db.collection("users").document(currentUser.uid).get().await()

            val nameFromAuth = currentUser.displayName?.takeIf { it.isNotEmpty() }
                ?: currentUser.email?.substringBefore("@")
                ?: "User"

            if (doc.exists()) {
                Result.success(UserProfile(
                    name         = doc.getString("name")?.takeIf { it.isNotEmpty() } ?: nameFromAuth,
                    balance      = doc.getLong("balance")      ?: 0L,
                    monthlySpend = doc.getLong("monthlySpend") ?: 0L,
                    monthlyTrips = doc.getLong("monthlyTrips") ?: 0L,
                    co2Saved     = doc.getDouble("co2Saved")   ?: 0.0,
                    points       = doc.getLong("points")       ?: 0L,
                    todayTrips   = doc.getLong("todayTrips")   ?: 0L
                ))
            } else {
                // Document chưa có → fallback từ Firebase Auth
                Result.success(UserProfile(
                    name         = nameFromAuth,
                    balance      = 0L,
                    monthlySpend = 0L,
                    monthlyTrips = 0L,
                    co2Saved     = 0.0,
                    points       = 0L,
                    todayTrips   = 0L
                ))
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                // Firestore rules chặn → hiển thị thông tin cơ bản từ Firebase Auth
                val nameFromAuth = currentUser.displayName?.takeIf { it.isNotEmpty() }
                    ?: currentUser.email?.substringBefore("@")
                    ?: "User"
                Result.success(UserProfile(
                    name         = nameFromAuth,
                    balance      = 0L,
                    monthlySpend = 0L,
                    monthlyTrips = 0L,
                    co2Saved     = 0.0,
                    points       = 0L,
                    todayTrips   = 0L
                ))
            } else {
                Result.failure(e)
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
            val list = docs.map { it.toObject(Transaction::class.java).copy(id = it.id) }
            Result.success(list)
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                Result.success(emptyList())
            else
                Result.failure(e)
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
        } catch (e: Exception) {
            Result.success(listOf(
                RewardItem("1", "Giảm 50% vé xe buýt", "Áp dụng cho mọi tuyến xe", 100),
                RewardItem("2", "Miễn phí 1 chuyến đi", "Tối đa 10.000đ", 200),
                RewardItem("3", "Voucher Highlands 20k", "Cho hóa đơn từ 50k", 500)
            ))
        }
    }
}
