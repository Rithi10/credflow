package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.dto.CollectionForecastDto
import com.example.credflow.data.remote.dto.ForecastCustomerDto
import com.example.credflow.data.remote.dto.ForecastHorizonDto
import com.example.credflow.data.remote.dto.TimelineDayDto
import com.example.credflow.ui.viewmodel.ForecastUiState

@Composable
fun CollectionForecastPage(
    forecastState: ForecastUiState,
    onRefresh: () -> Unit = {},
    onPeriodSelect: (String) -> Unit = {},
    onCustomerClick: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Collection Forecast",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = "Empirical Recovery & Cash-Flow Projection",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            }
        }

        when (forecastState) {
            is ForecastUiState.Loading -> {
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
                                text = "Calculating collection forecast...",
                                fontWeight = FontWeight.SemiBold,
                                color = Navy,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Analyzing live MySQL schedules and historical repayment rates",
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is ForecastUiState.Error -> {
                item {
                    Surface(
                        color = Color(0xFFFFF2F2),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFF5C2C2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFC92A2A),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Failed to Load Forecast",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC92A2A),
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = forecastState.message,
                                color = Navy,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Retry Connection", color = Color.White)
                            }
                        }
                    }
                }
            }

            is ForecastUiState.Empty -> {
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
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Muted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No Active Installments",
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = forecastState.message,
                                color = Muted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is ForecastUiState.Idle -> {
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Blue, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Ready to Forecast Collections", fontWeight = FontWeight.Bold, color = Navy)
                            Spacer(Modifier.height(4.dp))
                            Text("Tap refresh to calculate expected recoveries.", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                                Text("Load Forecast", color = Color.White)
                            }
                        }
                    }
                }
            }

            is ForecastUiState.Success -> {
                val forecast = forecastState.forecast
                val selectedKey = forecastState.selectedPeriod
                val activeHorizon = forecast.getHorizon(selectedKey)

                // 1. Prediction Method Pill / Status Badge
                item {
                    MethodBadgeCard(forecast = forecast)
                }

                // 2. Insufficient Data Warning (if applicable)
                if (!forecast.isSufficientData || forecast.isRuleBased) {
                    item {
                        InsufficientDataNotice(forecast = forecast)
                    }
                }

                // 3. Horizon Selector (Tomorrow, Next 7 Days, Next 30 Days)
                item {
                    HorizonSelectorRow(
                        selectedKey = selectedKey,
                        onSelect = onPeriodSelect
                    )
                }

                // 4. Main KPI Hero Card for selected horizon
                item {
                    HorizonKpiCard(
                        selectedKey = selectedKey,
                        horizon = activeHorizon,
                        timestamp = forecast.dataTimestamp
                    )
                }

                // 5. Customer-Wise Forecast Details for Selected Horizon
                item {
                    CustomerForecastSection(
                        horizon = activeHorizon,
                        selectedKey = selectedKey,
                        onCustomerClick = onCustomerClick
                    )
                }

                // 6. Cycle Breakdown (Daily, Weekly, Monthly)
                item {
                    CycleBreakdownSection(forecast = forecast)
                }

                // 7. Weekly Monday - Sunday Distribution
                item {
                    WeeklyDistributionSection(forecast = forecast)
                }

                // 8. 30-Day Daily Timeline Projection
                if (forecast.timeline.isNotEmpty()) {
                    item {
                        TimelineProjectionSection(timeline = forecast.timeline)
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Prediction Method Badge
// -------------------------------------------------------------
@Composable
private fun MethodBadgeCard(forecast: CollectionForecastDto) {
    val isEmpirical = forecast.isEmpirical
    val bgColor = if (isEmpirical) Color(0xFFE6F7F0) else Color(0xFFEFF5FF)
    val strokeColor = if (isEmpirical) Color(0xFF86E3CE) else Color(0xFFBDD7FF)
    val textColor = if (isEmpirical) Color(0xFF0F6848) else Color(0xFF1B4FA8)

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, strokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEmpirical) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEmpirical) "Empirical Realization Model" else "Rule-Based Cash-Flow Model",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = textColor
                )
                Text(
                    text = forecast.predictionMethodLabel,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Insufficient Data Warning Notice
// -------------------------------------------------------------
@Composable
private fun InsufficientDataNotice(forecast: CollectionForecastDto) {
    Surface(
        color = Color(0xFFFFF9E6),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFFE08A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Statistical Confidence Notice",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Orange
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Portfolio is maturing. Baseline recovery percentages are applied to live MySQL installments until 15 historical matured installments are reached.",
                fontSize = 11.sp,
                color = Navy.copy(alpha = 0.85f)
            )
            if (forecast.insufficientDataReasons.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                forecast.insufficientDataReasons.forEach { reason ->
                    Text(
                        text = "• $reason",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Navy
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Horizon Selector Tabs
// -------------------------------------------------------------
@Composable
private fun HorizonSelectorRow(
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    val periods = listOf(
        Triple("tomorrow", "Tomorrow", "1 Day"),
        Triple("next_7_days", "Next 7 Days", "1 Week"),
        Triple("next_30_days", "Next 30 Days", "1 Month")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { (key, title, subtitle) ->
            val isSelected = selectedKey == key
            Surface(
                color = if (isSelected) Blue else Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) Blue else Border),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(key) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else Navy
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Muted
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Horizon Main KPI Hero Card
// -------------------------------------------------------------
@Composable
private fun HorizonKpiCard(
    selectedKey: String,
    horizon: ForecastHorizonDto,
    timestamp: String
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedKey) {
                        "tomorrow" -> "Tomorrow's Expected Collection"
                        "next_7_days" -> "Next 7 Days Expected Collection"
                        else -> "Next 30 Days Expected Collection"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted
                )
                if (horizon.targetDate.isNotBlank()) {
                    Text(
                        text = horizon.targetDate,
                        fontSize = 11.sp,
                        color = Blue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Primary Expected Collection Amount
            Text(
                text = rupees(horizon.expectedAmount),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Navy
            )

            Spacer(Modifier.height(6.dp))

            // Confidence Range Pill
            Surface(
                color = Color(0xFFF3F6FA),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confidence Range:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Muted
                    )
                    Text(
                        text = "${rupees(horizon.lowEstimate)}  —  ${rupees(horizon.highEstimate)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = Border, thickness = 0.8.dp)
            Spacer(Modifier.height(14.dp))

            // Metrics row: Scheduled Amount & Customer Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gross Scheduled", fontSize = 11.sp, color = Muted)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = rupees(horizon.scheduledAmount),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Expected Customers", fontSize = 11.sp, color = Muted)
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${horizon.expectedCustomerCount} customers",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                }
            }

            if (timestamp.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Live MySQL timestamp: $timestamp",
                    fontSize = 10.sp,
                    color = Muted.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Customer-Wise Forecast Section
// -------------------------------------------------------------
@Composable
private fun CustomerForecastSection(
    horizon: ForecastHorizonDto,
    selectedKey: String,
    onCustomerClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scheduled Customers",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Navy
            )
            Text(
                text = "${horizon.customers.size} accounts",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Blue
            )
        }

        Spacer(Modifier.height(10.dp))

        if (horizon.customers.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No customer installments scheduled for this period.",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                horizon.customers.forEach { customer ->
                    CustomerForecastCard(customer = customer, onClick = { onCustomerClick(customer.customerId) })
                }
            }
        }
    }
}

@Composable
private fun CustomerForecastCard(
    customer: ForecastCustomerDto,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cycle avatar
            Surface(
                color = when (customer.cycle) {
                    "Daily" -> Color(0xFFEAF1FE)
                    "Weekly" -> Color(0xFFE6F7F0)
                    else -> Color(0xFFFFF4E5)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = customer.cycle.take(1),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when (customer.cycle) {
                            "Daily" -> Blue
                            "Weekly" -> Green
                            else -> Orange
                        }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Navy
                )
                Text(
                    text = "${customer.cycle} • Phone: ${customer.phone.ifBlank { "N/A" }}",
                    fontSize = 11.sp,
                    color = Muted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rupees(customer.expectedAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Navy
                )
                Text(
                    text = "Due: ${rupees(customer.amountDue)}",
                    fontSize = 11.sp,
                    color = Muted
                )
            }

            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
        }
    }
}

// -------------------------------------------------------------
// Component: Cycle Breakdown (Daily, Weekly, Monthly)
// -------------------------------------------------------------
@Composable
private fun CycleBreakdownSection(forecast: CollectionForecastDto) {
    val cycles = listOf(
        Triple("Daily", forecast.cycleBreakdown["daily"], Blue),
        Triple("Weekly", forecast.cycleBreakdown["weekly"], Green),
        Triple("Monthly", forecast.cycleBreakdown["monthly"], Orange)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cycle Breakdown (Next 30 Days)",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Navy
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cycles.forEach { (title, data, accentColor) ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = accentColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = rupees(data?.expectedAmount ?: 0.0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy
                        )
                        Text(
                            text = "Sched: ${rupees(data?.scheduledAmount ?: 0.0)}",
                            fontSize = 10.sp,
                            color = Muted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${data?.customerCount ?: 0} cust.",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Muted
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: Weekly Monday - Sunday Distribution
// -------------------------------------------------------------
@Composable
private fun WeeklyDistributionSection(forecast: CollectionForecastDto) {
    val weeklyData = forecast.cycleBreakdown["weekly"]
    val distribution = weeklyData?.weekdayDistribution ?: emptyMap()
    val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Weekly Collection by Weekday",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Navy
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                weekdays.forEachIndexed { index, day ->
                    val amt = distribution[day] ?: 0.0
                    val isNonZero = amt > 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isNonZero) Green else Color(0xFFD0D7E2))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = day,
                                fontWeight = if (isNonZero) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isNonZero) Navy else Muted
                            )
                        }
                        Text(
                            text = rupees(amt),
                            fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isNonZero) Navy else Muted
                        )
                    }
                    if (index < weekdays.size - 1) {
                        Divider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Component: 30-Day Daily Projection Timeline
// -------------------------------------------------------------
@Composable
private fun TimelineProjectionSection(timeline: List<TimelineDayDto>) {
    var expanded by remember { mutableStateOf(false) }
    val displayList = if (expanded) timeline else timeline.take(7)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Day-by-Day Projection",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Navy
            )
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    text = if (expanded) "Show 7 Days" else "Show All 30 Days",
                    fontSize = 12.sp,
                    color = Blue
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                displayList.forEachIndexed { index, day ->
                    val isNonZero = day.scheduledAmount > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${day.date} (${day.dayName.take(3)})${if (day.expectedCustomerCount > 0) " • ${day.expectedCustomerCount} cust." else ""}",
                                fontWeight = if (isNonZero) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (isNonZero) Navy else Muted
                            )
                            if (isNonZero) {
                                Text(
                                    text = "Range: ${rupees(day.lowEstimate)} - ${rupees(day.highEstimate)}",
                                    fontSize = 10.sp,
                                    color = Muted
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = rupees(day.expectedAmount),
                                fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isNonZero) Navy else Muted
                            )
                            if (isNonZero) {
                                Text(
                                    text = "Sched: ${rupees(day.scheduledAmount)}",
                                    fontSize = 10.sp,
                                    color = Muted
                                )
                            }
                        }
                    }
                    if (index < displayList.size - 1) {
                        Divider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

