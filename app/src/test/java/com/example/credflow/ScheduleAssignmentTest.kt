package com.example.credflow

import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.LoanCalculator
import com.example.credflow.domain.model.LoanScheduleCalculator
import com.example.credflow.util.ScheduleUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleAssignmentTest {

    @Test
    fun weeklyNormalization_supportsAllSevenWeekdaysAndNeverGuessesMissing() {
        val expected = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )
        expected.forEach { wd ->
            assertEquals(wd, ScheduleUtils.normalizeWeeklyDay(wd.lowercase()))
            assertEquals(wd, ScheduleUtils.normalizeWeeklyDay(wd.take(3)))
        }
        assertEquals("", ScheduleUtils.normalizeWeeklyDay("0"))
        assertEquals("", ScheduleUtils.normalizeWeeklyDay(""))
        assertEquals("Tuesday", ScheduleUtils.normalizeWeeklyDay("0", "2026-12-01"))
    }

    @Test
    fun monthlyNormalization_extractsDueDayAndPreservesAnchorAcrossMonths() {
        assertEquals("Day 23", ScheduleUtils.normalizeMonthlyDay("0", "2026-12-23"))
        assertEquals("Day 15", ScheduleUtils.normalizeMonthlyDay("Day 15"))
        assertEquals("Day 5", ScheduleUtils.normalizeMonthlyDay("5"))
        assertEquals("", ScheduleUtils.normalizeMonthlyDay("Monthly", ""))
        assertEquals("", ScheduleUtils.normalizeMonthlyDay("0", ""))

        val calc = LoanCalculator.calculateLoan(
            principal = 6000.0,
            interestRate = 0.0,
            period = 3,
            periodUnit = "Months",
            frequency = "Monthly",
            collectionDay = "Day 31",
            firstDueDateStr = "2027-01-31"
        )
        assertEquals(listOf("2027-01-31", "2027-02-28", "2027-03-31"), calc.schedule.map { it.dueDate })
    }

    @Test
    fun loanCalculator_weeklyGeneratesMatchingWeekdayForAllSevenDays() {
        val map = mapOf(
            "Monday" to DayOfWeek.MONDAY,
            "Tuesday" to DayOfWeek.TUESDAY,
            "Wednesday" to DayOfWeek.WEDNESDAY,
            "Thursday" to DayOfWeek.THURSDAY,
            "Friday" to DayOfWeek.FRIDAY,
            "Saturday" to DayOfWeek.SATURDAY,
            "Sunday" to DayOfWeek.SUNDAY
        )
        map.forEach { (wd, expectedDow) ->
            val calc = LoanCalculator.calculateLoan(
                principal = 4000.0,
                interestRate = 10.0,
                period = 4,
                periodUnit = "Weeks",
                frequency = "Weekly",
                collectionDay = wd
            )
            assertEquals(4, calc.schedule.size)
            calc.schedule.forEach { item ->
                val d = LocalDate.parse(item.dueDate)
                assertEquals(expectedDow, d.dayOfWeek)
            }
        }
    }

    @Test
    fun monthlyDueToday_usesStoredDueDayNotCreatedAt() {
        val cDueToday = Customer(
            id = 10,
            name = "Monthly Match",
            phone = "9876543210",
            amount = 5000.0,
            cycle = "Monthly",
            day = "Day 15",
            firstDueDate = "2026-12-15",
            createdAt = "2026-11-01 10:00:00",
            totalDue = 5500.0,
            paid = 0.0,
            due = 5500.0
        )
        assertTrue(LoanScheduleCalculator.isDueToday(cDueToday, LocalDate.of(2026, 12, 15)))
        assertFalse(LoanScheduleCalculator.isDueToday(cDueToday, LocalDate.of(2026, 12, 1)))
        assertFalse(cDueToday.scheduleNeedsCorrection)
        assertEquals("Monthly · Day 15", cDueToday.scheduleDisplayLabel)

        val missingWeekly = Customer(
            id = 11,
            name = "Legacy Missing",
            phone = "9876543211",
            amount = 3000.0,
            cycle = "Weekly",
            day = "",
            firstDueDate = "",
            totalDue = 3000.0,
            paid = 0.0,
            due = 3000.0
        )
        assertTrue(missingWeekly.scheduleNeedsCorrection)
        assertEquals("Weekly · Weekday missing", missingWeekly.scheduleDisplayLabel)
    }
}

