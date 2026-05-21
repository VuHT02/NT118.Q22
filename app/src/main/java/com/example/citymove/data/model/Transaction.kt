package com.example.citymove.data.model

data class Transaction(
    val id: String,
    val title: String,
    val amount: Long,      // Đổi sang Long để khớp với formatCurrency
    val type: String,      // Thêm trường này ("PAYMENT" hoặc "TOPUP")
    val timestamp: Long,   // Thêm trường này để hiển thị ngày tháng
    val date: String = ""  // Giữ lại nếu cần
)