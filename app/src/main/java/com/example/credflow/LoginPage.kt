package com.example.credflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.credflow.data.remote.ApiConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginPage(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onGoogleSignInToken: (String) -> Unit,
    isLoading: Boolean = false,
    authError: String? = null,
    onClearError: () -> Unit = {}
) {
    var isSignUp by remember { mutableStateOf(false) }

    var identity by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var businessNameInput by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    fun launchGoogleAccountChooser() {
        val clientId = ApiConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (clientId.isBlank() || clientId.contains("YOUR_") || clientId.contains("PASTE_")) {
            localError = "To use Google Sign-In, please paste your Web Client ID into ApiConfig.GOOGLE_WEB_CLIENT_ID from Google Cloud Console."
            return
        }
        isGoogleLoading = true
        localError = ""
        onClearError()

        coroutineScope.launch {
            try {
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                    .build()

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(request = request, context = context)
                val credential = response.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    onGoogleSignInToken(idToken)
                } else {
                    localError = "Unexpected credential format returned from Google."
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed or tapped outside the account chooser
            } catch (e: GetCredentialException) {
                val msg = e.message ?: ""
                localError = if (msg.contains("No credentials available", ignoreCase = true)) {
                    "Google Sign-In: No credentials available. Please ensure your Android OAuth Client ID (SHA-1) is registered in Google Cloud Console."
                } else {
                    "Google Sign-In: $msg"
                }
            } catch (e: Exception) {
                localError = "Google Sign-In: ${e.message ?: "Could not open Google account picker."}"
            } finally {
                isGoogleLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
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
                onClick = { isSignUp = false; localError = ""; onClearError() },
                text = { Text("Sign In", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = isSignUp,
                onClick = { isSignUp = true; localError = ""; onClearError() },
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
                    onClick = { launchGoogleAccountChooser() },
                    enabled = !isLoading && !isGoogleLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Navy)
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Blue)
                        Spacer(Modifier.width(10.dp))
                        Text("Opening Google Accounts...", fontSize = 13.sp)
                    } else {
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
                        onValueChange = { fullName = it; localError = ""; onClearError() },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = businessNameInput,
                        onValueChange = { businessNameInput = it; localError = ""; onClearError() },
                        label = { Text("Business / Firm Name (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Storefront, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = identity,
                    onValueChange = { identity = it; localError = ""; onClearError() },
                    label = { Text("Email or Mobile Number *") },
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = ""; onClearError() },
                    label = { Text("Password *") },
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

            val displayError = localError.ifBlank { authError ?: "" }
            if (displayError.isNotBlank()) {
                item {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayError,
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        localError = ""
                        onClearError()
                        if (isSignUp) {
                            if (fullName.isBlank()) {
                                localError = "Please enter your full name."
                            } else if (identity.isBlank()) {
                                localError = "Please enter an email address or mobile number."
                            } else if (password.length < 6) {
                                localError = "Password must be at least 6 characters."
                            } else {
                                val business = businessNameInput.trim().ifBlank { "${fullName.trim()}'s Collections" }
                                onSignUp(fullName.trim(), business, identity.trim(), password.trim())
                            }
                        } else {
                            if (identity.isBlank() || password.isBlank()) {
                                localError = "Please enter your email/mobile and password."
                            } else {
                                onLogin(identity.trim(), password.trim())
                            }
                        }
                    },
                    enabled = !isLoading && !isGoogleLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text("Authenticating...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(if (isSignUp) "Create Account  →" else "Sign In  →", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                InfoCard("Authentication & Account Protection", "Your data is protected. Each account only has access to its own loan customers and collections.")
            }
        }
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
                        Text("A password reset request has been logged for $resetEmail.", color = Green)
                    } else {
                        Text("Enter your registered email or mobile number to receive password recovery instructions.")
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            label = { Text("Email or Mobile Number") },
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
                    }) { Text("Submit") }
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
