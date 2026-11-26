package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.domain.model.Customer
import java.util.Locale

private val ClosureCrimson = Color(0xFFC62828)

@Composable
fun CloseLoanPage(
    customer: Customer?,
    isClosing: Boolean = false,
    closureError: String? = null,
    onClearError: () -> Unit = {},
    onSubmitClosure: (
        settlementType: String,
        settlementAmountStr: String,
        paymentMethod: String,
        note: String,
        closeLoan: Boolean
    ) -> Unit,
    onBack: () -> Unit
) {
    if (customer == null) {
        EmptyCard("No customer selected", "Select a customer first to close or settle their loan.")
        return
    }

    var settlementType by remember(customer.id) { mutableStateOf("FULL") }
    var settlementAmount by remember(customer.id, customer.due) {
        mutableStateOf(
            if (customer.due > 0.001) String.format(Locale.ROOT, "%.2f", customer.due) else "0.00"
        )
    }
    var paymentMethod by remember(customer.id) { mutableStateOf("Cash") }
    var settlementNote by remember(customer.id) { mutableStateOf("") }
    var localValidationError by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val parsedAmount = settlementAmount.toDoubleOrNull() ?: 0.0
    val remainingAfter = (customer.due - parsedAmount).coerceAtLeast(0.0)
    val willCloseLoan = settlementType == "FULL" || remainingAfter <= 0.001

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Loan Closure & Settlement", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
            Text("Settle remaining dues and safely close ${customer.name}'s loan.", fontSize = 12.sp, color = Muted)
        }

        // Customer & Loan Breakdown Card
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(customer.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Navy)
                            Text("${customer.phone} · ${customer.cycle}", fontSize = 12.sp, color = Muted)
                        }
                        Surface(
                            color = if (customer.isClosed) Color(0xFFE8F5E9) else Color(0xFFEAF1FE),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (customer.isClosed) "CLOSED" else "ACTIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.isClosed) Color(0xFF2E7D32) else Blue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    ClosureRow("Original Principal", rupees(customer.amount))
                    ClosureRow("Interest (${customer.interest}%)", rupees(customer.totalInterest))
                    ClosureRow("Total Loan Payable", rupees(customer.totalDue))
                    ClosureRow("Total Paid So Far", rupees(customer.paid), valueColor = Green)

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    ClosureRow("Outstanding Principal", rupees(customer.outstandingPrincipal))
                    ClosureRow("Outstanding Interest", rupees(customer.outstandingInterest))
                    ClosureRow(
                        "Remaining Balance",
                        rupees(customer.due),
                        isBold = true,
                        valueColor = if (customer.due > 0.001) ClosureCrimson else Green
                    )
                }
            }
        }

        if (customer.isClosed) {
            item {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Green.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Loan Already Closed", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 15.sp)
                        Text(
                            "This loan was closed${if (customer.closedAt.isNotBlank()) " on ${customer.closedAt}" else ""}. All payment history is preserved and no further installments will be generated.",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to Customer Details")
                }
            }
            return@LazyColumn
        }

        // Settlement Mode Selection
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Settlement Type", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = settlementType == "FULL",
                            onClick = {
                                settlementType = "FULL"
                                settlementAmount = if (customer.due > 0.001) String.format(Locale.ROOT, "%.2f", customer.due) else "0.00"
                                localValidationError = null
                                onClearError()
                            },
                            label = { Text("Full Settlement") },
                            leadingIcon = { Icon(Icons.Default.TaskAlt, null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = settlementType == "PARTIAL",
                            enabled = customer.due > 0.001,
                            onClick = {
                                settlementType = "PARTIAL"
                                if ((settlementAmount.toDoubleOrNull() ?: 0.0) >= customer.due) {
                                    settlementAmount = ""
                                }
                                localValidationError = null
                                onClearError()
                            },
                            label = { Text("Partial Settlement") },
                            leadingIcon = { Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Text(
                        text = if (settlementType == "FULL") {
                            "Full Settlement records the entire remaining balance and marks the loan as CLOSED."
                        } else {
                            "Partial Settlement records a settlement payment while keeping the loan ACTIVE until the balance reaches ₹0.00."
                        },
                        fontSize = 12.sp,
                        color = Muted
                    )

                    OutlinedTextField(
                        value = settlementAmount,
                        onValueChange = {
                            settlementAmount = it
                            localValidationError = null
                            onClearError()
                        },
                        label = { Text("Final Settlement Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = !isClosing && customer.due > 0.001,
                        trailingIcon = {
                            if (customer.due > 0.001) {
                                TextButton(onClick = {
                                    settlementAmount = String.format(Locale.ROOT, "%.2f", customer.due)
                                    localValidationError = null
                                }) {
                                    Text("Full", fontSize = 12.sp)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Method", fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "UPI", "GPay", "Bank").forEach { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(m, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = settlementNote,
                        onValueChange = { settlementNote = it },
                        label = { Text("Settlement Note (optional)") },
                        singleLine = true,
                        enabled = !isClosing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        val activeError = localValidationError ?: closureError
        if (!activeError.isNullOrBlank()) {
            item {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ClosureCrimson.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = ClosureCrimson, modifier = Modifier.size(18.dp))
                        Text(activeError, color = ClosureCrimson, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val amt = settlementAmount.toDoubleOrNull()
                    if (customer.due > 0.001 && (amt == null || amt <= 0.0)) {
                        localValidationError = "Enter a valid settlement amount greater than zero."
                        return@Button
                    }
                    val valAmt = amt ?: 0.0
                    if (valAmt > customer.due + 0.001) {
                        localValidationError = "Settlement amount exceeds remaining balance of ${rupees(customer.due)}."
                        return@Button
                    }
                    if (settlementType == "FULL" && (customer.due - valAmt) > 0.001) {
                        localValidationError = "Cannot close loan while ${rupees(customer.due - valAmt)} remains unpaid. Enter the full remaining balance (${rupees(customer.due)}) or switch to Partial Settlement."
                        return@Button
                    }
                    localValidationError = null
                    showConfirmDialog = true
                },
                enabled = !isClosing,
                colors = ButtonDefaults.buttonColors(containerColor = if (willCloseLoan) Navy else Blue),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isClosing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing Settlement...")
                } else {
                    Icon(if (willCloseLoan) Icons.Default.Lock else Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (willCloseLoan) "Review & Close Loan" else "Record Partial Settlement")
                }
            }
        }

        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isClosing) showConfirmDialog = false },
            title = {
                Text(
                    text = if (willCloseLoan) "Confirm Loan Closure" else "Confirm Partial Settlement",
                    fontWeight = FontWeight.Bold,
                    color = Navy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Customer: ${customer.name}", fontWeight = FontWeight.SemiBold)
                    Text("Settlement Type: ${if (settlementType == "FULL") "Full Settlement" else "Partial Settlement"}")
                    Text("Settlement Amount: ${rupees(parsedAmount)} via $paymentMethod", color = Green, fontWeight = FontWeight.Bold)
                    Text("Balance After Settlement: ${rupees(remainingAfter)}", fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    Text(
                        text = if (willCloseLoan) {
                            "Confirming will settle the full balance, generate a payment receipt, mark the loan as CLOSED, and prevent any new installments from being generated."
                        } else {
                            "Confirming will record this partial settlement and generate a payment receipt. Because ${rupees(remainingAfter)} remains unpaid, the loan will remain ACTIVE."
                        },
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSubmitClosure(
                            settlementType,
                            settlementAmount,
                            paymentMethod,
                            settlementNote,
                            willCloseLoan
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (willCloseLoan) Navy else Blue)
                ) {
                    Text(if (willCloseLoan) "Confirm & Close Loan" else "Confirm Settlement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Back")
                }
            }
        )
    }
}

@Composable
private fun ClosureRow(label: String, value: String, isBold: Boolean = false, valueColor: Color = Navy) {
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

