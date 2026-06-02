package com.example.citymove.data.model

data class Feedback(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val routeId: String? = null,
    val routeName: String? = null,
    val rating: Float = 0f,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
