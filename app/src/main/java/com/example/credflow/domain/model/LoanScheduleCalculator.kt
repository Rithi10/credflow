package com.example.credflow.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object LoanScheduleCalculator {
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val cleanStr = dateStr.trim().take(10)
        return try {
            LocalDate.parse(cleanStr, isoDateFormatter)
        } catch (_: Exception) {
            null
        }
    }

    fun isDueToday(customer: Customer, today: LocalDate = LocalDate.now()): Boolean {
        if (customer.due <= 0.001) return false
        return when (customer.cycle.lowercase(Locale.ROOT)) {
            "daily" -> true
            "weekly" -> {
                val todayName = today.dayOfWeek.name.lowercase(Locale.ROOT)
                val customerDay = customer.day.trim().lowercase(Locale.ROOT)
                customerDay == todayName || customerDay.take(3) == todayName.take(3)
            }
            "monthly" -> {
                val created = parseDate(customer.createdAt)
                val targetDay = created?.dayOfMonth ?: 1
                today.dayOfMonth == targetDay
            }
            else -> false
        }
    }

    fun getNextDueDate(customer: Customer, today: LocalDate = LocalDate.now(), payments: List<Payment> = emptyList()): String {
        if (customer.due <= 0.001) return "Paid in Full"

        val calc = LoanCalculator.calculateLoan(
            principal = customer.amount,
            interestRate = customer.interest,
            period = customer.period,
            periodUnit = customer.periodUnit,
            frequency = customer.cycle,
            collectionDay = customer.day,
            firstDueDateStr = customer.firstDueDate
        )

        var remainingPaid = customer.paid
        for (item in calc.schedule) {
            val alloc = min(item.amount, max(0.0, remainingPaid))
            remainingPaid -= alloc
            val unpaid = (item.amount - alloc).coerceAtLeast(0.0)

            if (unpaid >= 1.0) {
                return item.dueDate
            }
        }

        return "Paid in Full"
    }

    fun getOverdueInfo(
        customer: Customer,
        today: LocalDate = LocalDate.now(),
        payments: List<Payment> = emptyList()
    ): Pair<Boolean, Long> {
        if (customer.due <= 0.001) return Pair(false, 0L)

        val calc = LoanCalculator.calculateLoan(
            principal = customer.amount,
            interestRate = customer.interest,
            period = customer.period,
            periodUnit = customer.periodUnit,
            frequency = customer.cycle,
            collectionDay = customer.day,
            firstDueDateStr = customer.firstDueDate
        )

        var remainingPaid = customer.paid
        var earliestOverdueDueDate: LocalDate? = null

        for (item in calc.schedule) {
            val dueDate = parseDate(item.dueDate) ?: continue
            val alloc = min(item.amount, max(0.0, remainingPaid))
            remainingPaid -= alloc
            val unpaid = (item.amount - alloc).coerceAtLeast(0.0)

            if (unpaid >= 1.0 && dueDate.isBefore(today)) {
                earliestOverdueDueDate = dueDate
                break
            }
        }

        return if (earliestOverdueDueDate != null) {
            val days = ChronoUnit.DAYS.between(earliestOverdueDueDate, today)
            Pair(true, days.coerceAtLeast(1L))
        } else {
            Pair(false, 0L)
        }
    }

    fun getRemainingCycles(customer: Customer): Int {
        return customer.remainingInstallments
    }

    private fun parseDayOfWeek(dayStr: String): DayOfWeek? {
        val s = dayStr.trim().uppercase(Locale.ROOT)
        return try {
            DayOfWeek.valueOf(s)
        } catch (_: Exception) {
            when {
                s.startsWith("MON") -> DayOfWeek.MONDAY
                s.startsWith("TUE") -> DayOfWeek.TUESDAY
                s.startsWith("WED") -> DayOfWeek.WEDNESDAY
                s.startsWith("THU") -> DayOfWeek.THURSDAY
                s.startsWith("FRI") -> DayOfWeek.FRIDAY
                s.startsWith("SAT") -> DayOfWeek.SATURDAY
                s.startsWith("SUN") -> DayOfWeek.SUNDAY
                else -> null
            }
        }
    }
}
