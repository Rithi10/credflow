package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.dto.CustomerRiskDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.ui.viewmodel.PredictionUiState

@Composable
fun PredictionPage(
    customers: List<Customer>,
    predictionState: PredictionUiState = PredictionUiState.Idle,
    onRunPrediction: () -> Unit = {},
    onSelectCustomer: (Customer) -> Unit = {},
    onDetails: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Repayment Prediction",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = "Live MySQL Risk & Collection Forecast",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            }
        }

        item {
            SummaryCard(
                title = "Customers with remaining balance",
                value = "${customers.count { it.due > 0 }}",
                description = "Total remaining ${rupees(customers.sumOf { it.due })}"
            )
        }

        item {
            when (predictionState) {
                is PredictionUiState.Idle -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFEAF1FE),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Insights, contentDescription = null, tint = Blue)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Live Repayment Prediction",
                                        fontWeight = FontWeight.Bold,
                                        color = Navy,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Dynamic MySQL Risk Analysis",
                                        fontSize = 12.sp,
                                        color = Muted
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Analyze your live customers, collection history, and installment dues to predict on-time repayment likelihood.",
                                fontSize = 13.sp,
                                color = Muted,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onRunPrediction,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Run Prediction", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                is PredictionUiState.Loading -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Blue,
                                modifier = Modifier.size(38.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Evaluating Live Database Records...",
                                fontWeight = FontWeight.Bold,
                                color = Navy,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Querying live MySQL payments, installments, and customer balances in real-time.",
                                color = Muted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is PredictionUiState.Success -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            // Model Type Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (predictionState.isMlModel) Color(0xFFEDE7F6) else Color(0xFFEAF1FE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (predictionState.isMlModel) Icons.Default.Psychology else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (predictionState.isMlModel) Color(0xFF5E35B1) else Blue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (predictionState.isMlModel) "PyTorch ML Model" else "Early Dynamic Prediction",
                                            color = if (predictionState.isMlModel) Color(0xFF5E35B1) else Blue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "${predictionState.historicalRows}/${predictionState.requiredTrainingRows} ML Records",
                                    color = Muted,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            // Probability & Risk Tier
                            val pct = (predictionState.repaymentProbability * 100).toInt().coerceIn(0, 100)
                            val riskColor = if (pct >= 75) Green else if (pct >= 50) Orange else Color(0xFFC62828)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Portfolio On-Time Likelihood", color = Muted, fontSize = 12.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "$pct%",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = riskColor
                                    )
                                }
                                Surface(
                                    color = riskColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = predictionState.overallRiskTier,
                                        color = riskColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            if (predictionState.overallReason.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = predictionState.overallReason,
                                    color = Navy,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Border)
                            Spacer(Modifier.height(12.dp))

                            InfoRow("Confidence / Basis", predictionState.confidence)
                            Spacer(Modifier.height(6.dp))
                            InfoRow("Active Accounts Evaluated", "${predictionState.totalCustomers} customers")
                            Spacer(Modifier.height(6.dp))
                            InfoRow("Active Installments", "${predictionState.predictedInstallments}")
                            Spacer(Modifier.height(6.dp))
                            InfoRow("Algorithm / Engine", predictionState.predictionTypeDisplay)

                            if (predictionState.disclaimer.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = predictionState.disclaimer,
                                    fontSize = 11.sp,
                                    color = Muted,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                is PredictionUiState.NoActiveInstallments -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFEAF1FE),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = Green)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "No Active Loan Dues",
                                        fontWeight = FontWeight.Bold,
                                        color = Navy,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "All accounts are settled",
                                        fontSize = 12.sp,
                                        color = Green
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = predictionState.message,
                                color = Muted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                is PredictionUiState.Error -> {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFC62828))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Prediction Error",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828),
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Unable to fetch live prediction",
                                        fontSize = 12.sp,
                                        color = Muted
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = predictionState.message,
                                color = Navy,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onRunPrediction,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // Customer Repayment Risk Breakdown Section
        if (predictionState is PredictionUiState.Success && predictionState.customerPredictions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customer Risk Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = "${predictionState.customerPredictions.size} Accounts",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
            }

            items(predictionState.customerPredictions) { item ->
                CustomerRiskCard(
                    item = item,
                    onClick = {
                        val matching = customers.find { it.id == item.customerId }
                        if (matching != null) {
                            onSelectCustomer(matching)
                        }
                    }
                )
            }
        }

        item {
            CycleCard(
                title = "Repayment Risk Details",
                count = "View data summary",
                description = "Understand what this AI model evaluates",
                icon = Icons.Default.Insights,
                onClick = onDetails
            )
        }
    }
}

@Composable
fun CustomerRiskCard(item: CustomerRiskDto, onClick: () -> Unit) {
    val riskColor = when (item.riskTier) {
        "Low Risk" -> Green
        "Moderate Risk" -> Orange
        "Settled" -> Blue
        else -> Color(0xFFC62828)
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFEAF1FE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.name.firstOrNull()?.uppercase() ?: "C",
                                color = Blue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            color = Navy,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${item.cycle} · ₹${String.format("%.2f", item.remainingDue)} due",
                            fontSize = 11.sp,
                            color = Muted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    val pct = (item.repaymentProbability * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = "$pct%",
                        fontWeight = FontWeight.ExtraBold,
                        color = riskColor,
                        fontSize = 16.sp
                    )
                    Surface(
                        color = riskColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.riskTier,
                            color = riskColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (item.riskReason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.riskReason,
                    color = Muted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
