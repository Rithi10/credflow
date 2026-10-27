package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SimpleListPage(
    title: String, subtitle: String, rows: List<String>,
    onPrimary: () -> Unit, primaryLabel: String,
    onSecondary: (() -> Unit)? = null, secondaryLabel: String = ""
) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { Text(subtitle, color = Muted, fontSize = 13.sp) }
        items(rows) { row -> InfoCard(row, "") }
        item { Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) { Text(primaryLabel) } }
        if (onSecondary != null) item { OutlinedButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondaryLabel) } }
    }
}
