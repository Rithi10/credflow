package com.example.credflow.domain.model

data class Payment(
    val id: Int = 0,
    val customerId: Int,
    val customerName: String = "",
    val customerPhone: String = "",
    val amount: Double,
    val paymentDate: String,
    val note: String = "Cash",
    val receiptNumber: String = "",
    val totalLoanAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0
) {
    fun toDisplayText(): String {
        val label = if (customerName.isNotBlank()) customerName else "Customer #$customerId"
        val receiptSuffix = if (receiptNumber.isNotBlank()) " • $receiptNumber" else ""
        return "$label • ₹${String.format("%.2f", amount)} • $note • $paymentDate$receiptSuffix"
    }
}
