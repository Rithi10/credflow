package com.example.credflow.domain.model

import kotlin.math.floor

data class Customer(
    val id: Int = 0,
    val name: String,
    val phone: String,
    val amount: Double, // Principal Loan Amount
    val interest: Double = 0.0, // Interest Rate %
    val interestType: String = "Flat Interest",
    val period: Int = 1,
    val periodUnit: String = "Months",
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 1,
    val firstDueDate: String = "",
    val finalDueDate: String = "",
    val cycle: String,
    val day: String,
    val totalDue: Double = amount,
    val paid: Double = 0.0,
    val due: Double = (totalDue - paid).coerceAtLeast(0.0),
    val note: String = "",
    val createdAt: String = ""
) {
    val totalInterest: Double
        get() = (totalDue - amount).coerceAtLeast(0.0)

    val remainingInstallments: Int
        get() {
            if (due <= 0.001) return 0
            val single = when {
                installmentAmount > 0.0 -> installmentAmount
                totalInstallments > 0 -> totalDue / totalInstallments
                else -> due
            }
            if (single <= 0.0) return 0
            val paidCount = floor((paid + 0.999) / single).toInt()
            return (totalInstallments - paidCount).coerceIn(0, totalInstallments)
        }
}
