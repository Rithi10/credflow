package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CollectionSavedPage(customer: Customer?, onDone: () -> Unit, onHistory: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Collection recorded", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text(customer?.name ?: "Customer", color = Muted)
        Text("Remaining balance: ${rupees(customer?.due ?: 0.0)}", color = Navy, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Back to Dashboard") }
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Payment History") }
    }
}
