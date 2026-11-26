package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.LedgerInstallmentDto
import com.example.credflow.data.remote.dto.LedgerPaymentDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.ui.viewmodel.CustomerLedgerUiState
import com.example.credflow.ui.viewmodel.ExportUiState

private val LedgerCrimson = Color(0xFFC62828)

@Composable
fun CustomerLedgerPage(
    ledgerState: CustomerLedgerUiState,
    exportState: ExportUiState = ExportUiState.Idle,
    selectedCustomer: Customer?,
    customers: List<Customer> = emptyList(),
    onSelectCustomer: (Customer) -> Unit = {},
    onRefresh: (Int) -> Unit = {},
    onSelectTab: (String) -> Unit = {},
    onDownloadPdf: (Int) -> Unit = {},
    onSharePdf: (Int) -> Unit = {},
    onViewPaymentReceipt: (Int) -> Unit = {},
    onSharePaymentReceipt: (Int) -> Unit = {},
    onCloseLoan: (Customer) -> Unit = {},
    onCollect: (Customer) -> Unit = {},
    onBack: () -> Unit = {}
) {
    if (selectedCustomer == null) {
        CustomerLedgerPicker(
            customers = customers,
            onSelectCustomer = onSelectCustomer,
            onBack = onBack
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (ledgerState) {
            is CustomerLedgerUiState.Idle,
            is CustomerLedgerUiState.Loading -> {
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
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Fetching customer ledger from MySQL...",
                                fontWeight = FontWeight.SemiBold,
                                color = Navy,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Loading loan balance, installments, overdue dues, and payments for ${selectedCustomer.name}.",
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            is CustomerLedgerUiState.Error -> {
                item {
                    Surface(
                        color = Color(0xFFFFF5F5),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, LedgerCrimson.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = LedgerCrimson,
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Unable to load customer ledger",
                                fontWeight = FontWeight.Bold,
                                color = LedgerCrimson,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = ledgerState.message,
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onBack) {
                                    Text("Go Back")
                                }
                                Button(
                                    onClick = { onRefresh(selectedCustomer.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            is CustomerLedgerUiState.Success -> {
                val ledger = ledgerState.ledger
                val summary = ledger.summary
                val customer = ledger.customer
                val selectedTab = ledgerState.selectedTab

                // 1. Header & Export Actions
                item {
                    LedgerHeaderCard(
                        ledger = ledger,
                        exportState = exportState,
                        onRefresh = { onRefresh(customer.id) },
                        onDownloadPdf = { onDownloadPdf(customer.id) },
                        onSharePdf = { onSharePdf(customer.id) }
                    )
                }

                // 2. Loan & Interest Details
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Loan & Interest Overview", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                            HorizontalDivider(color = Border, thickness = 0.5.dp)
                            LedgerMetricRow("Loan Amount (Principal)", rupees(summary.loanAmount))
                            LedgerMetricRow("Interest Rate", "${summary.interestRate}% (${summary.interestType})")
                            LedgerMetricRow("Total Interest", rupees(summary.totalInterest))
                            LedgerMetricRow("Total Repayment Due", rupees(summary.totalRepayment), isBold = true)
                            LedgerMetricRow(
                                "Installment Amount",
                                "${rupees(summary.installmentAmount)} / ${customer.cycle.lowercase()}",
                                valueColor = Blue
                            )
                            if (customer.firstDueDate.isNotBlank() || customer.finalDueDate.isNotBlank()) {
                                LedgerMetricRow(
                                    "Schedule Window",
                                    "${customer.firstDueDate.ifBlank { "-" }} to ${customer.finalDueDate.ifBlank { "-" }}"
                                )
                            }
                        }
                    }
                }

                // 3. Balance, Paid & Overdue Summary
                item {
                    val progress = if (summary.totalRepayment > 0.0) {
                        (summary.totalPaid / summary.totalRepayment).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Payment & Balance Summary", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                            HorizontalDivider(color = Border, thickness = 0.5.dp)
                            LedgerMetricRow("Total Paid", rupees(summary.totalPaid), isBold = true, valueColor = Green)
                            LedgerMetricRow(
                                "Remaining Balance",
                                rupees(summary.remainingBalance),
                                isBold = true,
                                valueColor = if (summary.remainingBalance > 0) LedgerCrimson else Green
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Green,
                                trackColor = Border
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LedgerStatChip(
                                    label = "Paid",
                                    count = summary.paidInstallmentsCount,
                                    sub = "of ${summary.totalInstallments}",
                                    bg = Color(0xFFE8F5E9),
                                    fg = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                LedgerStatChip(
                                    label = "Pending",
                                    count = summary.pendingInstallmentsCount,
                                    sub = rupees(summary.pendingAmount),
                                    bg = Color(0xFFFFF3E0),
                                    fg = Orange,
                                    modifier = Modifier.weight(1f)
                                )
                                LedgerStatChip(
                                    label = "Overdue",
                                    count = summary.overdueInstallmentsCount,
                                    sub = rupees(summary.overdueAmount),
                                    bg = if (summary.overdueInstallmentsCount > 0) Color(0xFFFFEBEE) else Color(0xFFF1F5F9),
                                    fg = if (summary.overdueInstallmentsCount > 0) LedgerCrimson else Muted,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            LedgerMetricRow("Next Due Date", summary.nextDueDate.ifBlank { "-" })
                            if (summary.overdueDays > 0) {
                                LedgerMetricRow(
                                    "Overdue Duration",
                                    "${summary.overdueDays} day(s) overdue",
                                    isBold = true,
                                    valueColor = LedgerCrimson
                                )
                            }
                        }
                    }
                }

                // 4. Ledger Section Tabs
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LedgerTabButton(
                            label = "Schedule (${ledger.installments.size})",
                            isSelected = selectedTab == "schedule",
                            onClick = { onSelectTab("schedule") },
                            modifier = Modifier.weight(1f)
                        )
                        LedgerTabButton(
                            label = "Payments (${ledger.payments.size})",
                            isSelected = selectedTab == "payments",
                            onClick = { onSelectTab("payments") },
                            modifier = Modifier.weight(1f)
                        )
                        val duesCount = ledger.overdueInstallments.size + ledger.pendingInstallments.size
                        LedgerTabButton(
                            label = "Dues ($duesCount)",
                            isSelected = selectedTab == "dues",
                            onClick = { onSelectTab("dues") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. Tab Content
                when (selectedTab) {
                    "payments" -> {
                        if (ledger.payments.isEmpty()) {
                            item {
                                EmptyCard(
                                    "No payments recorded yet",
                                    "Recorded payments for ${customer.name} will appear here with exact dates, amounts, and PDF receipts."
                                )
                            }
                        } else {
                            items(ledger.payments, key = { "pay_${it.id}" }) { payment ->
                                LedgerPaymentRow(
                                    payment = payment,
                                    onViewReceipt = { onViewPaymentReceipt(payment.id) },
                                    onShareReceipt = { onSharePaymentReceipt(payment.id) }
                                )
                            }
                        }
                    }

                    "dues" -> {
                        if (ledger.overdueInstallments.isEmpty() && ledger.pendingInstallments.isEmpty()) {
                            item {
                                EmptyCard(
                                    "No pending or overdue installments",
                                    "All scheduled installments for ${customer.name} have been settled."
                                )
                            }
                        } else {
                            if (ledger.overdueInstallments.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Overdue Installments (${ledger.overdueInstallments.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = LedgerCrimson,
                                        fontSize = 15.sp
                                    )
                                }
                                items(ledger.overdueInstallments, key = { "ov_${it.installmentNumber}" }) { inst ->
                                    LedgerInstallmentRow(inst)
                                }
                            }
                            if (ledger.pendingInstallments.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Pending Installments (${ledger.pendingInstallments.size})",
                                        fontWeight = FontWeight.Bold,
                                        color = Navy,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(ledger.pendingInstallments, key = { "pend_${it.installmentNumber}" }) { inst ->
                                    LedgerInstallmentRow(inst)
                                }
                            }
                        }
                    }

                    else -> {
                        if (ledger.installments.isEmpty()) {
                            item {
                                EmptyCard(
                                    "No installment schedule",
                                    "No scheduled installments were found for this loan."
                                )
                            }
                        } else {
                            items(ledger.installments, key = { "inst_${it.installmentNumber}" }) { inst ->
                                LedgerInstallmentRow(inst)
                            }
                        }
                    }
                }

                // 6. Settlement History (if any) & Bottom Actions
                if (ledger.settlements.isNotEmpty()) {
                    item {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Settlement History", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                                HorizontalDivider(color = Border, thickness = 0.5.dp)
                                ledger.settlements.forEach { st ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "${st.settlementType} Settlement${if (st.loanClosed) " (CLOSED)" else ""}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (st.loanClosed) Green else Navy
                                            )
                                            Text(
                                                "${st.paymentMethod} · ${st.createdAt}",
                                                fontSize = 11.sp,
                                                color = Muted
                                            )
                                        }
                                        Text(
                                            rupees(st.settlementAmount),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val isClosed = customer.loanStatus.equals("CLOSED", ignoreCase = true)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isClosed && summary.remainingBalance > 0.001) {
                            Button(
                                onClick = { onCollect(selectedCustomer) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Payments, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Record Collection")
                            }
                        }
                        if (!isClosed) {
                            OutlinedButton(
                                onClick = { onCloseLoan(selectedCustomer) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Close Loan / Settle")
                            }
                        }
                        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerPicker(
    customers: List<Customer>,
    onSelectCustomer: (Customer) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Customer Ledgers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("Select a customer to view their live MySQL ledger and export PDF.", color = Muted, fontSize = 12.sp)
        }
        if (customers.isEmpty()) {
            item {
                EmptyCard(
                    "No customers available",
                    "Add a customer first to inspect their loan ledger and export statements."
                )
            }
        } else {
            items(customers, key = { it.id }) { customer ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectCustomer(customer) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                            Text("${customer.phone} · ${customer.cycle}", color = Muted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = rupees(customer.due),
                                fontWeight = FontWeight.Bold,
                                color = if (customer.due > 0) Orange else Green,
                                fontSize = 14.sp
                            )
                            Text(if (customer.due > 0) "Balance" else "Paid", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun LedgerHeaderCard(
    ledger: CustomerLedgerDto,
    exportState: ExportUiState,
    onRefresh: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit
) {
    val customer = ledger.customer
    val isExporting = exportState is ExportUiState.Exporting

    Surface(
        color = Navy,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(customer.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${customer.phone.ifBlank { "No phone" }} · ${customer.cycle} (${customer.collectionDay})",
                        color = Color(0xFFCAD9EF),
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = when (ledger.summary.status) {
                            "CLOSED", "PAID" -> Color(0xFF2E7D32)
                            "OVERDUE" -> LedgerCrimson
                            else -> Blue
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = ledger.summary.status,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, "Refresh Ledger", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDownloadPdf,
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onSharePdf,
                    enabled = !isExporting,
                    border = BorderStroke(1.dp, Color(0xFFCAD9EF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            when (exportState) {
                is ExportUiState.Exporting -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
                        Text(exportState.label, color = Color(0xFFCAD9EF), fontSize = 11.sp)
                    }
                }
                is ExportUiState.Success -> {
                    Text("Saved: ${exportState.fileName}", color = Color(0xFFA5D6A7), fontSize = 11.sp)
                }
                is ExportUiState.Error -> {
                    Text("Export error: ${exportState.message}", color = Color(0xFFFFCDD2), fontSize = 11.sp)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun LedgerMetricRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Navy
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Muted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun LedgerStatChip(
    label: String,
    count: Int,
    sub: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = fg, fontWeight = FontWeight.SemiBold)
            Text("$count", fontSize = 16.sp, color = fg, fontWeight = FontWeight.Bold)
            Text(sub, fontSize = 10.sp, color = fg)
        }
    }
}

@Composable
private fun LedgerTabButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) Blue else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }
}

@Composable
private fun LedgerInstallmentRow(inst: LedgerInstallmentDto) {
    val (bg, fg) = when {
        inst.status.equals("Paid", ignoreCase = true) -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        inst.isOverdue -> Pair(Color(0xFFFFEBEE), LedgerCrimson)
        inst.paidAmount > 0 -> Pair(Color(0xFFFFF3E0), Orange)
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (inst.isOverdue) LedgerCrimson.copy(alpha = 0.25f) else Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (inst.isOverdue) Color(0xFFFFEBEE) else Color(0xFFEAF1FE),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#${inst.installmentNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inst.isOverdue) LedgerCrimson else Blue
                        )
                    }
                }
                Column {
                    Text(inst.dueDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                    Text(
                        "Amount: ${rupees(inst.amount)} · Paid: ${rupees(inst.paidAmount)}",
                        fontSize = 11.sp,
                        color = Muted
                    )
                    if (inst.unpaidAmount > 0.0) {
                        Text(
                            "Remaining: ${rupees(inst.unpaidAmount)}" +
                                    if (inst.isOverdue && inst.daysOverdue > 0) " · ${inst.daysOverdue}d overdue" else "",
                            fontSize = 11.sp,
                            color = if (inst.isOverdue) LedgerCrimson else Orange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = inst.status,
                    color = fg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LedgerPaymentRow(
    payment: LedgerPaymentDto,
    onViewReceipt: () -> Unit = {},
    onShareReceipt: () -> Unit = {}
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Green.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                        }
                    }
                    Column {
                        Text(payment.paymentDate, fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 13.sp)
                        Text(
                            "${payment.note.ifBlank { "Cash" }} · ${payment.receiptNumber.ifBlank { "Payment #${payment.id}" }}",
                            fontSize = 11.sp,
                            color = Muted
                        )
                    }
                }
                Text(
                    text = "+${rupees(payment.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Green
                )
            }
            if (payment.id > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Receipt PDF", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onShareReceipt,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).height(34.dp)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share Receipt", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

