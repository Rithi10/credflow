package com.example.credflow

import com.example.credflow.data.remote.dto.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportsDtoTest {

    @Test
    fun reportsDtoSummaryMetricsAndCalculations() {
        val summary = ReportsSummaryDto(
            todayCollection = 4633.0,
            weeklyCollection = 28616.0,
            monthlyCollection = 28616.0,
            totalCollected = 28616.0,
            totalPending = 97764.0,
            totalOverdue = 142.67,
            filteredCollection = 28616.0,
            filteredPaymentsCount = 24,
            activeCustomersCount = 11,
            overdueCustomersCount = 1
        )

        assertEquals(4633.0, summary.todayCollection, 0.001)
        assertEquals(28616.0, summary.weeklyCollection, 0.001)
        assertEquals(28616.0, summary.monthlyCollection, 0.001)
        assertEquals(28616.0, summary.totalCollected, 0.001)
        assertEquals(97764.0, summary.totalPending, 0.001)
        assertEquals(142.67, summary.totalOverdue, 0.001)
        assertEquals(24, summary.filteredPaymentsCount)
        assertEquals(11, summary.activeCustomersCount)
        assertEquals(1, summary.overdueCustomersCount)
    }

    @Test
    fun chartPointDtosContainExpectedProperties() {
        val dailyPoint = DailyChartPointDto(
            date = "2026-11-25",
            displayName = "Nov 25",
            dayName = "Wednesday",
            amount = 4633.0,
            count = 2
        )
        assertEquals("2026-11-25", dailyPoint.date)
        assertEquals("Nov 25", dailyPoint.displayName)
        assertEquals("Wednesday", dailyPoint.dayName)
        assertEquals(4633.0, dailyPoint.amount, 0.001)
        assertEquals(2, dailyPoint.count)

        val weeklyPoint = WeeklyChartPointDto(
            key = "2026-W48",
            displayName = "Nov 23 - Nov 29",
            amount = 28616.0,
            count = 24
        )
        assertEquals("2026-W48", weeklyPoint.key)
        assertEquals(28616.0, weeklyPoint.amount, 0.001)

        val monthlyPoint = MonthlyChartPointDto(
            key = "2026-11",
            displayName = "Nov 2026",
            amount = 28616.0,
            count = 24
        )
        assertEquals("2026-11", monthlyPoint.key)
        assertEquals("Nov 2026", monthlyPoint.displayName)
        assertEquals(28616.0, monthlyPoint.amount, 0.001)
    }

    @Test
    fun cycleBreakdownAndPaymentMethodsIntegrity() {
        val dailyCycle = CycleReportDto(
            cycle = "Daily",
            customerCount = 4,
            collectedAmount = 2036.0,
            pendingAmount = 21794.0,
            overdueAmount = 142.67
        )
        val weeklyCycle = CycleReportDto(
            cycle = "Weekly",
            customerCount = 5,
            collectedAmount = 19381.0,
            pendingAmount = 18769.0,
            overdueAmount = 0.0
        )
        val monthlyCycle = CycleReportDto(
            cycle = "Monthly",
            customerCount = 2,
            collectedAmount = 7199.0,
            pendingAmount = 57201.0,
            overdueAmount = 0.0
        )

        val cycleMap = mapOf(
            "daily" to dailyCycle,
            "weekly" to weeklyCycle,
            "monthly" to monthlyCycle
        )

        val paymentMethods = mapOf(
            "Cash" to 28616.0,
            "UPI" to 0.0,
            "Bank" to 0.0
        )

        val recentCollection = RecentCollectionDto(
            paymentId = 46,
            customerId = 24,
            customerName = "karthi",
            customerPhone = "9632587136",
            cycle = "Monthly",
            amount = 3533.0,
            paymentDate = "2026-11-25",
            method = "Cash",
            note = "Cash"
        )

        val dto = ReportsDto(
            status = "ok",
            userId = 31,
            dataTimestamp = "2026-11-25 13:11:32",
            filter = FilterInfoDto(rangePreset = "all"),
            summary = ReportsSummaryDto(
                todayCollection = 4633.0,
                weeklyCollection = 28616.0,
                monthlyCollection = 28616.0,
                totalCollected = 28616.0,
                totalPending = 97764.0,
                totalOverdue = 142.67
            ),
            dailyChart = listOf(DailyChartPointDto(date = "2026-11-25", displayName = "Nov 25", dayName = "Wednesday", amount = 4633.0, count = 2)),
            weeklyChart = listOf(WeeklyChartPointDto(key = "2026-W48", displayName = "Nov 23 - Nov 29", amount = 28616.0, count = 24)),
            monthlyChart = listOf(MonthlyChartPointDto(key = "2026-11", displayName = "Nov 2026", amount = 28616.0, count = 24)),
            cycleBreakdown = cycleMap,
            paymentMethods = paymentMethods,
            recentCollections = listOf(recentCollection)
        )

        assertTrue(dto.isSuccess)
        assertEquals(31, dto.userId)
        assertEquals(3, dto.cycleBreakdown.size)
        assertEquals(28616.0, dto.paymentMethods["Cash"] ?: 0.0, 0.001)
        assertEquals(1, dto.recentCollections.size)
        assertEquals("karthi", dto.recentCollections[0].customerName)
    }
}

