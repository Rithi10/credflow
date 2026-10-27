package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchPage(query: String, onQuery: (String) -> Unit, customers: List<Customer>, onSelect: (Customer) -> Unit, onCollect: (Customer) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Search Customers", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy) }
        item { OutlinedTextField(query, onQuery, label = { Text("Name or mobile number") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (customers.isEmpty()) item { EmptyCard("No matching customers", "Try another name or phone number.") }
        items(customers) { c -> CustomerCard(c, onClick = { onSelect(c) }, onCollect = { onCollect(c) }) }
    }
}
