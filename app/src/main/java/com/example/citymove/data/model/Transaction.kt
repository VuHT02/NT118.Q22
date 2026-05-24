package com.example.citymove.data.model

data class Transaction(
    val id: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = "",
    val timestamp: Long = 0L,
    val date: String = ""
)
