package com.example.credflow.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

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
        if (customer.due <= 0.0) return false
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
        if (customer.due <= 0.0) return "Paid in Full"

        val customerPayments = payments.filter { it.customerId == customer.id }
        val hasPaidToday = customerPayments.any { parseDate(it.paymentDate) == today }

        return when (customer.cycle.lowercase(Locale.ROOT)) {
            "daily" -> {
                val nextDate = if (hasPaidToday) today.plusDays(1) else today
                nextDate.format(isoDateFormatter)
            }
            "weekly" -> {
                val targetDay = parseDayOfWeek(customer.day) ?: DayOfWeek.MONDAY
                var next = today
                if (today.dayOfWeek == targetDay) {
                    if (hasPaidToday) {
                        next = today.plusWeeks(1)
                    }
                } else {
                    while (next.dayOfWeek != targetDay) {
                        next = next.plusDays(1)
                    }
                }
                next.format(isoDateFormatter)
            }
            "monthly" -> {
                val created = parseDate(customer.createdAt)
                val dayOfMonth = created?.dayOfMonth ?: 1
                var nextMonth = if (today.dayOfMonth > dayOfMonth || (today.dayOfMonth == dayOfMonth && hasPaidToday)) {
                    today.plusMonths(1)
                } else {
                    today
                }
                val validDay = dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
                nextMonth.withDayOfMonth(validDay).format(isoDateFormatter)
            }
            else -> today.format(isoDateFormatter)
        }
    }

    fun getOverdueInfo(
        customer: Customer,
        today: LocalDate = LocalDate.now(),
        payments: List<Payment> = emptyList()
    ): Pair<Boolean, Long> {
        // Fully paid customers are never overdue
        if (customer.due <= 0.0) return Pair(false, 0L)

        // Payment due today is NOT overdue per Requirement 2
        val customerPayments = payments.filter { it.customerId == customer.id }
        val latestPayment = customerPayments.mapNotNull { parseDate(it.paymentDate) }.maxOrNull()
        val createdDate = parseDate(customer.createdAt) ?: today

        when (customer.cycle.lowercase(Locale.ROOT)) {
            "daily" -> {
                val referenceDate = latestPayment ?: createdDate
                val daysDiff = ChronoUnit.DAYS.between(referenceDate, today)
                // If more than 1 day has passed without payment and due > 0, overdue
                return if (daysDiff > 1) {
                    Pair(true, daysDiff - 1)
                } else {
                    Pair(false, 0L)
                }
            }
            "weekly" -> {
                val targetDay = parseDayOfWeek(customer.day) ?: DayOfWeek.MONDAY
                // If today is collection day, it is due today, not overdue
                if (today.dayOfWeek == targetDay) return Pair(false, 0L)

                // Find the most recent collection day before today
                var lastDueDay = today.minusDays(1)
                while (lastDueDay.dayOfWeek != targetDay) {
                    lastDueDay = lastDueDay.minusDays(1)
                }

                // If created after that last due day, not overdue
                if (createdDate.isAfter(lastDueDay)) return Pair(false, 0L)

                // Check if payment was made on or after the last due day
                val paidForLastCycle = latestPayment != null && !latestPayment.isBefore(lastDueDay)
                return if (!paidForLastCycle) {
                    val daysOver = ChronoUnit.DAYS.between(lastDueDay, today)
                    Pair(true, daysOver)
                } else {
                    Pair(false, 0L)
                }
            }
            "monthly" -> {
                val dayOfMonth = createdDate.dayOfMonth
                var lastDueDate = today.withDayOfMonth(dayOfMonth.coerceAtMost(today.lengthOfMonth()))
                if (lastDueDate.isAfter(today)) {
                    val prevMonth = today.minusMonths(1)
                    lastDueDate = prevMonth.withDayOfMonth(dayOfMonth.coerceAtMost(prevMonth.lengthOfMonth()))
                }
                if (lastDueDate == today) return Pair(false, 0L)
                if (createdDate.isAfter(lastDueDate)) return Pair(false, 0L)

                val paidRecently = latestPayment != null && !latestPayment.isBefore(lastDueDate)
                return if (!paidRecently) {
                    val days = ChronoUnit.DAYS.between(lastDueDate, today)
                    Pair(true, days)
                } else {
                    Pair(false, 0L)
                }
            }
            else -> return Pair(false, 0L)
        }
    }

    fun getRemainingCycles(customer: Customer): Int {
        if (customer.due <= 0.0) return 0
        if (customer.amount <= 0.0) return 1
        return ceil(customer.due / customer.amount).toInt().coerceAtLeast(1)
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

