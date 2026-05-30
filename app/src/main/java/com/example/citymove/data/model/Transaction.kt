package com.example.citymove.data.model

data class Transaction(
    val id: String = "",
    val title: String = "",
    val amount: Long = 0L,
    val type: String = "",
    val timestamp: Long = 0L,
    val date: String = "",
    val routeName: String = "",
    val quantity: Int = 1,
    val ticketCode: String = "",
    val ticketType: String = "",
    val transport: String = ""
)
