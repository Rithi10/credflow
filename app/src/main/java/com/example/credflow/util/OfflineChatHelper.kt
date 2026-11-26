package com.example.credflow.util

import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import com.example.credflow.rupees

object OfflineChatHelper {

    private val WEEKDAY_RULES = listOf(
        "Monday" to listOf("monday", "munday", "monady", "thingal", "திங்கள்"),
        "Tuesday" to listOf("tuesday", "tuesdy", "tuseday", "sevvaai", "sevvai", "செவ்வாய்"),
        "Wednesday" to listOf("wednesday", "wednsday", "wensday", "budhan", "puthan", "புதன்"),
        "Thursday" to listOf("thursday", "thrusday", "thursdy", "viyazhan", "vyazhan", "guru", "வியாழன்"),
        "Friday" to listOf("friday", "fridy", "velli", "வெள்ளி"),
        "Saturday" to listOf("saturday", "satrday", "saterday", "sani", "சனி"),
        "Sunday" to listOf("sunday", "sundya", "nyaayiru", "nyayiru", "nyiru", "ஞாயிறு")
    )

    private val MUTATION_PATTERNS = listOf(
        Regex("\\b(move|shift|transfer|reassign|mathu|maathu|seru|podu)\\b"),
        Regex("\\b(add|create|new|puthu|pudhu)\\s+customer\\b"),
        Regex("\\badd\\s+pannu\\b"),
        Regex("\\b(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|list)\\b.*\\badd\\b"),
        Regex("\\b(record|add)\\s+payment\\b"),
        Regex("\\b(update|edit)\\s+(customer|phone|note|name|cycle)\\b"),
        Regex("வாடிக்கையாளரை\\s+சேர்|மாற்று|வசூல்\\s+பதிவு")
    )

    fun isOfflineMutationCommand(question: String): Boolean {
        val q = question.trim().lowercase()
        if (q.isBlank()) return false
        return MUTATION_PATTERNS.any { it.containsMatchIn(q) }
    }

    fun extractWeekdayStrict(question: String): String? {
        val q = question.trim().lowercase()
        for ((dayName, aliases) in WEEKDAY_RULES) {
            if (aliases.any { Regex("(?<!\\p{L})" + Regex.escape(it) + "(?!\\p{L})").containsMatchIn(q) }) {
                return dayName
            }
        }
        return null
    }

    fun filterActiveWeekdayCustomers(customers: List<Customer>, dayName: String): List<Customer> {
        return customers.filter {
            !it.isClosed &&
                it.cycle.equals("Weekly", ignoreCase = true) &&
                it.day.equals(dayName, ignoreCase = true) &&
                it.due > 0.01
        }
    }

    fun computeFallbackReply(
        question: String,
        customers: List<Customer>,
        payments: List<Payment>
    ): String {
        val q = question.trim().lowercase()

        if (isOfflineMutationCommand(q)) {
            return "You are currently offline. Updating customers, moving weekday lists, or recording payments requires an active connection to the Credflow MySQL backend. Please reconnect and retry."
        }

        val dayName = extractWeekdayStrict(q)
        if (dayName != null) {
            val matched = filterActiveWeekdayCustomers(customers, dayName)
            val totalDue = matched.sumOf { if (it.installmentAmount > 0) it.installmentAmount else it.due }
            return if (matched.isNotEmpty()) {
                val lines = matched.mapIndexed { i, c ->
                    val inst = if (c.installmentAmount > 0) c.installmentAmount else c.due
                    "${i + 1}. ${c.name} (Phone: ${c.phone.ifBlank { "N/A" }}, $dayName Weekly) — Due: ${rupees(inst)}"
                }
                "$dayName Customers (Offline Cache)\nTotal Customers: ${matched.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Expected ($dayName): ${rupees(totalDue)}"
            } else {
                "$dayName Customers (Offline Cache)\nTotal Customers: 0\nNo active $dayName weekly customers found in offline cache."
            }
        }

        return when {
            "overdue" in q || "pending" in q || "pannala" in q || "kattala" in q -> {
                val pending = customers.filter { !it.isClosed && it.due > 0.01 }
                val pendingAmount = pending.sumOf { it.due }
                val lines = pending.take(5).mapIndexed { i, c -> "${i + 1}. ${c.name} — Balance: ${rupees(c.due)}" }
                "Pending Customers (Offline Cache)\nTotal Customers: ${pending.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Outstanding: ${rupees(pendingAmount)}"
            }
            "tomorrow" in q || "naalaiku" in q || "nalaiku" in q -> {
                val daily = customers.filter { !it.isClosed && it.cycle.equals("Daily", ignoreCase = true) && it.due > 0.01 }
                val dailyExpected = daily.sumOf { if (it.installmentAmount > 0) it.installmentAmount else it.due }
                val lines = daily.mapIndexed { i, c -> "${i + 1}. ${c.name} (Phone: ${c.phone.ifBlank { "N/A" }}, Daily) — Due: ${rupees(if (c.installmentAmount > 0) c.installmentAmount else c.due)}" }
                "Tomorrow Schedule (Offline Cache - Daily Accounts)\nTotal Customers: ${daily.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Expected: ${rupees(dailyExpected)}"
            }
            "today" in q || "daily" in q || "inniku" in q || "inniki" in q -> {
                val daily = customers.filter { !it.isClosed && it.cycle.equals("Daily", ignoreCase = true) }
                val lines = daily.mapIndexed { i, c -> "${i + 1}. ${c.name} (Phone: ${c.phone.ifBlank { "N/A" }}) — Due: ${rupees(c.due)}" }
                "Daily Customers (Offline Cache)\nTotal Customers: ${daily.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Remaining: ${rupees(daily.sumOf { it.due })}"
            }
            "week" in q || "weekly" in q || "vaaram" in q || "varam" in q -> {
                val weekly = customers.filter { !it.isClosed && it.cycle.equals("Weekly", ignoreCase = true) }
                val lines = weekly.mapIndexed { i, c -> "${i + 1}. ${c.name} (${c.day.ifBlank { "Weekly" }}) — Due: ${rupees(c.due)}" }
                "Weekly Customers (Offline Cache)\nTotal Customers: ${weekly.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Remaining: ${rupees(weekly.sumOf { it.due })}"
            }
            "month" in q || "monthly" in q || "maasam" in q || "masam" in q -> {
                val monthly = customers.filter { !it.isClosed && it.cycle.equals("Monthly", ignoreCase = true) }
                "Monthly Customers (Offline Cache)\nTotal Customers: ${monthly.size}\nRemaining due: ${rupees(monthly.sumOf { it.due })}."
            }
            "all" in q && "customer" in q -> {
                val lines = customers.take(8).mapIndexed { i, c -> "${i + 1}. ${c.name} (${c.cycle}) — Due: ${rupees(c.due)}" }
                "All Customers (Offline Cache)\nTotal Customers: ${customers.size}\n\n" +
                    lines.joinToString("\n") +
                    "\n\nTotal Outstanding: ${rupees(customers.sumOf { it.due })}"
            }
            "collect" in q || "paid" in q || "pannom" in q -> {
                "Total collections recorded: ${payments.size}. Total amount collected: ${rupees(customers.sumOf { it.paid })} (Offline Cache)."
            }
            else -> {
                val totalDue = customers.sumOf { it.due }
                val totalPaid = customers.sumOf { it.paid }
                "Offline Portfolio: ${customers.size} customer(s), ${rupees(totalPaid)} collected, ${rupees(totalDue)} outstanding (Offline Cache)."
            }
        }
    }
}

