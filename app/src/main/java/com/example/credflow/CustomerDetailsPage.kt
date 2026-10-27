package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CustomerDetailsPage(customer: Customer?, onCollect: () -> Unit, onHistory: () -> Unit, onBack: () -> Unit) {
    if (customer == null) {
        EmptyCard("No customer selected", "Go back and select a customer.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SummaryCard(customer.name, customer.phone, "${customer.cycle} · ${customer.day}") }
        item { SummaryCard("Remaining balance", rupees(customer.due), "Collected ${rupees(customer.paid)} of ${rupees(customer.amount)}") }
        item { InfoCard("Customer note", customer.note.ifBlank { "No note added." }) }
        item { Button(onClick = onCollect, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.EditNote, null); Spacer(Modifier.width(8.dp)); Text("Record Collection") } }
        item { OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("View Payment History") } }
        item { TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to customer list") } }
    }
}
