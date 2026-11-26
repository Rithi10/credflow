package com.example.credflow.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ScheduleUtils {
    val WEEKDAYS = listOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun normalizeCycle(rawCycle: String?): String {
        return when (rawCycle?.trim()?.lowercase(Locale.ROOT) ?: "") {
            "weekly", "week" -> "Weekly"
            "monthly", "month" -> "Monthly"
            else -> "Daily"
        }
    }

    fun normalizeWeeklyDay(rawDay: String?, firstDueDate: String? = null): String {
        val clean = rawDay?.trim()?.lowercase(Locale.ROOT) ?: ""
        val mapped = when (clean) {
            "mon", "monday" -> "Monday"
            "tue", "tues", "tuesday" -> "Tuesday"
            "wed", "wednesday" -> "Wednesday"
            "thu", "thur", "thurs", "thursday" -> "Thursday"
            "fri", "friday" -> "Friday"
            "sat", "saturday" -> "Saturday"
            "sun", "sunday" -> "Sunday"
            else -> ""
        }
        if (mapped.isNotEmpty()) return mapped

        val date = parseIsoDate(firstDueDate)
        if (date != null) {
            val name = date.dayOfWeek.name.lowercase(Locale.ROOT)
            return name.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
        return ""
    }

    fun extractMonthlyDueDay(rawDay: String?, firstDueDate: String? = null): Int {
        val clean = rawDay?.trim() ?: ""
        if (clean.isNotEmpty() && clean.lowercase(Locale.ROOT) !in setOf("0", "monthly", "daily", "weekly")) {
            val match = Regex("\\b([1-9]|[12][0-9]|3[01])\\b").find(clean)
            val parsed = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (parsed != null && parsed in 1..31) return parsed
        }
        val date = parseIsoDate(firstDueDate)
        return date?.dayOfMonth ?: 0
    }

    fun formatMonthlyDay(dueDay: Int): String {
        return if (dueDay in 1..31) "Day $dueDay" else ""
    }

    fun normalizeMonthlyDay(rawDay: String?, firstDueDate: String? = null): String {
        return formatMonthlyDay(extractMonthlyDueDay(rawDay, firstDueDate))
    }

    fun resolveCollectionDay(cycle: String, rawDay: String?, firstDueDate: String? = null): String {
        return when (normalizeCycle(cycle)) {
            "Weekly" -> normalizeWeeklyDay(rawDay, firstDueDate)
            "Monthly" -> normalizeMonthlyDay(rawDay, firstDueDate)
            else -> "Daily"
        }
    }

    fun needsScheduleCorrection(cycle: String, day: String?, firstDueDate: String? = null): Boolean {
        return when (normalizeCycle(cycle)) {
            "Weekly" -> normalizeWeeklyDay(day, firstDueDate).isBlank()
            "Monthly" -> extractMonthlyDueDay(day, firstDueDate) <= 0
            else -> false
        }
    }

    fun displayScheduleLabel(cycle: String, day: String?, firstDueDate: String? = null): String {
        return when (normalizeCycle(cycle)) {
            "Weekly" -> {
                val wd = normalizeWeeklyDay(day, firstDueDate)
                if (wd.isNotBlank()) "Weekly · $wd" else "Weekly · Weekday missing"
            }
            "Monthly" -> {
                val dom = extractMonthlyDueDay(day, firstDueDate)
                if (dom in 1..31) "Monthly · Day $dom" else "Monthly · Due date missing"
            }
            else -> "Daily"
        }
    }

    fun parseIsoDate(raw: String?): LocalDate? {
        val clean = raw?.trim()?.take(10) ?: return null
        if (clean.length < 10) return null
        return try {
            LocalDate.parse(clean, isoFormatter)
        } catch (_: Exception) {
            null
        }
    }
}

