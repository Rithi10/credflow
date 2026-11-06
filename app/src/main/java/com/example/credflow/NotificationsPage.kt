package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.LoanScheduleCalculator
import com.example.credflow.domain.model.Payment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min

data class CustomerNotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val dateText: String,
    val status: String, // "PAID", "PENDING", "OVERDUE", "COMPLETED", "UPCOMING"
    val customer: Customer? = null,
    val targetAction: NotificationTargetAction
)

enum class NotificationTargetAction {
    CUSTOMER_DETAILS,
    PAYMENT_HISTORY,
    OVERDUE_CUSTOMERS,
    DAILY_REPORT
}

private fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}

private fun getTomorrowDateString(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(cal.time)
}

@Composable
fun NotificationsPage(
    customers: List<Customer>,
    payments: List<Payment>,
    onSelectCustomer: (Customer) -> Unit,
    onOpenPaymentHistory: () -> Unit,
    onOpenOverdue: () -> Unit,
    onOpenDailyReport: () -> Unit
) {
    val todayStr = remember { getTodayDateString() }
    val tomorrowStr = remember { getTomorrowDateString() }

    // 1. Calculate Today's Collection Data
    val todayPayments = remember(payments, todayStr) {
        payments.filter { it.paymentDate.startsWith(todayStr) }
    }
    val todayTotalCollected = remember(todayPayments) {
        todayPayments.sumOf { it.amount }
    }
    val todayPaidCustomerIds = remember(todayPayments) {
        todayPayments.map { it.customerId }.toSet()
    }
    val todayPaidCustomersCount = todayPaidCustomerIds.size

    val customersDueToday = remember(customers) {
        customers.filter { LoanScheduleCalculator.isDueToday(it) }
    }
    val todayPendingCustomers = remember(customersDueToday, todayPaidCustomerIds) {
        customersDueToday.filter { it.due > 0 && !todayPaidCustomerIds.contains(it.id) }
    }
    val todayPendingCustomersCount = todayPendingCustomers.size
    val todayPendingAmount = remember(todayPendingCustomers) {
        todayPendingCustomers.sumOf {
            val instAmount = if (it.installmentAmount > 0) it.installmentAmount else it.amount
            min(it.due, instAmount)
        }
    }

    val overdueCustomersWithDays = remember(customers) {
        customers.mapNotNull { cust ->
            val (isOverdue, days) = LoanScheduleCalculator.getOverdueInfo(cust)
            if (isOverdue && cust.due > 0) cust to days else null
        }
    }
    val overdueCount = overdueCustomersWithDays.size
    val overdueTotalAmount = remember(overdueCustomersWithDays) {
        overdueCustomersWithDays.sumOf { it.first.due }
    }

    // Collection Breakdown by Cycle
    val dailyCollected = remember(todayPayments, customers) {
        todayPayments.filter { p ->
            customers.find { it.id == p.customerId }?.cycle.equals("Daily", true)
        }.sumOf { it.amount }
    }
    val weeklyCollected = remember(todayPayments, customers) {
        todayPayments.filter { p ->
            customers.find { it.id == p.customerId }?.cycle.equals("Weekly", true)
        }.sumOf { it.amount }
    }
    val monthlyCollected = remember(todayPayments, customers) {
        todayPayments.filter { p ->
            customers.find { it.id == p.customerId }?.cycle.equals("Monthly", true)
        }.sumOf { it.amount }
    }

    // 2. Generate Dynamic Customer Notifications
    val notificationItems = remember(customers, payments, todayStr, tomorrowStr) {
        val list = mutableListOf<CustomerNotificationItem>()

        // A. Payments Received Today
        for (p in todayPayments) {
            val cust = customers.find { it.id == p.customerId }
            val name = cust?.name ?: p.customerName.ifBlank { "Customer #${p.customerId}" }
            list.add(
                CustomerNotificationItem(
                    id = "pay_${p.id}_${p.paymentDate}",
                    title = "Payment Received: $name",
                    description = "$name paid ${rupees(p.amount)} today via ${p.note}.",
                    dateText = "Today (${p.paymentDate.takeLast(8)})",
                    status = "PAID",
                    customer = cust,
                    targetAction = NotificationTargetAction.PAYMENT_HISTORY
                )
            )
        }

        // B. Overdue Customer Alerts
        for ((cust, days) in overdueCustomersWithDays) {
            list.add(
                CustomerNotificationItem(
                    id = "overdue_${cust.id}",
                    title = "Overdue Alert: ${cust.name}",
                    description = "${cust.name}'s payment of ${rupees(cust.due)} is overdue by $days day(s).",
                    dateText = "Overdue ($days days)",
                    status = "OVERDUE",
                    customer = cust,
                    targetAction = NotificationTargetAction.OVERDUE_CUSTOMERS
                )
            )
        }

        // C. Pending Customers Due Today
        for (cust in todayPendingCustomers) {
            val instAmount = if (cust.installmentAmount > 0) cust.installmentAmount else cust.amount
            val dueTodayAmount = min(cust.due, instAmount)
            list.add(
                CustomerNotificationItem(
                    id = "pending_${cust.id}",
                    title = "Installment Pending: ${cust.name}",
                    description = "${cust.name}'s installment of ${rupees(dueTodayAmount)} is pending today (${cust.cycle} collection).",
                    dateText = "Due Today ($todayStr)",
                    status = "PENDING",
                    customer = cust,
                    targetAction = NotificationTargetAction.CUSTOMER_DETAILS
                )
            )
        }

        // D. Completed Loans
        for (cust in customers) {
            if (cust.due <= 0.001 && cust.paid > 0) {
                list.add(
                    CustomerNotificationItem(
                        id = "completed_${cust.id}",
                        title = "Loan Repayment Completed: ${cust.name}",
                        description = "${cust.name} completed loan repayment in full! Total paid: ${rupees(cust.paid)}.",
                        dateText = "Paid in Full",
                        status = "COMPLETED",
                        customer = cust,
                        targetAction = NotificationTargetAction.CUSTOMER_DETAILS
                    )
                )
            }
        }

        // E. Upcoming Installments Due Tomorrow
        for (cust in customers) {
            if (cust.due > 0) {
                val nextDueDate = LoanScheduleCalculator.getNextDueDate(cust)
                if (nextDueDate == tomorrowStr) {
                    val instAmount = if (cust.installmentAmount > 0) cust.installmentAmount else cust.amount
                    list.add(
                        CustomerNotificationItem(
                            id = "upcoming_${cust.id}",
                            title = "Upcoming Due Tomorrow: ${cust.name}",
                            description = "${cust.name}'s next installment of ${rupees(instAmount)} is due tomorrow ($tomorrowStr).",
                            dateText = "Due Tomorrow",
                            status = "UPCOMING",
                            customer = cust,
                            targetAction = NotificationTargetAction.CUSTOMER_DETAILS
                        )
                    )
                }
            }
        }

        list
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("Notifications", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Live daily collection summary & customer alerts", color = Muted, fontSize = 12.sp)
            }
        }

        // TODAY'S COLLECTION SUMMARY CARD (Clickable -> Daily Report)
        item {
            Surface(
                color = Navy,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { onOpenDailyReport() }
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Today, null, tint = Color(0xFFCAD9EF))
                            Text("TODAY'S COLLECTION", color = Color(0xFFCAD9EF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = Color(0xFF235DC5), shape = RoundedCornerShape(8.dp)) {
                            Text(todayStr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Text(rupees(todayTotalCollected), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    HorizontalDivider(color = Color(0xFF234B82), thickness = 0.8.dp)

                    // Grid Summary Stats
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryStatItem("Paid Today", "$todayPaidCustomersCount customers", Color(0xFF81C784))
                        SummaryStatItem("Pending Today", "$todayPendingCustomersCount (${rupees(todayPendingAmount)})", Color(0xFFFFB74D))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SummaryStatItem("Overdue", "$overdueCount (${rupees(overdueTotalAmount)})", Color(0xFFE57373))
                        SummaryStatItem("Installments Paid", "${todayPayments.size} entries", Color.White)
                    }

                    HorizontalDivider(color = Color(0xFF234B82), thickness = 0.8.dp)

                    // Daily / Weekly / Monthly Breakdown
                    Text("Today's Cycle Breakdown", color = Color(0xFFCAD9EF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Daily: ${rupees(dailyCollected)}", color = Color.White, fontSize = 12.sp)
                        Text("Weekly: ${rupees(weeklyCollected)}", color = Color.White, fontSize = 12.sp)
                        Text("Monthly: ${rupees(monthlyCollected)}", color = Color.White, fontSize = 12.sp)
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Tap to open Today's Collection Report", color = Color(0xFFCAD9EF), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFFCAD9EF), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // CUSTOMER ACTIVITY & ALERTS LIST
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Customer Activity & Alerts", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                Surface(color = Color(0xFFEAF1FE), shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "${notificationItems.size} items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Blue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        if (notificationItems.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NotificationsNone, null, tint = Blue, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No recent notifications", color = Navy, fontWeight = FontWeight.Bold)
                        Text("All collection entries and customer updates will appear here dynamically.", color = Muted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(notificationItems, key = { it.id }) { item ->
                NotificationCard(
                    item = item,
                    onClick = {
                        when (item.targetAction) {
                            NotificationTargetAction.CUSTOMER_DETAILS -> {
                                if (item.customer != null) {
                                    onSelectCustomer(item.customer)
                                } else {
                                    onOpenDailyReport()
                                }
                            }
                            NotificationTargetAction.PAYMENT_HISTORY -> onOpenPaymentHistory()
                            NotificationTargetAction.OVERDUE_CUSTOMERS -> onOpenOverdue()
                            NotificationTargetAction.DAILY_REPORT -> onOpenDailyReport()
                        }
                    }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun SummaryStatItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color(0xFFCAD9EF), fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotificationCard(item: CustomerNotificationItem, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.title, fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                NotificationBadge(item.status)
            }

            Text(item.description, color = Muted, fontSize = 13.sp, lineHeight = 18.sp)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.dateText, fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("View", fontSize = 11.sp, color = Blue, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, tint = Blue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun NotificationBadge(status: String) {
    val (bg, fg) = when (status) {
        "PAID", "COMPLETED" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "PENDING" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        "OVERDUE" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            status,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
