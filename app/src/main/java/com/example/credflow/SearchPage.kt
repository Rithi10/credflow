package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchPage(
    query: String,
    onQuery: (String) -> Unit,
    customers: List<Customer>,
    cycleFilter: String = "All",
    onCycleFilterChange: (String) -> Unit = {},
    statusFilter: String = "All",
    onStatusFilterChange: (String) -> Unit = {},
    sortOption: String = "name_asc",
    onSortOptionChange: (String) -> Unit = {},
    isSearching: Boolean = false,
    searchError: String? = null,
    onClearFilters: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSelect: (Customer) -> Unit,
    onCollect: (Customer) -> Unit
) {
    val hasActiveFilters = query.isNotBlank() ||
            !cycleFilter.equals("All", ignoreCase = true) ||
            !statusFilter.equals("All", ignoreCase = true) ||
            sortOption != "name_asc"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Search Customers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Live MySQL search & smart filters", fontSize = 12.sp, color = Muted)
                }
                if (hasActiveFilters) {
                    TextButton(onClick = onClearFilters) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear Filters", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                label = { Text("Name or mobile number") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cycle Filter Chips
        item {
            Text("Collection Cycle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Daily", "Weekly", "Monthly")) { cycle ->
                    FilterChip(
                        selected = cycleFilter.equals(cycle, ignoreCase = true),
                        onClick = { onCycleFilterChange(cycle) },
                        label = { Text(cycle, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Status Filter Chips
        item {
            Text("Account Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Paid", "Pending", "Overdue", "Closed")) { status ->
                    FilterChip(
                        selected = statusFilter.equals(status, ignoreCase = true),
                        onClick = { onStatusFilterChange(status) },
                        label = { Text(status, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Sort Chips
        item {
            Text("Sort By", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            Spacer(Modifier.height(4.dp))
            val sorts = listOf(
                "name_asc" to "Name (A–Z)",
                "balance_desc" to "Balance (High–Low)",
                "balance_asc" to "Balance (Low–High)",
                "due_date_asc" to "Next Due Date"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sorts) { (key, label) ->
                    FilterChip(
                        selected = sortOption == key,
                        onClick = { onSortOptionChange(key) },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Status / Count Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${customers.size} customer(s) found",
                    fontSize = 12.sp,
                    color = Muted,
                    fontWeight = FontWeight.Medium
                )
                if (isSearching) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Blue)
                        Text("Searching MySQL...", fontSize = 11.sp, color = Blue)
                    }
                }
            }
        }

        if (!searchError.isNullOrBlank()) {
            item {
                Surface(
                    color = Color(0xFFFFF5F5),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                            Text(searchError, color = Color(0xFFC62828), fontSize = 12.sp)
                        }
                        TextButton(onClick = onRetry) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (!isSearching && customers.isEmpty()) {
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No matching customers", color = Navy, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = if (hasActiveFilters) {
                                "No customers match your search query and active filters."
                            } else {
                                "Try searching by customer name or mobile number."
                            },
                            color = Muted,
                            fontSize = 12.sp
                        )
                        if (hasActiveFilters) {
                            OutlinedButton(onClick = onClearFilters) {
                                Text("Clear All Filters")
                            }
                        }
                    }
                }
            }
        }

        items(customers, key = { it.id }) { c ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CustomerCard(c, onClick = { onSelect(c) }, onCollect = { onCollect(c) })
                val badgeStatus = c.statusLabel.ifBlank {
                    when {
                        c.isClosed -> "Closed"
                        c.due <= 0.001 -> "Paid"
                        c.overdueDays > 0 -> "Overdue"
                        else -> "Pending"
                    }
                }
                val dueHint = c.nextDueDateLabel.takeIf { it.isNotBlank() } ?: ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Status: $badgeStatus" + if (c.overdueDays > 0) " (${c.overdueDays}d overdue)" else "",
                        fontSize = 11.sp,
                        color = when (badgeStatus.lowercase()) {
                            "overdue" -> Color(0xFFC62828)
                            "closed", "paid" -> Green
                            else -> Muted
                        },
                        fontWeight = FontWeight.Medium
                    )
                    if (dueHint.isNotBlank()) {
                        Text("Next Due: $dueHint", fontSize = 11.sp, color = Muted)
                    }
                }
            }
        }
    }
}
