package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun weekdayScreen(day: String): Screen = when (day.lowercase()) {
    "monday" -> Screen.Monday
    "tuesday" -> Screen.Tuesday
    "wednesday" -> Screen.Wednesday
    "thursday" -> Screen.Thursday
    "friday" -> Screen.Friday
    "saturday" -> Screen.Saturday
    else -> Screen.Sunday
}

@Composable
fun CustomerListPage(
    title: String, cycle: String, customers: List<Customer>, onAdd: () -> Unit,
    onSelect: (Customer) -> Unit, onCollect: (Customer) -> Unit, onWeekday: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("${customers.size} customers · ${rupees(customers.sumOf { it.due })} remaining", color = Muted, fontSize = 12.sp)
                }
                Button(onClick = onAdd) { Icon(Icons.Default.Add, null); Text(" Add") }
            }
        }
        if (cycle == "Weekly") {
            item {
                Text("Collection day", color = Navy, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(listOf("Monday", "Tuesday"), listOf("Wednesday", "Thursday"), listOf("Friday", "Saturday"), listOf("Sunday")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { weekday ->
                                OutlinedButton(onClick = { onWeekday(weekday) }, modifier = Modifier.weight(1f)) { Text(weekday, fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }
        }
        if (customers.isEmpty()) item { EmptyCard("No customers yet", "Tap Add to create your first ${cycle.lowercase()} customer.") }
        items(customers) { customer ->
            CustomerCard(customer, onClick = { onSelect(customer) }, onCollect = { onCollect(customer) })
        }
    }
}
