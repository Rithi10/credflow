package com.example.credflow.domain.model

import com.example.credflow.util.ScheduleUtils
import kotlin.math.floor
import kotlin.math.min

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
    val createdAt: String = "",
    val loanStatus: String = "ACTIVE",
    val closedAt: String = "",
    val settlementAmount: Double = 0.0,
    val settlementType: String = "",
    val closureNote: String = "",
    val statusLabel: String = "",
    val nextDueDateLabel: String = "",
    val overdueDays: Int = 0
) {
    val isClosed: Boolean
        get() = loanStatus.equals("CLOSED", ignoreCase = true)

    val totalInterest: Double
        get() = (totalDue - amount).coerceAtLeast(0.0)

    val outstandingPrincipal: Double
        get() {
            if (isClosed || due <= 0.001) return 0.0
            val paidPrincipal = min(amount, paid)
            return (amount - paidPrincipal).coerceAtLeast(0.0)
        }

    val outstandingInterest: Double
        get() {
            if (isClosed || due <= 0.001) return 0.0
            return (due - outstandingPrincipal).coerceAtLeast(0.0)
        }

    val remainingInstallments: Int
        get() {
            if (isClosed || due <= 0.001) return 0
            val single = when {
                installmentAmount > 0.0 -> installmentAmount
                totalInstallments > 0 -> totalDue / totalInstallments
                else -> due
            }
            if (single <= 0.0) return 0
            val paidCount = floor((paid + 0.999) / single).toInt()
            return (totalInstallments - paidCount).coerceIn(0, totalInstallments)
        }

    val monthlyDueDay: Int
        get() = if (cycle.equals("Monthly", ignoreCase = true)) {
            ScheduleUtils.extractMonthlyDueDay(day, firstDueDate)
        } else 0

    val scheduleNeedsCorrection: Boolean
        get() = ScheduleUtils.needsScheduleCorrection(cycle, day, firstDueDate)

    val scheduleDisplayLabel: String
        get() = ScheduleUtils.displayScheduleLabel(cycle, day, firstDueDate)
}
