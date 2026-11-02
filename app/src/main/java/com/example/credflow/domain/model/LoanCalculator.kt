package com.example.credflow.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

data class InstallmentScheduleItem(
    val number: Int,
    val dueDate: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val status: String = "Pending" // "Paid", "Partial", "Pending", "Overdue"
)

data class LoanCalculationResult(
    val principal: Double,
    val interestRate: Double,
    val interestAmount: Double,
    val totalRepayment: Double,
    val numberOfInstallments: Int,
    val installmentAmount: Double,
    val firstDueDate: String,
    val finalDueDate: String,
    val schedule: List<InstallmentScheduleItem>
)

object LoanCalculator {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculateLoan(
        principal: Double,
        interestRate: Double,
        period: Int,
        periodUnit: String,
        frequency: String,
        collectionDay: String = "Monday",
        firstDueDateStr: String? = null
    ): LoanCalculationResult {
        val safePrincipal = principal.coerceAtLeast(0.0)
        val safeInterestRate = interestRate.coerceAtLeast(0.0)
        val safePeriod = period.coerceAtLeast(1)

        // 1. Interest Amount = Principal * Interest Rate / 100
        val interestAmount = round((safePrincipal * safeInterestRate / 100.0) * 100.0) / 100.0

        // 2. Total Repayment = Principal + Interest Amount
        val totalRepayment = round((safePrincipal + interestAmount) * 100.0) / 100.0

        // 3. Number of Installments based on Period, Period Unit and Frequency
        val numberOfInstallments = calculateInstallmentCount(safePeriod, periodUnit, frequency)

        // 4. Base Installment Amount
        val baseInstallment = round((totalRepayment / numberOfInstallments) * 100.0) / 100.0

        // 5. Determine First Due Date
        val firstDue = parseOrComputeFirstDueDate(firstDueDateStr, frequency, collectionDay)
        val firstDueDateFormatted = firstDue.format(dateFormatter)

        // 6. Generate Schedule & handle final installment rounding
        val schedule = ArrayList<InstallmentScheduleItem>(numberOfInstallments)
        var currentDate = firstDue
        var allocatedSum = 0.0

        for (i in 1..numberOfInstallments) {
            val instAmount = if (i == numberOfInstallments) {
                round((totalRepayment - allocatedSum) * 100.0) / 100.0
            } else {
                baseInstallment
            }
            allocatedSum += instAmount

            schedule.add(
                InstallmentScheduleItem(
                    number = i,
                    dueDate = currentDate.format(dateFormatter),
                    amount = instAmount,
                    paidAmount = 0.0,
                    status = "Pending"
                )
            )

            currentDate = advanceDueDate(currentDate, frequency)
        }

        val finalDueDateFormatted = schedule.lastOrNull()?.dueDate ?: firstDueDateFormatted

        return LoanCalculationResult(
            principal = safePrincipal,
            interestRate = safeInterestRate,
            interestAmount = interestAmount,
            totalRepayment = totalRepayment,
            numberOfInstallments = numberOfInstallments,
            installmentAmount = baseInstallment,
            firstDueDate = firstDueDateFormatted,
            finalDueDate = finalDueDateFormatted,
            schedule = schedule
        )
    }

    fun calculateInstallmentCount(period: Int, periodUnit: String, frequency: String): Int {
        val p = period.coerceAtLeast(1)
        val unit = periodUnit.trim().lowercase(Locale.ROOT)
        val freq = frequency.trim().lowercase(Locale.ROOT)

        val count = when (freq) {
            "daily" -> {
                when {
                    unit.startsWith("day") -> p
                    unit.startsWith("week") -> p * 7
                    unit.startsWith("month") -> p * 30
                    else -> p
                }
            }
            "weekly" -> {
                when {
                    unit.startsWith("week") -> p
                    unit.startsWith("month") -> p * 4
                    unit.startsWith("day") -> (p / 7).coerceAtLeast(1)
                    else -> p
                }
            }
            "monthly" -> {
                when {
                    unit.startsWith("month") -> p
                    unit.startsWith("week") -> (p / 4).coerceAtLeast(1)
                    unit.startsWith("day") -> (p / 30).coerceAtLeast(1)
                    else -> p
                }
            }
            else -> p
        }
        return count.coerceAtLeast(1)
    }

    fun parseOrComputeFirstDueDate(
        firstDueDateStr: String?,
        frequency: String,
        collectionDay: String
    ): LocalDate {
        if (!firstDueDateStr.isNullOrBlank()) {
            try {
                return LocalDate.parse(firstDueDateStr.trim().take(10), dateFormatter)
            } catch (_: Exception) {}
        }

        val today = LocalDate.now()
        val freq = frequency.trim().lowercase(Locale.ROOT)

        return when (freq) {
            "daily" -> today.plusDays(1)
            "weekly" -> {
                val targetDay = parseDayOfWeek(collectionDay) ?: DayOfWeek.MONDAY
                var next = today.plusDays(1)
                while (next.dayOfWeek != targetDay) {
                    next = next.plusDays(1)
                }
                next
            }
            "monthly" -> today.plusMonths(1)
            else -> today.plusDays(1)
        }
    }

    private fun advanceDueDate(current: LocalDate, frequency: String): LocalDate {
        return when (frequency.trim().lowercase(Locale.ROOT)) {
            "daily" -> current.plusDays(1)
            "weekly" -> current.plusWeeks(1)
            "monthly" -> current.plusMonths(1)
            else -> current.plusDays(1)
        }
    }

    fun parseDayOfWeek(dayStr: String): DayOfWeek? {
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
