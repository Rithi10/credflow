package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.credflow.ui.viewmodel.ChatUiState

@Composable
fun AIChatPage(
    question: String,
    onQuestion: (String) -> Unit,
    answer: String,
    onAsk: () -> Unit,
    chatState: ChatUiState = ChatUiState.Idle,
    lastAskedQuestion: String = "",
    onRetry: () -> Unit = onAsk,
    onSelectSuggestion: (String) -> Unit = { suggestion ->
        onQuestion(suggestion)
        onAsk()
    }
) {
    val suggestedQuestions = listOf(
        "Monday customers",
        "Monday customers yaaru?",
        "Naalaiku yaar yaar customers?",
        "Tuesday customers yaaru?",
        "Daily customers yaaru?",
        "Weekly customers ethana per?",
        "All customers list kudu",
        "Inniku collection evlo?",
        "Indha month collection evlo?",
        "Total outstanding evlo iruku?"
    )

    val isLoading = chatState is ChatUiState.Loading

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Finance Chatbot",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy
                    )
                    Text(
                        text = "Live MySQL Database Assistant",
                        fontSize = 12.sp,
                        color = Muted
                    )
                }
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Green,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text("Connected", color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. Suggested Question Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Suggested Questions",
                    color = Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(suggestedQuestions) { prompt ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelectSuggestion(prompt) },
                            label = { Text(prompt, fontSize = 12.sp, color = Navy) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = Blue,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                labelColor = Navy
                            ),
                            border = BorderStroke(1.dp, Border)
                        )
                    }
                }
            }
        }

        // 3. Question Input Field
        item {
            OutlinedTextField(
                value = question,
                onValueChange = onQuestion,
                label = { Text("Ask about dues, collections, customers...") },
                placeholder = { Text("e.g. How much collected today?") },
                minLines = 2,
                maxLines = 4,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 4. Ask Button
        item {
            Button(
                onClick = onAsk,
                enabled = !isLoading && question.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Fetching Live Data...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ask Assistant")
                }
            }
        }

        // 5. User's Last Question Display
        val displayedQuestion = lastAskedQuestion.ifBlank { if (isLoading) question else "" }
        if (displayedQuestion.isNotBlank()) {
            item {
                Surface(
                    color = Color(0xFFF1F5FB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = Blue,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Your Question",
                                color = Muted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                displayedQuestion,
                                color = Navy,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 6. Response States: Loading, Error, Success, or Idle Fallback
        when (chatState) {
            is ChatUiState.Loading -> {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Blue,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Querying live MySQL database...",
                                    color = Navy,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Calculating real-time financial metrics",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            is ChatUiState.Error -> {
                item {
                    Surface(
                        color = Color(0xFFFFF4F2),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFD32F2F))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Unable to Fetch Response",
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = chatState.message,
                                color = Navy,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onRetry,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Retry Question")
                            }
                        }
                    }
                }
            }

            is ChatUiState.Success -> {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFEAF1FE),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = Blue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Finance Assistant",
                                        color = Navy,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }

                                if (chatState.isOfflineFallback) {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                                    ) {
                                        Text(
                                            "OFFLINE DATA",
                                            color = Orange,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    val langBadge = when (chatState.language.uppercase()) {
                                        "TANGLISH" -> "LIVE · TANGLISH"
                                        "TAMIL" -> "LIVE · தமிழ்"
                                        else -> "LIVE MYSQL DATA"
                                    }
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                                    ) {
                                        Text(
                                            langBadge,
                                            color = Green,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = chatState.answer,
                                color = Navy,
                                fontSize = 14.sp,
                                lineHeight = 21.sp
                            )

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = Border)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Source: ${chatState.source}",
                                    color = Muted,
                                    fontSize = 11.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onRetry,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Blue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is ChatUiState.Idle -> {
                if (answer.isNotBlank()) {
                    item {
                        SummaryCard("Answer", answer, "Computed from your financial database records")
                    }
                }
            }
        }
    }
}
