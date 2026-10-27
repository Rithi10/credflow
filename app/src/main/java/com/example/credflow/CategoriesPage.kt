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
fun CategoriesPage(customers: List<Customer>, onDaily: () -> Unit, onWeekly: () -> Unit, onMonthly: () -> Unit, onAdd: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SummaryCard("Your customer book", "${customers.size} customers", "${rupees(customers.sumOf { it.due })} total outstanding") }
        item { Text("Choose a collection cycle", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { CycleCard("Daily Customers", "${customers.count { it.cycle == "Daily" }} customers", "Add or open daily customers", Icons.Default.Today, onDaily, { onAdd("Daily") }) }
        item { CycleCard("Weekly Customers", "${customers.count { it.cycle == "Weekly" }} customers", "Add customers and choose collection day", Icons.Default.CalendarViewWeek, onWeekly, { onAdd("Weekly") }) }
        item { CycleCard("Monthly Customers", "${customers.count { it.cycle == "Monthly" }} customers", "Add or open monthly customers", Icons.Default.CalendarMonth, onMonthly, { onAdd("Monthly") }) }
    }
}
