package com.example.citymove.data.model

data class Transaction(
    val id: String = "",
    val type: String = "", // "PAYMENT", "TOPUP", "REWARD"
    val title: String = "",
    val amount: Long = 0,
    val timestamp: Long = 0,
    val status: String = "SUCCESS"
)

data class RewardItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val pointsRequired: Int = 0,
    val iconResId: Int = 0
)
