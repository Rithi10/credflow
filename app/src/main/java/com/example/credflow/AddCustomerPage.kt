package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddCustomerPage(
    cycle: String, name: String, onName: (String) -> Unit,
    phone: String, onPhone: (String) -> Unit, amount: String, onAmount: (String) -> Unit,
    day: String, onDay: (String) -> Unit, note: String, onNote: (String) -> Unit, onSave: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Add $cycle Customer", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { Text("Enter the customer details below.", color = Muted, fontSize = 13.sp) }
        item { OutlinedTextField(name, onName, label = { Text("Customer name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(phone, onPhone, label = { Text("Mobile number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(amount, onAmount, label = { Text(if (cycle == "Daily") "Daily collection amount (₹)" else if (cycle == "Weekly") "Weekly collection amount (₹)" else "Monthly collection amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (cycle == "Weekly") {
            item { Text("Collection day", fontWeight = FontWeight.SemiBold, color = Navy) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(listOf("Monday", "Tuesday"), listOf("Wednesday", "Thursday"), listOf("Friday", "Saturday"), listOf("Sunday")).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { weekday ->
                                FilterChip(selected = day == weekday, onClick = { onDay(weekday) }, label = { Text(weekday) })
                            }
                        }
                    }
                }
            }
        }
        item { OutlinedTextField(note, onNote, label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        item {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Save Customer") }
        }
        item { Text("No online payment is collected. This app only records money received in cash or GPay.", color = Muted, fontSize = 12.sp) }
    }
}
