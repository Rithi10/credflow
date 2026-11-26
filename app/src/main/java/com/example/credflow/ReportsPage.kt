package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.dto.*
import com.example.credflow.ui.viewmodel.ExportUiState
import com.example.credflow.ui.viewmodel.ReportsUiState

private val Crimson = Color(0xFFC53030)

@Composable
fun ReportsPage(
    reportsState: ReportsUiState = ReportsUiState.Idle,
    exportState: ExportUiState = ExportUiState.Idle,
    customers: List<Customer> = emptyList(),
    payments: List<String> = emptyList(),
    onDaily: () -> Unit = {},
    onWeekly: () -> Unit = {},
    onMonthly: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPresetSelect: (preset: String, startDate: String?, endDate: String?) -> Unit = { _, _, _ -> },
    onChartTabSelect: (tab: String) -> Unit = {},
    onExportExcel: () -> Unit = {},
    onShareExcel: () -> Unit = {},
    onOpenCustomerLedgers: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var customStartInput by remember { mutableStateOf("") }
    var customEndInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reports & Analytics",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Green)
                        )
                        Text(
                            text = "Live MySQL Financial Analytics",
                            fontSize = 12.sp,
                            color = Muted
                        )
                    }
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Reports", tint = Blue)
                }
            }
        }

        // Export & Ledger Actions Card
        item {
            ReportsExportActionsCard(
                exportState = exportState,
                onExportExcel = onExportExcel,
                onShareExcel = onShareExcel,
                onOpenCustomerLedgers = onOpenCustomerLedgers
            )
        }

        // State-based content
        when (reportsState) {
            is ReportsUiState.Loading -> {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Blue)
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "Fetching live financial reports from MySQL...",
                                fontWeight = FontWeight.SemiBold,
                                color = Navy,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Aggregating payments, dues, installments and charts.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is ReportsUiState.Error -> {
                item {
                    Surface(
                        color = Color(0xFFFFF5F5),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Crimson.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Crimson, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Unable to load reports", fontWeight = FontWeight.Bold, color = Crimson, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(reportsState.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            is ReportsUiState.Success -> {
                val reports = reportsState.reports
                val summary = reports.summary
                val selectedPreset = reportsState.selectedPreset
                val selectedChartTab = reportsState.selectedChartTab

                // Date Filter Chips
                item {
                    DateRangeFilterBar(
                        selectedPreset = selectedPreset,
                        onSelectPreset = { preset ->
                            if (preset == "custom") {
                                showCustomDateDialog = true
                            } else {
                                onPresetSelect(preset, null, null)
                            }
                        }
                    )
                }

                // If filter is active, show filter summary badge
                if (selectedPreset != "all") {
                    item {
                        Surface(
                            color = Color(0xFFEAF1FE),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Blue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Filtered Collection (${getPresetLabel(selectedPreset)})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Blue
                                    )
                                    Text(
                                        text = "${summary.filteredPaymentsCount} payment(s) in this period",
                                        fontSize = 11.sp,
                                        color = Muted
                                    )
                                }
                                Text(
                                    text = rupees(summary.filteredCollection),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Blue
                                )
                            }
                        }
                    }
                }

                // 6 Main KPI Cards
                item {
                    Text(
                        text = "Key Financial Indicators",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Today's Collection",
                                amount = summary.todayCollection,
                                icon = Icons.Default.Today,
                                accentColor = Green,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Weekly Collection",
                                amount = summary.weeklyCollection,
                                icon = Icons.Default.CalendarViewWeek,
                                accentColor = Blue,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Monthly Collection",
                                amount = summary.monthlyCollection,
                                icon = Icons.Default.CalendarMonth,
                                accentColor = Navy,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Total Collected",
                                amount = summary.totalCollected,
                                icon = Icons.Default.AccountBalanceWallet,
                                accentColor = Green,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiMetricCard(
                                title = "Total Pending",
                                amount = summary.totalPending,
                                icon = Icons.Default.Schedule,
                                accentColor = Orange,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Total Overdue",
                                amount = summary.totalOverdue,
                                icon = Icons.Default.Warning,
                                accentColor = Crimson,
                                subtitle = "${summary.overdueCustomersCount} account(s) overdue",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Collection Trend Charts Section
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Collection Trends & Charts",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }

                item {
                    CollectionChartsCard(
                        dailyChart = reports.dailyChart,
                        weeklyChart = reports.weeklyChart,
                        monthlyChart = reports.monthlyChart,
                        selectedTab = selectedChartTab,
                        onTabSelect = onChartTabSelect
                    )
                }

                // Cycle Breakdown Section
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Collection Cycles Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }

                item {
                    val dailyCycle = reports.cycleBreakdown["daily"] ?: CycleReportDto(cycle = "Daily")
                    val weeklyCycle = reports.cycleBreakdown["weekly"] ?: CycleReportDto(cycle = "Weekly")
                    val monthlyCycle = reports.cycleBreakdown["monthly"] ?: CycleReportDto(cycle = "Monthly")

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CycleDetailReportCard(
                            title = "Daily Cycle",
                            icon = Icons.Default.Today,
                            report = dailyCycle,
                            onClick = onDaily
                        )
                        CycleDetailReportCard(
                            title = "Weekly Cycle",
                            icon = Icons.Default.CalendarViewWeek,
                            report = weeklyCycle,
                            onClick = onWeekly
                        )
                        CycleDetailReportCard(
                            title = "Monthly Cycle",
                            icon = Icons.Default.CalendarMonth,
                            report = monthlyCycle,
                            onClick = onMonthly
                        )
                    }
                }

                // Payment Methods Breakdown
                item {
                    Spacer(Modifier.height(4.dp))
                    PaymentMethodsCard(methods = reports.paymentMethods, totalCollected = summary.totalCollected)
                }

                // Recent Collections Feed
                if (reports.recentCollections.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Recent Collections (Live MySQL)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )
                    }

                    items(reports.recentCollections.take(10)) { item ->
                        RecentCollectionRow(item)
                    }
                }
            }

            is ReportsUiState.Idle -> {
                // Fallback initial view while loading triggers
                item {
                    SummaryCard(
                        "Collection overview",
                        "${payments.size} collection entries",
                        "Outstanding ${rupees(customers.sumOf { it.due })} · Collected ${rupees(customers.sumOf { it.paid })}"
                    )
                }
                item {
                    CycleCard("Daily Report Details", "${customers.count { it.cycle == "Daily" }} customers", "Daily totals and balances", Icons.Default.Today, onDaily)
                }
                item {
                    CycleCard("Weekly Report Details", "${customers.count { it.cycle == "Weekly" }} customers", "Weekly totals and balances", Icons.Default.CalendarViewWeek, onWeekly)
                }
                item {
                    CycleCard("Monthly Report Details", "${customers.count { it.cycle == "Monthly" }} customers", "Monthly totals and balances", Icons.Default.CalendarMonth, onMonthly)
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("Filter by Custom Date Range", fontWeight = FontWeight.Bold, color = Navy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter dates in YYYY-MM-DD format:", fontSize = 13.sp, color = Muted)
                    OutlinedTextField(
                        value = customStartInput,
                        onValueChange = { customStartInput = it },
                        label = { Text("Start Date (e.g. 2026-11-01)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customEndInput,
                        onValueChange = { customEndInput = it },
                        label = { Text("End Date (e.g. 2026-11-25)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCustomDateDialog = false
                        onPresetSelect("custom", customStartInput.trim(), customEndInput.trim())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text("Apply Filter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancel", color = Muted)
                }
            }
        )
    }
}

@Composable
private fun DateRangeFilterBar(
    selectedPreset: String,
    onSelectPreset: (String) -> Unit
) {
    val presets = listOf(
        "all" to "All Time",
        "today" to "Today",
        "week" to "This Week",
        "month" to "This Month",
        "last_30" to "Last 30 Days",
        "custom" to "Custom..."
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(presets) { (key, label) ->
            val isSelected = selectedPreset == key
            Surface(
                color = if (isSelected) Blue else Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (isSelected) Blue else Border),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectPreset(key) }
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Navy,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    accentColor: Color,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = rupees(amount),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Navy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Crimson,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CollectionChartsCard(
    dailyChart: List<DailyChartPointDto>,
    weeklyChart: List<WeeklyChartPointDto>,
    monthlyChart: List<MonthlyChartPointDto>,
    selectedTab: String,
    onTabSelect: (String) -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageBg, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChartTabButton(
                    label = "Daily",
                    isSelected = selectedTab == "daily",
                    onClick = { onTabSelect("daily") },
                    modifier = Modifier.weight(1f)
                )
                ChartTabButton(
                    label = "Weekly",
                    isSelected = selectedTab == "weekly",
                    onClick = { onTabSelect("weekly") },
                    modifier = Modifier.weight(1f)
                )
                ChartTabButton(
                    label = "Monthly",
                    isSelected = selectedTab == "monthly",
                    onClick = { onTabSelect("monthly") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            when (selectedTab) {
                "daily" -> RenderDailyBarChart(dailyChart)
                "weekly" -> RenderWeeklyBarChart(weeklyChart)
                "monthly" -> RenderMonthlyBarChart(monthlyChart)
                else -> RenderDailyBarChart(dailyChart)
            }
        }
    }
}

@Composable
private fun ChartTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Blue else Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun RenderDailyBarChart(chartData: List<DailyChartPointDto>) {
    if (chartData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No collection records in this period.", color = Muted, fontSize = 12.sp)
        }
        return
    }

    val maxAmount = (chartData.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)
    val totalPeriodAmount = chartData.sumOf { it.amount }
    val totalPayments = chartData.sumOf { it.count }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Last 14 Days Activity",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted
            )
            Text(
                text = "${rupees(totalPeriodAmount)} (${totalPayments} payments)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Blue
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            chartData.forEach { pt ->
                DailyBarItem(point = pt, maxAmount = maxAmount)
            }
        }
    }
}

@Composable
private fun DailyBarItem(point: DailyChartPointDto, maxAmount: Double) {
    val barHeightRatio = (point.amount / maxAmount).toFloat().coerceIn(0f, 1f)
    val barHeightDp = (barHeightRatio * 100).dp.coerceAtLeast(6.dp)
    val hasAmount = point.amount > 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(44.dp)
    ) {
        if (hasAmount) {
            Text(
                text = if (point.amount >= 1000) "₹${(point.amount / 1000).toInt()}k" else "₹${point.amount.toInt()}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasAmount) Blue else Muted,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .width(18.dp)
                .height(barHeightDp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(if (hasAmount) Blue else Border)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = point.displayName.substringBefore(" ").take(3) + " " + point.displayName.substringAfter(" ", ""),
            fontSize = 9.sp,
            color = Navy,
            fontWeight = if (hasAmount) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
        Text(
            text = point.dayName.take(3),
            fontSize = 8.sp,
            color = Muted,
            maxLines = 1
        )
    }
}

@Composable
private fun RenderWeeklyBarChart(chartData: List<WeeklyChartPointDto>) {
    if (chartData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No weekly records found.", color = Muted, fontSize = 12.sp)
        }
        return
    }

    val maxAmount = (chartData.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chartData.forEach { week ->
            val ratio = (week.amount / maxAmount).toFloat().coerceIn(0f, 1f)
            val hasAmount = week.amount > 0

            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = week.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (hasAmount) FontWeight.Bold else FontWeight.Medium,
                        color = Navy
                    )
                    Text(
                        text = "${rupees(week.amount)} (${week.count} txn)",
                        fontSize = 12.sp,
                        fontWeight = if (hasAmount) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasAmount) Green else Muted
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(if (hasAmount) 0.04f else 0f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (hasAmount) Blue else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderMonthlyBarChart(chartData: List<MonthlyChartPointDto>) {
    if (chartData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No monthly records found.", color = Muted, fontSize = 12.sp)
        }
        return
    }

    val maxAmount = (chartData.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chartData.forEach { month ->
            val ratio = (month.amount / maxAmount).toFloat().coerceIn(0f, 1f)
            val hasAmount = month.amount > 0

            Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = month.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (hasAmount) FontWeight.Bold else FontWeight.Medium,
                        color = Navy
                    )
                    Text(
                        text = "${rupees(month.amount)} (${month.count} txn)",
                        fontSize = 12.sp,
                        fontWeight = if (hasAmount) FontWeight.Bold else FontWeight.Normal,
                        color = if (hasAmount) Green else Muted
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(if (hasAmount) 0.04f else 0f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (hasAmount) Green else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleDetailReportCard(
    title: String,
    icon: ImageVector,
    report: CycleReportDto,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFEAF1FE),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Blue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
                        Text("${report.customerCount} Active Customers", color = Muted, fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Muted)
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Border.copy(alpha = 0.5f), thickness = 0.8.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Collected", fontSize = 10.sp, color = Muted)
                    Text(rupees(report.collectedAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Green)
                }
                Column {
                    Text("Pending", fontSize = 10.sp, color = Muted)
                    Text(rupees(report.pendingAmount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Orange)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Overdue", fontSize = 10.sp, color = Muted)
                    Text(
                        rupees(report.overdueAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (report.overdueAmount > 0) Crimson else Muted
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodsCard(methods: Map<String, Double>, totalCollected: Double) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Collection Methods Breakdown",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Spacer(Modifier.height(12.dp))

            val methodColors = mapOf(
                "Cash" to Green,
                "UPI" to Blue,
                "Bank" to Navy
            )

            listOf("Cash", "UPI", "Bank").forEach { method ->
                val amount = methods[method] ?: 0.0
                val ratio = if (totalCollected > 0) (amount / totalCollected).toFloat() else 0f
                val pct = if (totalCollected > 0) (amount / totalCollected * 100).toInt() else 0
                val col = methodColors[method] ?: Blue

                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(method, fontSize = 12.sp, color = Navy, fontWeight = FontWeight.Medium)
                        Text(
                            "${rupees(amount)} ($pct%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = col
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Border)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(col)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCollectionRow(item: RecentCollectionDto) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Green.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(item.customerName, fontWeight = FontWeight.Bold, color = Navy, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.paymentDate, fontSize = 11.sp, color = Muted)
                        Text("•", fontSize = 10.sp, color = Muted)
                        Text(item.method, fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Medium)
                        Text("•", fontSize = 10.sp, color = Muted)
                        Text(item.cycle, fontSize = 11.sp, color = Muted)
                    }
                }
            }

            Text(
                text = "+${rupees(item.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Green
            )
        }
    }
}

private fun getPresetLabel(preset: String): String {
    return when (preset) {
        "today" -> "Today"
        "week" -> "This Week"
        "month" -> "This Month"
        "last_30" -> "Last 30 Days"
        "custom" -> "Custom Range"
        else -> "All Time"
    }
}

@Composable
fun ReportDetailPage(
    title: String,
    customers: List<Customer>,
    payments: List<String>,
    cycleReport: CycleReportDto? = null,
    onBack: () -> Unit
) {
    val items = if (cycleReport != null) {
        listOf(
            "Active Customers: ${cycleReport.customerCount}",
            "Collected Amount: ${rupees(cycleReport.collectedAmount)}",
            "Pending Balance: ${rupees(cycleReport.pendingAmount)}",
            "Overdue Shortfall: ${rupees(cycleReport.overdueAmount)}",
            "Total Portfolio Due: ${rupees(cycleReport.collectedAmount + cycleReport.pendingAmount)}"
        )
    } else {
        listOf(
            "Customers: ${customers.size}",
            "Total amount: ${rupees(customers.sumOf { it.amount })}",
            "Collected: ${rupees(customers.sumOf { it.paid })}",
            "Remaining: ${rupees(customers.sumOf { it.due })}",
            "All collection entries in app: ${payments.size}"
        )
    }

    SimpleListPage(
        title = title,
        subtitle = if (cycleReport != null) "Live MySQL cycle breakdown" else "Summary from locally saved records",
        rows = items,
        onPrimary = onBack,
        primaryLabel = "Back to Reports"
    )
}

@Composable
private fun ReportsExportActionsCard(
    exportState: ExportUiState,
    onExportExcel: () -> Unit,
    onShareExcel: () -> Unit,
    onOpenCustomerLedgers: () -> Unit
) {
    val isExporting = exportState is ExportUiState.Exporting
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Excel Export & Customer Ledgers", fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onExportExcel,
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export Excel", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onShareExcel,
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share Excel", fontSize = 12.sp)
                }
            }
            OutlinedButton(
                onClick = onOpenCustomerLedgers,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Individual Customer Ledgers & PDF", fontSize = 12.sp)
            }
            when (exportState) {
                is ExportUiState.Exporting -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Blue)
                        Text(exportState.label, fontSize = 11.sp, color = Muted)
                    }
                }
                is ExportUiState.Success -> {
                    Text(exportState.summary, fontSize = 11.sp, color = Green, fontWeight = FontWeight.Medium)
                }
                is ExportUiState.Error -> {
                    Text(exportState.message, fontSize = 11.sp, color = Crimson)
                }
                else -> {}
            }
        }
    }
}
