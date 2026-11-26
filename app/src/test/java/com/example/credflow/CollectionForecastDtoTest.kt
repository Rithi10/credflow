package com.example.credflow

import com.example.credflow.data.remote.dto.CollectionForecastDto
import com.example.credflow.data.remote.dto.CycleBreakdownItemDto
import com.example.credflow.data.remote.dto.ForecastCustomerDto
import com.example.credflow.data.remote.dto.ForecastHorizonDto
import com.example.credflow.data.remote.dto.OverduePoolDto
import com.example.credflow.data.remote.dto.TimelineDayDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionForecastDtoTest {

    @Test
    fun empiricalForecastDtoPropertiesAndInvariants() {
        val tomorrowCustomer = ForecastCustomerDto(
            customerId = 4,
            name = "Kavitha",
            phone = "9876543213",
            cycle = "Daily",
            amountDue = 100.0,
            expectedAmount = 80.0
        )

        val tomorrowHorizon = ForecastHorizonDto(
            periodKey = "tomorrow",
            targetDate = "2026-11-26",
            daysCount = 1,
            scheduledAmount = 260.0,
            expectedAmount = 222.16,
            lowEstimate = 165.96,
            highEstimate = 255.90,
            expectedCustomerCount = 3,
            customers = listOf(tomorrowCustomer)
        )

        val next7DaysHorizon = ForecastHorizonDto(
            periodKey = "next_7_days",
            daysCount = 7,
            scheduledAmount = 4656.0,
            expectedAmount = 3763.68,
            lowEstimate = 2818.14,
            highEstimate = 4331.15,
            expectedCustomerCount = 4
        )

        val next30DaysHorizon = ForecastHorizonDto(
            periodKey = "next_30_days",
            daysCount = 30,
            scheduledAmount = 22216.0,
            expectedAmount = 21589.90,
            lowEstimate = 16172.65,
            highEstimate = 24840.92,
            expectedCustomerCount = 7
        )

        val weeklyCycle = CycleBreakdownItemDto(
            scheduledAmount = 10956.0,
            expectedAmount = 8764.8,
            customerCount = 3,
            weekdayDistribution = mapOf(
                "Monday" to 5500.0,
                "Wednesday" to 2456.0,
                "Friday" to 3000.0
            )
        )

        val timeline = listOf(
            TimelineDayDto(
                date = "2026-11-26",
                dayName = "Thursday",
                scheduledAmount = 260.0,
                expectedAmount = 208.0,
                lowEstimate = 156.0,
                highEstimate = 247.0
            )
        )

        val dto = CollectionForecastDto(
            status = "ok",
            userId = 31,
            forecastPeriod = "all",
            predictionMethod = "EMPIRICAL_HISTORICAL",
            predictionMethodLabel = "Empirical Realization Model (Historical Payment Rate)",
            isSufficientData = true,
            insufficientDataReasons = emptyList(),
            dataTimestamp = "2026-11-25 09:30:00",
            scheduledAmount = 22216.0,
            expectedAmount = 21589.90,
            lowEstimate = 16172.65,
            highEstimate = 24840.92,
            expectedCustomerCount = 7,
            overduePool = OverduePoolDto(2, 40.0, 14.16),
            horizons = mapOf(
                "tomorrow" to tomorrowHorizon,
                "next_7_days" to next7DaysHorizon,
                "next_30_days" to next30DaysHorizon
            ),
            cycleBreakdown = mapOf(
                "weekly" to weeklyCycle
            ),
            timeline = timeline
        )

        assertTrue(dto.isSuccess)
        assertTrue(dto.isEmpirical)
        assertFalse(dto.isRuleBased)
        assertTrue(dto.isSufficientData)
        assertEquals(31, dto.userId)
        assertEquals(22216.0, dto.scheduledAmount, 0.001)
        assertEquals(21589.90, dto.expectedAmount, 0.001)
        assertEquals(7, dto.expectedCustomerCount)

        // Horizons
        val tomorrow = dto.getHorizon("tomorrow")
        assertNotNull(tomorrow)
        assertEquals("2026-11-26", tomorrow.targetDate)
        assertEquals(222.16, tomorrow.expectedAmount, 0.001)
        assertEquals(1, tomorrow.customers.size)
        assertEquals("Kavitha", tomorrow.customers[0].name)
        assertEquals(80.0, tomorrow.customers[0].expectedAmount, 0.001)

        // Invariant check: low <= expected <= high
        assertTrue(tomorrow.lowEstimate <= tomorrow.expectedAmount)
        assertTrue(tomorrow.expectedAmount <= tomorrow.highEstimate)

        // Cycle Breakdown & Weekday Distribution
        val weekly = dto.cycleBreakdown["weekly"]
        assertNotNull(weekly)
        assertEquals(10956.0, weekly!!.scheduledAmount, 0.001)
        assertEquals(5500.0, weekly.weekdayDistribution["Monday"] ?: 0.0, 0.001)
        assertEquals(3000.0, weekly.weekdayDistribution["Friday"] ?: 0.0, 0.001)

        // Timeline
        assertEquals(1, dto.timeline.size)
        assertEquals("Thursday", dto.timeline[0].dayName)
    }

    @Test
    fun ruleBasedFallbackWithInsufficientDataReasons() {
        val dto = CollectionForecastDto(
            status = "ok",
            userId = 31,
            forecastPeriod = "all",
            predictionMethod = "RULE_BASED_CASHFLOW",
            predictionMethodLabel = "Rule-Based Cash-Flow Model (Baseline Recovery)",
            isSufficientData = false,
            insufficientDataReasons = listOf(
                "Portfolio has only 11 matured installment records (15 recommended for empirical statistical confidence)."
            ),
            scheduledAmount = 5000.0,
            expectedAmount = 4000.0,
            lowEstimate = 3000.0,
            highEstimate = 4750.0,
            expectedCustomerCount = 2
        )

        assertTrue(dto.isSuccess)
        assertFalse(dto.isEmpirical)
        assertTrue(dto.isRuleBased)
        assertFalse(dto.isSufficientData)
        assertEquals(1, dto.insufficientDataReasons.size)
        assertTrue(dto.insufficientDataReasons[0].contains("11 matured"))
    }

    @Test
    fun noActiveInstallmentsEmptyState() {
        val dto = CollectionForecastDto(
            status = "no_active_installments",
            message = "There are no active customer loan balances to forecast."
        )

        assertTrue(dto.isNoActiveInstallments)
        assertEquals("There are no active customer loan balances to forecast.", dto.message)
    }
}
