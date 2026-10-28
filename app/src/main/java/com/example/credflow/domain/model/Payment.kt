package com.example.credflow.domain.model

data class Payment(
    val id: Int = 0,
    val customerId: Int,
    val customerName: String = "",
    val amount: Double,
    val paymentDate: String,
    val note: String = "Cash"
) {
    fun toDisplayText(): String =
        "${if (customerName.isNotBlank()) customerName else "Customer #$customerId"} • ₹${String.format("%.2f", amount)} • $note • $paymentDate"
}

