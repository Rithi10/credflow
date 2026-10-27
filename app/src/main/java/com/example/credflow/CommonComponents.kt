package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Blue, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.ShowChart, null, tint = Color.White) }
        }
        Spacer(Modifier.width(12.dp))
        Text("Credflow", fontSize = 25.sp, color = Navy, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(title: String, description: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(description, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, description: String) {
    Surface(color = Navy, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color(0xFFCAD9EF), fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(description, color = Color(0xFFCAD9EF), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun CycleCard(title: String, count: String, description: String, icon: ImageVector, onClick: () -> Unit, onAdd: (() -> Unit)? = null) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFEAF1FE), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Blue) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontWeight = FontWeight.SemiBold)
                Text(count, color = Green, fontSize = 12.sp)
                Text(description, color = Muted, fontSize = 11.sp)
            }
            if (onAdd != null) {
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add") }
            } else Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
fun CustomerCard(customer: Customer, onClick: () -> Unit, onCollect: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.clickable(onClick = onClick).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFEAF1FE), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(customer.name.firstOrNull()?.uppercase() ?: "C", color = Blue, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, color = Navy)
                    Text(customer.phone, fontSize = 12.sp, color = Muted)
                    Text("${customer.cycle} · ${customer.day}", fontSize = 11.sp, color = Muted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(rupees(customer.due), color = if (customer.due > 0) Orange else Green, fontWeight = FontWeight.Bold)
                    Text(if (customer.due > 0) "Remaining" else "Paid", color = Muted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) { Text("Details") }
                Button(onClick = onCollect, modifier = Modifier.weight(1f)) { Text("Record Collection") }
            }
        }
    }
}

@Composable
fun EmptyCard(title: String, description: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.PeopleOutline, null, tint = Blue, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, color = Navy, fontWeight = FontWeight.Bold)
            Text(description, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value.ifBlank { "-" }, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Navy, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
