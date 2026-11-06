package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.data.remote.ApiConfig
import com.example.credflow.domain.model.Customer
import java.util.Locale

@Composable
fun PredictionPage(
    customers: List<Customer>,
    isPredicting: Boolean = false,
    probability: Double? = null,
    errorMsg: String? = null,
    onRunPrediction: () -> Unit = {},
    onDetails: () -> Unit
) {
    val activeCustomers = customers.filter { it.due > 0 }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("AI Repayment Prediction", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Connected to Python FastAPI Machine Learning Model", color = Muted, fontSize = 12.sp)
            }
        }

        item {
            SummaryCard(
                "Active Portfolio Exposure",
                "${activeCustomers.size} customers",
                "Total outstanding: ${rupees(customers.sumOf { it.due })}"
            )
        }

        // FastAPI Prediction Trigger Card
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Blue)
                            Text("FastAPI Model Service", fontWeight = FontWeight.Bold, color = Navy, fontSize = 15.sp)
                        }
                        Surface(color = Color(0xFFEAF1FE), shape = RoundedCornerShape(8.dp)) {
                            Text("POST /predict", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    Text("Target Endpoint: ${ApiConfig.getAiEndpoint("predict")}", fontSize = 12.sp, color = Muted)

                    Button(
                        onClick = onRunPrediction,
                        enabled = !isPredicting,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isPredicting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text("Querying FastAPI AI Model...", fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Run AI Repayment Prediction", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Prediction Loading / Success / Error Card
        if (isPredicting) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Blue, strokeWidth = 3.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Predicting Repayment Probability...", fontWeight = FontWeight.Bold, color = Navy, fontSize = 14.sp)
                            Text("Evaluating parameters on Python FastAPI backend...", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        } else if (probability != null) {
            item {
                val probPercent = probability * 100.0
                val (badgeText, badgeBg, badgeFg) = when {
                    probPercent >= 80.0 -> Triple("HIGH LIKELIHOOD", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                    probPercent >= 50.0 -> Triple("MODERATE RISK", Color(0xFFFFF3E0), Color(0xFFE65100))
                    else -> Triple("HIGH RISK OF DEFAULT", Color(0xFFFFEBEE), Color(0xFFC62828))
                }

                Surface(
                    color = Navy,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Repayment Probability", color = Color(0xFFCAD9EF), fontSize = 13.sp)
                            Surface(color = badgeBg, shape = RoundedCornerShape(8.dp)) {
                                Text(badgeText, color = badgeFg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${String.format(Locale.ROOT, "%.1f", probPercent)}%", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("($probability)", color = Color(0xFFCAD9EF), fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                        }

                        HorizontalDivider(color = Color(0xFF234B82), thickness = 0.8.dp)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                            Text(
                                if (probPercent >= 80.0)
                                    "FastAPI AI Model predicts a high likelihood of complete on-time repayment."
                                else
                                    "FastAPI AI Model recommends increased collection frequency for this portfolio.",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else if (errorMsg != null) {
            item {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828))
                            Text("FastAPI AI Connection Error", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 14.sp)
                        }
                        Text(errorMsg, color = Color(0xFFB71C1C), fontSize = 12.sp)

                        OutlinedButton(
                            onClick = onRunPrediction,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                            border = BorderStroke(1.dp, Color(0xFFE57373)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry FastAPI Prediction", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            CycleCard(
                "Repayment Risk Details",
                "View data summary",
                "Understand live metrics sent to FastAPI AI model",
                Icons.Default.Insights,
                onDetails
            )
        }
    }
}
