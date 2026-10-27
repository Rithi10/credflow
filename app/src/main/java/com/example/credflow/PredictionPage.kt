package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PredictionPage(customers: List<Customer>, onDetails: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("AI Repayment Prediction", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { SummaryCard("Customers with remaining balance", "${customers.count { it.due > 0 }}", "Total remaining ${rupees(customers.sumOf { it.due })}") }
        item { InfoCard("Prediction status", "This starter does not yet run a trained AI model. A genuine late-payment probability needs historical due dates and actual payment dates.") }
        item { CycleCard("Repayment Risk Details", "View data summary", "Understand what this demo can and cannot predict", Icons.Default.Insights, onDetails) }
    }
}
