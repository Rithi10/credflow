package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportsPage(customers: List<Customer>, payments: List<String>, onDaily: () -> Unit, onWeekly: () -> Unit, onMonthly: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Reports", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { SummaryCard("Collection overview", "${payments.size} collection entries", "Outstanding ${rupees(customers.sumOf { it.due })} · Collected ${rupees(customers.sumOf { it.paid })}") }
        item { CycleCard("Daily Report Details", "${customers.count { it.cycle == "Daily" }} customers", "Daily totals and balances", Icons.Default.Today, onDaily) }
        item { CycleCard("Weekly Report Details", "${customers.count { it.cycle == "Weekly" }} customers", "Weekly totals and balances", Icons.Default.CalendarViewWeek, onWeekly) }
        item { CycleCard("Monthly Report Details", "${customers.count { it.cycle == "Monthly" }} customers", "Monthly totals and balances", Icons.Default.CalendarMonth, onMonthly) }
    }
}

@Composable
fun ReportDetailPage(title: String, customers: List<Customer>, payments: List<String>, onBack: () -> Unit) {
    SimpleListPage(title, "Summary from locally saved records",
        listOf("Customers: ${customers.size}", "Total amount: ${rupees(customers.sumOf { it.amount })}", "Collected: ${rupees(customers.sumOf { it.paid })}", "Remaining: ${rupees(customers.sumOf { it.due })}", "All collection entries in app: ${payments.size}"),
        onPrimary = onBack, primaryLabel = "Back to Reports")
}
