package com.example.credflow

import androidx.compose.foundation.BorderStroke
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
    onSearch: () -> Unit, onReports: () -> Unit = {},
    onNotifications: () -> Unit, onSettings: () -> Unit,
    onDaily: () -> Unit, onWeekly: () -> Unit, onMonthly: () -> Unit,
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    refreshError: String? = null,
    onDismissRefreshError: () -> Unit = {}
) {
    val outstanding = customers.sumOf { it.due }
    val collected = customers.sumOf { it.paid }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text(
                        if (isRefreshing) "Refreshing live data from MySQL..." else "Good morning · Your collection book",
                        fontSize = 12.sp,
                        color = if (isRefreshing) Blue else Muted
                    )
                }
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Navy)
                    } else {
                        Icon(Icons.Default.Refresh, "Sync from MySQL")
                    }
                }
                IconButton(onClick = onNotifications) { Icon(Icons.Default.NotificationsNone, "Notifications") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
            }
        }
        if (isRefreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        if (!refreshError.isNullOrBlank()) {
            item {
                Surface(
                    color = Color(0xFFFFF3F2),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                        Column(Modifier.weight(1f)) {
                            Text("Refresh failed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFFC62828))
                            Text(refreshError, fontSize = 12.sp, color = Color(0xFF8E2424))
                        }
                        TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                            Text("Retry")
                        }
                        IconButton(onClick = onDismissRefreshError, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
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
    }
}
