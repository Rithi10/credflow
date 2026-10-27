package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(
    acceptCash: Boolean, onAcceptCashChange: (Boolean) -> Unit,
    acceptUpi: Boolean, onAcceptUpiChange: (Boolean) -> Unit,
    acceptBank: Boolean, onAcceptBankChange: (Boolean) -> Unit,
    dailyReminders: Boolean, onDailyRemindersChange: (Boolean) -> Unit,
    overdueAlerts: Boolean, onOverdueAlertsChange: (Boolean) -> Unit,
    defaultCycle: String, onDefaultCycleChange: (String) -> Unit,
    appLockEnabled: Boolean, onAppLockChange: (Boolean) -> Unit,
    lateFeeRule: String, onLateFeeRuleChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenProfile: () -> Unit,
    onExportData: () -> Unit,
    onLogOut: () -> Unit,
    onResetData: () -> Unit
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("App Settings", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Collection preferences, security & data control", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = onOpenProfile) {
                    Icon(Icons.Default.Person, "Profile", tint = Navy)
                }
            }
        }

        // Collection & Payment Preferences
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Collection Rules & Payments", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)

                    Text("Accepted Payment Methods", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Medium)
                    SwitchRow("Cash Collections", acceptCash, onAcceptCashChange)
                    SwitchRow("GPay / UPI Payments", acceptUpi, onAcceptUpiChange)
                    SwitchRow("Bank Transfer / NEFT", acceptBank, onAcceptBankChange)

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    Text("Default Loan Cycle", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Daily", "Weekly", "Monthly").forEach { c ->
                            FilterChip(
                                selected = defaultCycle == c,
                                onClick = { onDefaultCycleChange(c) },
                                label = { Text(c) }
                            )
                        }
                    }

                    HorizontalDivider(color = Border, thickness = 0.5.dp)

                    Text("Late Fine / Interest Rule", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("No Charge", "1% / Week", "2% / Month").forEach { r ->
                            FilterChip(
                                selected = lateFeeRule == r,
                                onClick = { onLateFeeRuleChange(r) },
                                label = { Text(r) }
                            )
                        }
                    }
                }
            }
        }

        // Notifications & Alerts
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Notifications & Reminders", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                    SwitchRow("Daily Collection Reminders", dailyReminders, onDailyRemindersChange)
                    SwitchRow("Overdue Customer Alerts", overdueAlerts, onOverdueAlertsChange)
                }
            }
        }

        // Security & App Lock
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security & Privacy", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                    SwitchRow("Biometric / PIN App Lock", appLockEnabled, onAppLockChange)
                    OutlinedButton(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Change Account Password")
                    }
                }
            }
        }

        // Data Backup & Export
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Data Backup & Control", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                    OutlinedButton(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Collection Book (CSV/Excel)")
                    }
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All App Data")
                    }
                }
            }
        }

        // Save & Account Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Settings")
                }
                OutlinedButton(
                    onClick = onOpenProfile,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Person, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Business Profile")
                }
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(Icons.Default.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out")
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }

    // Log Out Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to log out?", fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Yes") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("No") }
            }
        )
    }

    // Change Password Dialog
    if (showPasswordDialog) {
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPass, onValueChange = { oldPass = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPass, onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPass, onValueChange = { confirmPass = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    if (passError.isNotBlank()) {
                        Text(passError, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (oldPass.isBlank()) {
                        passError = "Enter current password"
                    } else if (newPass.length < 4) {
                        passError = "New password must be at least 4 characters"
                    } else if (newPass != confirmPass) {
                        passError = "Passwords do not match"
                    } else {
                        showPasswordDialog = false
                    }
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reset Data Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Clear All App Data?", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = { Text("This will permanently remove all customers and collection records. Are you sure?") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Reset Data") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}
