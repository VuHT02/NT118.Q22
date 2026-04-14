package com.example.citymove.data.model

data class UserProfile(
    val name         : String,
    val balance      : Long,
    val monthlySpend : Long,
    val monthlyTrips : Long,
    val co2Saved     : Double,
    val points       : Long,
    val todayTrips   : Long,
)

data class WeeklyStats(
    val trips : Long,
    val cost  : Long,
    val co2   : Double,
)

data class HomeData(
    val profile : UserProfile,
    val weekly  : WeeklyStats,
)
