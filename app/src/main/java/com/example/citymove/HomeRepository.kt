package com.example.citymove.data.repository

import com.example.citymove.data.model.HomeData
import com.example.citymove.data.model.UserProfile
import com.example.citymove.data.model.WeeklyStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class HomeRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    suspend fun getHomeData(): Result<HomeData> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("Chưa đăng nhập"))

            // Fetch song song cả 2 document
            val userDoc  = db.collection("users").document(uid).get().await()
            val statsDoc = db.collection("users").document(uid)
                .collection("weeklyStats").document("current").get().await()

            val profile = UserProfile(
                name         = userDoc.getString("name")
                    ?: userDoc.getString("email")
                    ?: "Bạn",
                balance      = userDoc.getLong("balance") ?: 0L,
                monthlySpend = userDoc.getLong("monthlySpend") ?: 0L,
                monthlyTrips = userDoc.getLong("monthlyTrips") ?: 0L,
                co2Saved     = userDoc.getDouble("co2Saved") ?: 0.0,
                points       = userDoc.getLong("points") ?: 0L,
                todayTrips   = userDoc.getLong("todayTrips") ?: 0L,
            )

            val weekly = WeeklyStats(
                trips = statsDoc.getLong("trips") ?: 0L,
                cost  = statsDoc.getLong("cost") ?: 0L,
                co2   = statsDoc.getDouble("co2") ?: 0.0,
            )

            Result.success(HomeData(profile, weekly))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
