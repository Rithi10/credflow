package com.example.credflow

import androidx.compose.runtime.Composable

@Composable
fun ForecastDetailPage(title: String, customers: List<Customer>, cycle: String, onBack: () -> Unit) {
    val filtered = customers.filter { it.cycle == cycle }
    SimpleListPage(title, "Collection estimate summary",
        listOf("Customers: ${filtered.size}", "Remaining amount: ${rupees(filtered.sumOf { it.due })}", "Collected: ${rupees(filtered.sumOf { it.paid })}", "This is a current-record summary, not a trained AI forecast."),
        onPrimary = onBack, primaryLabel = "Back to forecast")
}
