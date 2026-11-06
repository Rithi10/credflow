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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfilePage(
    businessName: String,
    onBusinessNameChange: (String) -> Unit = {},
    ownerName: String,
    onOwnerNameChange: (String) -> Unit = {},
    ownerPhone: String,
    onOwnerPhoneChange: (String) -> Unit = {},
    ownerEmail: String,
    onOwnerEmailChange: (String) -> Unit = {},
    businessAddress: String,
    onAddressChange: (String) -> Unit = {},
    gstinNumber: String,
    onGstinChange: (String) -> Unit = {},
    onSaveProfile: ((String, String, String, String, String, String, (Result<Unit>) -> Unit) -> Unit)? = null,
    onSave: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onResetPassword: () -> Unit = {}
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    var localBusinessName by rememberSaveable(businessName) { mutableStateOf(businessName) }
    var localOwnerName by rememberSaveable(ownerName) { mutableStateOf(ownerName) }
    var localOwnerPhone by rememberSaveable(ownerPhone) { mutableStateOf(ownerPhone) }
    var localOwnerEmail by rememberSaveable(ownerEmail) { mutableStateOf(ownerEmail) }
    var localGstinNumber by rememberSaveable(gstinNumber) { mutableStateOf(gstinNumber) }
    var localBusinessAddress by rememberSaveable(businessAddress) { mutableStateOf(businessAddress) }

    LaunchedEffect(businessName, ownerName, ownerPhone, ownerEmail, businessAddress, gstinNumber) {
        if (!isEditing) {
            localBusinessName = businessName
            localOwnerName = ownerName
            localOwnerPhone = ownerPhone
            localOwnerEmail = ownerEmail
            localGstinNumber = gstinNumber
            localBusinessAddress = businessAddress
        }
    }

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
                                val initials = (if (isEditing) localOwnerName else ownerName).trim().split(" ")
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
                            Text(
                                (if (isEditing) localBusinessName else businessName).ifBlank { "Business Name" },
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Proprietor: ${(if (isEditing) localOwnerName else ownerName).ifBlank { "Owner Name" }}",
                                color = Color(0xFFCAD9EF),
                                fontSize = 13.sp
                            )
                            Text(
                                "+91 ${(if (isEditing) localOwnerPhone else ownerPhone).ifBlank { "Phone Number" }}",
                                color = Color(0xFFCAD9EF),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            if (isEditing) {
                                localBusinessName = businessName
                                localOwnerName = ownerName
                                localOwnerPhone = ownerPhone
                                localOwnerEmail = ownerEmail
                                localGstinNumber = gstinNumber
                                localBusinessAddress = businessAddress
                            }
                            isEditing = !isEditing
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCAD9EF))
                    ) {
                        Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isEditing) "Close Editor" else "Edit Business Profile")
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
                            value = localBusinessName,
                            onValueChange = { localBusinessName = it },
                            label = { Text("Business Name") },
                            leadingIcon = { Icon(Icons.Default.Storefront, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = localOwnerName,
                            onValueChange = { localOwnerName = it },
                            label = { Text("Owner / Proprietor Name") },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = localOwnerPhone,
                            onValueChange = { localOwnerPhone = it },
                            label = { Text("Mobile Phone Number") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = localOwnerEmail,
                            onValueChange = { localOwnerEmail = it },
                            label = { Text("Gmail / Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = localGstinNumber,
                            onValueChange = { localGstinNumber = it },
                            label = { Text("GSTIN / Registration No.") },
                            leadingIcon = { Icon(Icons.Default.ReceiptLong, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = localBusinessAddress,
                            onValueChange = { localBusinessAddress = it },
                            label = { Text("Business Address") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                saveError = null
                                if (onSaveProfile != null) {
                                    isSaving = true
                                    onSaveProfile(
                                        localBusinessName,
                                        localOwnerName,
                                        localOwnerPhone,
                                        localOwnerEmail,
                                        localBusinessAddress,
                                        localGstinNumber,
                                        { result ->
                                            isSaving = false
                                            result.onSuccess {
                                                isEditing = false
                                            }.onFailure { error ->
                                                saveError = error.message ?: "Could not save Business Profile."
                                            }
                                        }
                                    )
                                } else {
                                    isEditing = false
                                    onBusinessNameChange(localBusinessName)
                                    onOwnerNameChange(localOwnerName)
                                    onOwnerPhoneChange(localOwnerPhone)
                                    onOwnerEmailChange(localOwnerEmail)
                                    onAddressChange(localBusinessAddress)
                                    onGstinChange(localGstinNumber)
                                    onSave()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save Business Profile")
                            }
                        }
                        saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
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

        item {
            OutlinedButton(
                onClick = onResetPassword,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset Your Password")
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}
