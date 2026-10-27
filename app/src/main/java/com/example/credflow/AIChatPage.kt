package com.example.credflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AIChatPage(question: String, onQuestion: (String) -> Unit, answer: String, onAsk: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("AI Finance Chatbot", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { InfoCard("Try asking", "How much is pending? · Show weekly customers · How much remains for monthly customers?") }
        item { OutlinedTextField(question, onQuestion, label = { Text("Ask about your collections") }, minLines = 2, modifier = Modifier.fillMaxWidth()) }
        item { Button(onClick = onAsk, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("Ask") } }
        if (answer.isNotBlank()) item { SummaryCard("Answer", answer, "Based on records saved on this device") }
    }
}
