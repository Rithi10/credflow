package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardPage(
    customers: List<Customer>, payments: List<String>, onCategories: () -> Unit,
    onSearch: () -> Unit, onReports: () -> Unit,
    onNotifications: () -> Unit, onSettings: () -> Unit,
    onDaily: () -> Unit, onWeekly: () -> Unit, onMonthly: () -> Unit
) {
    val outstanding = customers.sumOf { it.due }
    val collected = customers.sumOf { it.paid }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Good morning · Your collection book", fontSize = 12.sp, color = Muted)
                }
                IconButton(onClick = onNotifications) { Icon(Icons.Default.NotificationsNone, "Notifications") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
            }
        }
        item { OutlinedButton(onClick = onSearch, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("Search customers") } }
        item {
            Surface(color = Navy, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Total outstanding", color = Color(0xFFCAD9EF))
                    Text(rupees(outstanding), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Collected", color = Color(0xFFCAD9EF), fontSize = 12.sp); Text(rupees(collected), color = Color.White, fontWeight = FontWeight.SemiBold) }
                        Column { Text("Customers", color = Color(0xFFCAD9EF), fontSize = 12.sp); Text("${customers.size}", color = Color.White, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        item { Text("Your collection sections", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy) }
        item { CycleCard("Daily Customers", "${customers.count { it.cycle == "Daily" }} customers", "Add and record daily collections", Icons.Default.Today, onDaily) }
        item { CycleCard("Weekly Customers", "${customers.count { it.cycle == "Weekly" }} customers", "Separate Monday to Sunday lists", Icons.Default.CalendarViewWeek, onWeekly) }
        item { CycleCard("Monthly Customers", "${customers.count { it.cycle == "Monthly" }} customers", "Monthly collection list", Icons.Default.CalendarMonth, onMonthly) }
        item { CycleCard("Reports", "${payments.size} collection entries", "Daily, weekly and monthly summaries", Icons.Default.BarChart, onReports) }
        item { Button(onClick = onCategories, modifier = Modifier.fillMaxWidth()) { Text("View Customer Categories") } }
    }
}
