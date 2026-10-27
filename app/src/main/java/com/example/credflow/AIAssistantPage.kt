package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AIAssistantPage(onChat: () -> Unit, onPrediction: () -> Unit, onForecast: () -> Unit, onReports: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("AI Assistant", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { Text("Simple insights to help manage collections.", color = Muted) }
        item { CycleCard("AI Finance Chatbot", "Ask in everyday language", "“How much is pending this week?”", Icons.Default.Chat, onChat) }
        item { CycleCard("Repayment Prediction", "Review remaining balances", "Demo summary; not a validated ML model", Icons.Default.Psychology, onPrediction) }
        item { CycleCard("Collection Forecast", "Daily / weekly / monthly totals", "Current records and estimated summaries", Icons.Default.TrendingUp, onForecast) }
        item { CycleCard("Reports", "View collection totals", "Summaries by collection cycle", Icons.Default.BarChart, onReports) }
        item { InfoCard("AI status", "Chat answers are calculated from local app records in this demo. A real language model and trained prediction/forecasting services must be connected separately.") }
    }
}
