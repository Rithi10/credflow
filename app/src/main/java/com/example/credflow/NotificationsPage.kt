package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.credflow.data.remote.dto.NotificationItemDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import com.example.credflow.ui.viewmodel.NotificationsUiState

private val Crimson = Color(0xFFC53030)

@Composable
fun NotificationsPage(
    notificationsState: NotificationsUiState = NotificationsUiState.Idle,
    customers: List<Customer> = emptyList(),
    payments: List<Payment> = emptyList(),
    onSelectCustomer: (Customer) -> Unit = {},
    onSelectCustomerId: (Int) -> Unit = {},
    onMarkAsRead: (Int) -> Unit = {},
    onMarkAllAsRead: () -> Unit = {},
    onFilterSelect: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onOpenPaymentHistory: () -> Unit = {},
    onOpenOverdue: () -> Unit = {},
    onOpenDailyReport: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notifications & Reminders",
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
                            text = "Live MySQL Due Reminders & Overdue Alerts",
                            fontSize = 12.sp,
                            color = Muted
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Notifications", tint = Blue)
                    }
                }
            }
        }

        when (notificationsState) {
            is NotificationsUiState.Loading -> {
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
                                text = "Loading notifications from MySQL...",
                                fontWeight = FontWeight.SemiBold,
                                color = Navy,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Checking due dates, overdue shortfall & payment receipts.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            is NotificationsUiState.Error -> {
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
                            Text("Unable to load notifications", fontWeight = FontWeight.Bold, color = Crimson, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(notificationsState.message, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
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

            is NotificationsUiState.Success -> {
                val itemsList = notificationsState.notifications
                val unreadCount = notificationsState.unreadCount
                val selectedFilter = notificationsState.selectedFilter

                // Filter Row and Mark All Read
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NotificationFilterChips(
                            selectedFilter = selectedFilter,
                            onSelectFilter = onFilterSelect,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Unread Count Status & Mark All Read Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (unreadCount > 0) {
                            Surface(
                                color = Color(0xFFEAF1FE),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$unreadCount unread alert(s)",
                                    color = Blue,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            TextButton(onClick = onMarkAllAsRead) {
                                Icon(Icons.Default.DoneAll, null, tint = Blue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mark All Read", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "All notifications read",
                                color = Green,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (itemsList.isEmpty()) {
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
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.NotificationsNone, null, tint = Muted, modifier = Modifier.size(44.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("No notifications found", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text("There are no active alerts or reminders under this filter.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(itemsList, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onItemClick = {
                                if (!item.isRead) {
                                    onMarkAsRead(item.id)
                                }
                                val cid = item.customerId
                                if (cid != null && cid > 0) {
                                    val cust = customers.find { it.id == cid }
                                    if (cust != null) {
                                        onSelectCustomer(cust)
                                    } else {
                                        onSelectCustomerId(cid)
                                    }
                                }
                            },
                            onMarkRead = { onMarkAsRead(item.id) },
                            onOpenCustomer = {
                                if (!item.isRead) {
                                    onMarkAsRead(item.id)
                                }
                                val cid = item.customerId
                                if (cid != null && cid > 0) {
                                    val cust = customers.find { it.id == cid }
                                    if (cust != null) {
                                        onSelectCustomer(cust)
                                    } else {
                                        onSelectCustomerId(cid)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            is NotificationsUiState.Idle -> {
                // Initial fallback while triggered
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
                            CircularProgressIndicator(color = Blue)
                            Spacer(Modifier.height(14.dp))
                            Text("Connecting to Notification Centre...", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFilterChips(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        "all" to "All",
        "unread" to "Unread",
        "due" to "Due Reminders",
        "overdue" to "Overdue Alerts",
        "payment" to "Payments"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(filters) { (key, label) ->
            val isSelected = selectedFilter == key
            Surface(
                color = if (isSelected) Blue else Color.White,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (isSelected) Blue else Border),
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectFilter(key) }
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Navy,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItemDto,
    onItemClick: () -> Unit,
    onMarkRead: () -> Unit,
    onOpenCustomer: () -> Unit
) {
    val (typeBg, typeTint, typeIcon) = when (item.type) {
        "OVERDUE_ALERT" -> Triple(Color(0xFFFFF0F0), Crimson, Icons.Default.Warning)
        "PAYMENT_RECEIVED" -> Triple(Color(0xFFE6F7F2), Green, Icons.Default.CheckCircle)
        else -> Triple(Color(0xFFEAF1FE), Blue, Icons.Default.Today)
    }

    val cardBg = if (item.isRead) Color.White else Color(0xFFF8FAFF)
    val cardBorder = if (item.isRead) Border else Blue.copy(alpha = 0.4f)

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onItemClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = typeBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(typeIcon, null, tint = typeTint, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.title,
                                fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                                color = Navy,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!item.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Blue)
                                )
                            }
                        }

                        Text(
                            text = item.createdAt,
                            fontSize = 11.sp,
                            color = Muted
                        )
                    }
                }

                // Amount Pill
                if (item.amount > 0) {
                    val amountColor = when (item.type) {
                        "OVERDUE_ALERT" -> Crimson
                        "PAYMENT_RECEIVED" -> Green
                        else -> Blue
                    }
                    Text(
                        text = if (item.type == "PAYMENT_RECEIVED") "+${rupees(item.amount)}" else rupees(item.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = amountColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Body Message
            Text(
                text = item.message,
                fontSize = 12.sp,
                color = if (item.isRead) Muted else Navy,
                lineHeight = 17.sp
            )

            // Extra metrics pill row
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.overdueDays > 0) {
                        Surface(
                            color = Crimson.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${item.overdueDays} days overdue",
                                color = Crimson,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.customerCycle.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF1F4F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.customerCycle,
                                color = Muted,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.remainingBalance > 0) {
                        Text(
                            text = "Bal: ${rupees(item.remainingBalance)}",
                            fontSize = 10.sp,
                            color = Muted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!item.isRead) {
                        Text(
                            text = "Mark read",
                            fontSize = 11.sp,
                            color = Blue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(onClick = onMarkRead)
                                .padding(4.dp)
                        )
                    }
                    if (item.customerId != null && item.customerId > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(onClick = onOpenCustomer)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "View",
                                fontSize = 11.sp,
                                color = Blue,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = Blue, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
