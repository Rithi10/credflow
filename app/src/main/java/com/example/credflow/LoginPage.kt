package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
fun LoginPage(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onGoogleSignIn: (String, String) -> Unit
) {
    var isSignUp by remember { mutableStateOf(false) }

    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var businessNameInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column {
                Text("Credflow Auth", color = Muted, fontSize = 11.sp)
                Text(if (isSignUp) "Create Account" else "Sign In", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Navy)
            }
        }
        Spacer(Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = if (isSignUp) 1 else 0,
            containerColor = Color(0xFFEAF1FE),
            contentColor = Blue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = !isSignUp,
                onClick = { isSignUp = false; error = "" },
                text = { Text("Sign In", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = isSignUp,
                onClick = { isSignUp = true; error = "" },
                text = { Text("Sign Up", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (isSignUp) "Register your business" else "Welcome back",
                    fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Navy
                )
                Text(
                    if (isSignUp) "Start managing your loan collections easily" else "Sign in to access your collection book",
                    color = Muted, fontSize = 13.sp
                )
            }

            item {
                OutlinedButton(
                    onClick = { showGoogleAccountPicker = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Navy)
                ) {
                    Surface(
                        color = Color(0xFF4285F4),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (isSignUp) "Sign Up with Google / Gmail" else "Continue with Google / Gmail",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                    Text(
                        if (isSignUp) "  OR REGISTER WITH EMAIL/PHONE  " else "  OR SIGN IN WITH EMAIL/PHONE  ",
                        color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                    )
                    HorizontalDivider(Modifier.weight(1f), color = Border)
                }
            }

            if (isSignUp) {
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it },
                        label = { Text("Business Name") },
                        leadingIcon = { Icon(Icons.Default.Storefront, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = identity,
                    onValueChange = { identity = it },
                    label = { Text("Gmail / Email or Mobile Number") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (!isSignUp) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showForgotPasswordDialog = true }) {
                            Text("Forgot Password?", fontSize = 12.sp, color = Blue)
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                item {
                    Text(error, color = Color.Red, fontSize = 12.sp)
                }
            }

            item {
                Button(
                    onClick = {
                        if (isSignUp) {
                            if (fullName.isBlank()) {
                                error = "Please enter your full name."
                            } else if (identity.isBlank()) {
                                error = "Please enter an email address or mobile number."
                            } else if (password.length < 4) {
                                error = "Password must be at least 4 characters."
                            } else {
                                onSignUp(fullName, businessNameInput, identity, password)
                            }
                        } else {
                            if (identity.isBlank() || password.isBlank()) {
                                error = "Enter a valid mobile/email and password."
                            } else {
                                onLogin(identity, password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isSignUp) "Create Account  →" else "Sign In  →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                InfoCard("Multiple Sign-In Options", "Choose Gmail for one-tap sign in or register with mobile number & password.")
            }
        }
    }

    // Google Account Picker Dialog
    if (showGoogleAccountPicker) {
        var customGmail by remember { mutableStateOf("") }
        var showAddAccountInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showGoogleAccountPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF4285F4),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Choose a Google Account", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select an account to continue to Credflow:", color = Muted, fontSize = 13.sp)

                    val accounts = listOf(
                        Pair("Ramesh Kumar", "ramesh.kumar@gmail.com"),
                        Pair("Credflow Finance", "credflow.official@gmail.com"),
                        Pair("Personal User", "user.personal@gmail.com")
                    )

                    accounts.forEach { (accName, accEmail) ->
                        Surface(
                            color = Color(0xFFF3F6FA),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showGoogleAccountPicker = false
                                    onGoogleSignIn(accName, accEmail)
                                }
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Blue,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(accName.firstOrNull()?.toString() ?: "G", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(accName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy)
                                    Text(accEmail, fontSize = 12.sp, color = Muted)
                                }
                            }
                        }
                    }

                    if (showAddAccountInput) {
                        OutlinedTextField(
                            value = customGmail,
                            onValueChange = { customGmail = it },
                            label = { Text("Enter Gmail Address") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (customGmail.isNotBlank()) {
                                    val nameFromEmail = customGmail.substringBefore("@").replace(".", " ")
                                    showGoogleAccountPicker = false
                                    onGoogleSignIn(nameFromEmail, customGmail)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign In with This Account")
                        }
                    } else {
                        TextButton(
                            onClick = { showAddAccountInput = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add another Google account")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGoogleAccountPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(identity) }
        var resetSent by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (resetSent) {
                        Text("A password reset link has been sent to $resetEmail.", color = Green)
                    } else {
                        Text("Enter your registered Gmail or mobile number to receive a password reset link.")
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Gmail or Mobile") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (resetSent) {
                    Button(onClick = { showForgotPasswordDialog = false }) { Text("OK") }
                } else {
                    Button(onClick = {
                        if (resetEmail.isNotBlank()) resetSent = true
                    }) { Text("Send Link") }
                }
            },
            dismissButton = {
                if (!resetSent) {
                    TextButton(onClick = { showForgotPasswordDialog = false }) { Text("Cancel") }
                }
            }
        )
    }
}
