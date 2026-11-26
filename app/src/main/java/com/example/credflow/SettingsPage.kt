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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPage(
    acceptCash: Boolean, onAcceptCashChange: (Boolean) -> Unit,
    acceptUpi: Boolean, onAcceptUpiChange: (Boolean) -> Unit,
    acceptBank: Boolean, onAcceptBankChange: (Boolean) -> Unit,
    dailyReminders: Boolean, onDailyRemindersChange: (Boolean) -> Unit,
    overdueAlerts: Boolean, onOverdueAlertsChange: (Boolean) -> Unit,
    reminderHour: Int = 9,
    reminderMinute: Int = 0,
    onReminderTimeChange: (Int, Int) -> Unit = { _, _ -> },
    defaultCycle: String, onDefaultCycleChange: (String) -> Unit,
    lateFeeRule: String, onLateFeeRuleChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenProfile: () -> Unit,
    onExportData: () -> Unit,
    onExportExcel: () -> Unit = {},
    onLogOut: () -> Unit,
    isLoggingOut: Boolean = false,
    onResetData: () -> Unit,
    onDeleteServerData: () -> Unit = {},
    onChangePassword: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    isChangingPassword: Boolean = false
) {
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteServerDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var logoutSubmitted by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var changePasswordErrorText by remember { mutableStateOf<String?>(null) }
    var changePasswordSuccessText by remember { mutableStateOf<String?>(null) }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var deleteConfirmText by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

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
                    if (dailyReminders) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Reminder Time", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Navy)
                                val formattedTime = String.format(
                                    "%02d:%02d %s",
                                    if (reminderHour % 12 == 0) 12 else reminderHour % 12,
                                    reminderMinute,
                                    if (reminderHour >= 12) "PM" else "AM"
                                )
                                Text(formattedTime, fontSize = 12.sp, color = Blue)
                            }
                            OutlinedButton(
                                onClick = {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, h, m -> onReminderTimeChange(h, m) },
                                        reminderHour,
                                        reminderMinute,
                                        false
                                    ).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Change Time", fontSize = 12.sp)
                            }
                        }
                    }
                    HorizontalDivider(color = Border, thickness = 0.5.dp)
                    SwitchRow("Overdue Customer Alerts", overdueAlerts, onOverdueAlertsChange)
                }
            }
        }

        // Security & Privacy
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Security & Privacy", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)
                    Button(
                        onClick = {
                            currentPasswordInput = ""
                            newPasswordInput = ""
                            confirmPasswordInput = ""
                            changePasswordErrorText = null
                            changePasswordSuccessText = null
                            showChangePasswordDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Change Password (Current Verification)")
                    }
                }
            }
        }

        // Export & Data Control
        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Data Export & Control", fontWeight = FontWeight.Bold, color = Navy, fontSize = 16.sp)

                    OutlinedButton(
                        onClick = onExportData,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Collection Book (CSV/Excel)")
                    }
                    OutlinedButton(
                        onClick = onExportExcel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.TableChart, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Full Report (Excel .xlsx)")
                    }
                    OutlinedButton(
                        onClick = { showClearCacheDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                    ) {
                        Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Clear Local Cache (Device Only)")
                    }
                    OutlinedButton(
                        onClick = {
                            deleteConfirmText = ""
                            showDeleteServerDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.DeleteForever, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete All Account Data (Server & Device)")
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
                    enabled = !isLoggingOut && !logoutSubmitted,
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
            onDismissRequest = { if (!isLoggingOut && !logoutSubmitted) showLogoutDialog = false },
            title = { Text("Confirm Log Out", fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to log out?", fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isLoggingOut && !logoutSubmitted) {
                            logoutSubmitted = true
                            showLogoutDialog = false
                            onLogOut()
                        }
                    },
                    enabled = !isLoggingOut && !logoutSubmitted,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) { Text("Yes") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("No") }
            }
        )
    }

    // Clear Local Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Local Cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear cached customer and collection records on this device. Your records stored on the server will NOT be deleted and will reload upon your next sync.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearCacheDialog = false
                        onResetData()
                    }
                ) { Text("Clear Cache") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Permanent Server Delete Confirmation Dialog
    if (showDeleteServerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteServerDialog = false },
            title = { Text("Permanently Delete Server Data?", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "DANGER: This action is permanent and cannot be undone. All your loan customers, payment transactions, and collection history will be deleted from the MySQL database and this device.",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp
                    )
                    Text("Type DELETE in capital letters to confirm:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteServerDialog = false
                        onDeleteServerData()
                    },
                    enabled = deleteConfirmText.trim() == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete Everything") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteServerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isChangingPassword) showChangePasswordDialog = false
            },
            title = {
                Text("Change Password", fontWeight = FontWeight.Bold, color = Navy)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Verify your current password to set a new password. Your session will be secured automatically.",
                        fontSize = 12.sp,
                        color = Muted
                    )

                    if (changePasswordErrorText != null) {
                        Text(changePasswordErrorText!!, color = Color.Red, fontSize = 12.sp)
                    }
                    if (changePasswordSuccessText != null) {
                        Text(changePasswordSuccessText!!, color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        visualTransformation = if (showCurrentPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                                Icon(
                                    if (showCurrentPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Password (min 6 chars)") },
                        singleLine = true,
                        visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                Icon(
                                    if (showNewPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val curr = currentPasswordInput.trim()
                        val newPass = newPasswordInput.trim()
                        val conf = confirmPasswordInput.trim()

                        if (curr.isBlank()) {
                            changePasswordErrorText = "Please enter your current password."
                            return@Button
                        }
                        if (newPass.length < 6) {
                            changePasswordErrorText = "New password must be at least 6 characters."
                            return@Button
                        }
                        if (newPass == curr) {
                            changePasswordErrorText = "New password must be different from current password."
                            return@Button
                        }
                        if (newPass != conf) {
                            changePasswordErrorText = "New password and confirmation do not match."
                            return@Button
                        }

                        changePasswordErrorText = null
                        onChangePassword(curr, newPass, conf) { success, msg ->
                            if (success) {
                                changePasswordSuccessText = msg
                                changePasswordErrorText = null
                            } else {
                                changePasswordErrorText = msg
                            }
                        }
                    },
                    enabled = !isChangingPassword
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("Updating...")
                    } else {
                        Text("Update Password")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isChangingPassword
                ) {
                    Text("Close")
                }
            }
        )
    }
}

internal fun validatePasswordChange(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String
): String? {
    if (currentPassword.isBlank()) {
        return "Enter your current password."
    }
    if (newPassword.length < 6) {
        return "New password must be at least 6 characters."
    }
    if (newPassword != confirmPassword) {
        return "New password and confirmation do not match."
    }
    if (newPassword.toByteArray(Charsets.UTF_8).size > 72) {
        return "New password must be no more than 72 UTF-8 bytes."
    }
    return null
}
