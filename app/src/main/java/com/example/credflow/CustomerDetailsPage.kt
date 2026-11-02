package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.InstallmentScheduleItem
import com.example.credflow.domain.model.LoanCalculator
import com.example.credflow.domain.model.LoanScheduleCalculator

@Composable
fun CustomerDetailsPage(
    customer: Customer?,
    installments: List<InstallmentScheduleItem> = emptyList(),
    onCollect: () -> Unit,
    onHistory: () -> Unit,
    onBack: () -> Unit
) {
    if (customer == null) {
        EmptyCard("No customer selected", "Go back and select a customer.")
        return
    }

    // If backend installments haven't loaded yet or offline, compute schedule locally
    val effectiveSchedule = remember(customer, installments) {
        if (installments.isNotEmpty()) {
            installments
        } else {
            val calc = LoanCalculator.calculateLoan(
                principal = customer.amount,
                interestRate = customer.interest,
                period = customer.period,
                periodUnit = customer.periodUnit,
                frequency = customer.cycle,
                collectionDay = customer.day,
                firstDueDateStr = customer.firstDueDate
            )
            // Allocate customer.paid across items
            var remainingPaid = customer.paid
            val todayStr = java.time.LocalDate.now().toString()
            calc.schedule.map { item ->
                val alloc = kotlin.math.min(item.amount, kotlin.math.max(0.0, remainingPaid))
                remainingPaid -= alloc
                val status = when {
                    alloc >= item.amount - 0.001 -> "Paid"
                    alloc > 0.0 -> "Partial"
                    item.dueDate < todayStr -> "Overdue"
                    else -> "Pending"
                }
                item.copy(paidAmount = alloc, status = status)
            }
        }
    }

    val (isOverdue, overdueDays) = LoanScheduleCalculator.getOverdueInfo(customer)
    val nextDueDateStr = LoanScheduleCalculator.getNextDueDate(customer)

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Customer Header Card
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(customer.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
                        StatusPill(
                            status = when {
                                customer.due <= 0.0 -> "PAID"
                                isOverdue -> "OVERDUE"
                                else -> "ACTIVE"
                            }
                        )
                    }
                    Text(customer.phone, fontSize = 14.sp, color = Muted)
                    Text("${customer.cycle} Collections · ${customer.day}", fontSize = 13.sp, color = Navy, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Loan Overview Card (Requirement 7)
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Loan Overview", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    DetailRow("Original Loan Amount", rupees(customer.amount))
                    DetailRow("Interest Percentage", "${customer.interest}% (${customer.interestType})")
                    DetailRow("Total Interest", rupees(customer.totalInterest))
                    DetailRow("Total Repayment", rupees(customer.totalDue), isBold = true, color = Navy)
                    DetailRow(
                        "Installment Amount",
                        "${rupees(if (customer.installmentAmount > 0) customer.installmentAmount else customer.amount)} / ${customer.cycle.lowercase()}",
                        color = Blue
                    )
                }
            }
        }

        // Repayment Progress & Balances Card (Requirement 7)
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Progress", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    DetailRow("Amount Paid", rupees(customer.paid), color = Green)
                    DetailRow("Outstanding Balance", rupees(customer.due), isBold = true, color = if (customer.due > 0) Color(0xFFC62828) else Green)
                    DetailRow("Remaining Installments", "${customer.remainingInstallments} of ${customer.totalInstallments} remaining")
                    DetailRow("Next Due Date", nextDueDateStr, color = Navy)
                    if (isOverdue && overdueDays > 0) {
                        DetailRow("Overdue Period", "$overdueDays day(s) overdue", color = Color.Red)
                    }
                }
            }
        }

        // Action Buttons
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCollect,
                    enabled = customer.due > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EditNote, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (customer.due > 0) "Collect" else "Paid in Full")
                }
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(Modifier.width(6.dp))
                    Text("History")
                }
            }
        }

        // Full Repayment Schedule Header (Requirement 7)
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Full Repayment Schedule", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                Text("${effectiveSchedule.size} Installments", fontSize = 12.sp, color = Muted)
            }
        }

        // Repayment Schedule Rows
        items(effectiveSchedule) { inst ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.8.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("#${inst.number}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy)
                            }
                        }
                        Column {
                            Text(inst.dueDate, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
                            Text(
                                "Due: ${rupees(inst.amount)}" + if (inst.paidAmount > 0) " · Paid: ${rupees(inst.paidAmount)}" else "",
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }
                    }

                    StatusBadge(inst.status)
                }
            }
        }

        // Customer Note
        if (customer.note.isNotBlank()) {
            item {
                InfoCard("Customer Note", customer.note)
            }
        }

        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to customer list")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Muted)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (color != Color.Unspecified) color else Navy
        )
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bg, fg) = when (status) {
        "PAID" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "OVERDUE" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
    }
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Text(
            status,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "paid" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "partial" -> Pair(Color(0xFFFFF3E0), Color(0xFFE65100))
        "overdue" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }
    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            status,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
