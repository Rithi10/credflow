package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.dto.PaymentReceiptDto

@Composable
fun CollectionSavedPage(
    customer: Customer?,
    receipt: PaymentReceiptDto? = null,
    receiptSavedFileName: String? = null,
    onAutoGenerateReceipt: (PaymentReceiptDto) -> Unit = {},
    onShareReceipt: (PaymentReceiptDto) -> Unit = {},
    onViewReceiptPdf: (PaymentReceiptDto) -> Unit = {},
    onDone: () -> Unit,
    onHistory: () -> Unit
) {
    LaunchedEffect(receipt?.paymentId, receipt?.receiptNumber) {
        if (receipt != null && receipt.receiptNumber.isNotBlank()) {
            onAutoGenerateReceipt(receipt)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("Collection recorded", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(customer?.name ?: (receipt?.customerName ?: "Customer"), color = Muted)
                Text(
                    "Remaining balance: ${rupees(receipt?.remainingBalance ?: (customer?.due ?: 0.0))}",
                    color = Navy,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (receipt != null && receipt.receiptNumber.isNotBlank()) {
            item {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Payment Receipt", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                            Surface(color = Color(0xFFEAF1FE), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = receipt.receiptNumber,
                                    color = Blue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = Border, thickness = 0.5.dp)
                        ReceiptRow("Customer", receipt.customerName)
                        if (receipt.customerPhone.isNotBlank()) {
                            ReceiptRow("Phone", receipt.customerPhone)
                        }
                        ReceiptRow("Amount Paid", rupees(receipt.paymentAmount), valueColor = Green)
                        ReceiptRow("Date", receipt.paymentDate)
                        ReceiptRow("Method", receipt.paymentMethod)
                        ReceiptRow("Total Loan", rupees(receipt.totalLoanAmount))
                        ReceiptRow("Total Paid", rupees(receipt.totalPaidAmount))
                        ReceiptRow("Remaining Balance", rupees(receipt.remainingBalance), valueColor = Navy)
                        if (!receiptSavedFileName.isNullOrBlank()) {
                            Text(
                                text = "PDF Receipt ready: $receiptSavedFileName",
                                fontSize = 11.sp,
                                color = Green,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { onShareReceipt(receipt) },
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Receipt (WhatsApp / Apps)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onViewReceiptPdf(receipt) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View Receipt PDF")
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to Dashboard") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Payment History") }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, valueColor: Color = Navy) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Muted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
