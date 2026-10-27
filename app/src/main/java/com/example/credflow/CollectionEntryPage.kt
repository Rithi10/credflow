package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionEntryPage(customer: Customer?, amount: String, onAmount: (String) -> Unit, method: String, onMethod: (String) -> Unit, onSave: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Record Collection", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { SummaryCard(customer?.name ?: "Customer", customer?.phone ?: "", "Remaining balance: ${rupees(customer?.due ?: 0.0)}") }
        item { OutlinedTextField(amount, onAmount, label = { Text("Amount received (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { Text("Payment method", fontWeight = FontWeight.SemiBold, color = Navy) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = method == "Cash", onClick = { onMethod("Cash") }, label = { Text("Cash") }, leadingIcon = { Icon(Icons.Default.Payments, null) })
                FilterChip(selected = method == "GPay", onClick = { onMethod("GPay") }, label = { Text("GPay") }, leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null) })
            }
        }
        item { InfoCard("Important", "This only updates your collection record. It does not make a payment or connect to a payment gateway.") }
        item { Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Save Collection") } }
    }
}
