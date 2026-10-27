package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfilePage(
    businessName: String, onBusinessNameChange: (String) -> Unit,
    ownerName: String, onOwnerNameChange: (String) -> Unit,
    ownerPhone: String, onOwnerPhoneChange: (String) -> Unit,
    ownerEmail: String, onOwnerEmailChange: (String) -> Unit,
    businessAddress: String, onAddressChange: (String) -> Unit,
    gstinNumber: String, onGstinChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Business Profile", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Your business identity & contact details", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, "Settings", tint = Navy)
                }
            }
        }

        // Digital Business Card
        item {
            Surface(
                color = Navy,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFEAF1FE),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val initials = ownerName.trim().split(" ")
                                    .mapNotNull { it.firstOrNull()?.toString() }
                                    .take(2).joinToString("").ifEmpty { "CF" }
                                Text(
                                    initials,
                                    color = Blue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(businessName.ifBlank { "Business Name" }, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Proprietor: ${ownerName.ifBlank { "Owner Name" }}", color = Color(0xFFCAD9EF), fontSize = 13.sp)
                            Text("+91 ${ownerPhone.ifBlank { "Phone Number" }}", color = Color(0xFFCAD9EF), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { isEditing = !isEditing },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCAD9EF))
                        ) {
                            Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isEditing) "Close" else "Edit Profile")
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Card")
                        }
                    }
                }
            }
        }

        // Editable Form or Display Details
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Business Details", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)

                    if (isEditing) {
                        OutlinedTextField(
                            value = businessName, onValueChange = onBusinessNameChange,
                            label = { Text("Business Name") }, leadingIcon = { Icon(Icons.Default.Storefront, null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerName, onValueChange = onOwnerNameChange,
                            label = { Text("Owner / Proprietor Name") }, leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerPhone, onValueChange = onOwnerPhoneChange,
                            label = { Text("Mobile Phone Number") }, leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ownerEmail, onValueChange = onOwnerEmailChange,
                            label = { Text("Gmail / Email Address") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = gstinNumber, onValueChange = onGstinChange,
                            label = { Text("GSTIN / Registration No.") }, leadingIcon = { Icon(Icons.Default.ReceiptLong, null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = businessAddress, onValueChange = onAddressChange,
                            label = { Text("Business Address") }, leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { isEditing = false; onSave() },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save Business Profile")
                        }
                    } else {
                        InfoRow("Business Name", businessName)
                        InfoRow("Proprietor", ownerName)
                        InfoRow("Mobile Phone", "+91 $ownerPhone")
                        InfoRow("Email Address", ownerEmail)
                        InfoRow("GSTIN", gstinNumber)
                        InfoRow("Address", businessAddress)
                    }
                }
            }
        }

        // Link to Settings
        item {
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Settings, null)
                Spacer(Modifier.width(8.dp))
                Text("Open App & Collection Settings  →")
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}
