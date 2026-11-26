package com.example.credflow

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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
    onUpdateServerUrl: (String) -> Unit = {},
    onTestConnection: ((Result<String>) -> Unit) -> Unit = {},
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
    var showServerConfigDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
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
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = try {
                    credentialManager.getCredential(request = request, context = activityContext)
                } catch (e: GetCredentialException) {
                    val msg = e.message ?: ""
                    if (e is NoCredentialException ||
                        msg.contains("NoCredentialException", ignoreCase = true) ||
                        msg.contains("No credentials", ignoreCase = true) ||
                        msg.contains("Cannot find a matching credential", ignoreCase = true)
                    ) {
                        // Fallback: explicit Sign In with Google flow
                        val fallbackOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId)
                            .build()
                        val fallbackRequest = GetCredentialRequest.Builder()
                            .addCredentialOption(fallbackOption)
                            .build()
                        credentialManager.getCredential(request = fallbackRequest, context = activityContext)
                    } else {
                        throw e
                    }
                }

                val credential = response.credential

                val idToken = try {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } else {
                        credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                            ?: credential.data.getString("id_token")
                            ?: ""
                    }
                } catch (_: Exception) {
                    credential.data.getString("id_token") ?: ""
                }

                if (!idToken.isNullOrBlank()) {
                    onGoogleSignInToken(idToken)
                } else {
                    localError = "Google returned an empty ID token. Please try again."
                }
            } catch (_: GetCredentialCancellationException) {
                // User dismissed or tapped outside the account chooser
            } catch (e: GetCredentialException) {
                val msg = e.message ?: ""
                localError = when {
                    msg.contains("28444") || msg.contains("Developer console is not set up correctly", ignoreCase = true) -> {
                        "Google Sign-In: [28444] Developer console setup required.\n\nIn Google Cloud Console under project 930439631272:\n1. Go to APIs & Services -> Credentials\n2. Click '+ CREATE CREDENTIALS' -> 'OAuth client ID'\n3. Application type: 'Android'\n4. Package name: com.example.credflow\n5. SHA-1: 9A:73:6E:4C:74:31:F1:35:98:C0:36:5D:64:8D:36:7F:99:39:7D:48\n6. Save and allow 2-5 minutes to propagate.\n\nNote: If already added, clear Google Play services cache in phone Settings > Apps."
                    }
                    msg.contains("28404") || msg.contains("No credentials available", ignoreCase = true) -> {
                        "Google Sign-In: [28404] No Google account found or account not authorized.\n\n1. Ensure a Google account is logged in on this phone in Settings > Passwords & Accounts.\n2. In Google Cloud Console, ensure your email is added under 'OAuth consent screen' -> 'Test users'."
                    }
                    else -> "Google Sign-In: $msg"
                }
            } catch (e: Exception) {
                localError = "Google Sign-In: ${e.message ?: "Could not open Google account picker."}"
            } finally {
                isGoogleLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column {
                    Text("Credflow Auth", color = Muted, fontSize = 11.sp)
                    Text(if (isSignUp) "Create Account" else "Sign In", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Navy)
                }
            }
            IconButton(onClick = { showServerConfigDialog = true }) {
                Icon(Icons.Default.Dns, contentDescription = "Server Settings", tint = Navy)
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

            val displayError = localError.ifBlank { authError ?: "" }
            if (displayError.isNotBlank()) {
                item {
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = displayError,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            if (displayError.contains("28444") || displayError.contains("28404")) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 Instant Login: You can sign in immediately using Email / Mobile Number and Password below without waiting for Google Cloud Console setup.",
                                        color = Color(0xFF2E7D32),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            if (displayError.contains("timed out", ignoreCase = true) ||
                                displayError.contains("Cannot connect", ignoreCase = true) ||
                                displayError.contains("host could not be reached", ignoreCase = true)
                            ) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showServerConfigDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Navy)
                                ) {
                                    Icon(Icons.Default.SettingsEthernet, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Change Server IP / Connection Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
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

    if (showServerConfigDialog) {
        var serverUrlInput by remember { mutableStateOf(ApiConfig.baseUrl) }
        var isTesting by remember { mutableStateOf(false) }
        var testResultMsg by remember { mutableStateOf<String?>(null) }
        var isTestSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isTesting) showServerConfigDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = Blue, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Backend Server Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Configure the PHP API server URL for your network environment.",
                        fontSize = 12.sp,
                        color = Muted
                    )

                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = {
                            serverUrlInput = it
                            testResultMsg = null
                        },
                        label = { Text("API Server Base URL *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Quick Presets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = serverUrlInput == ApiConfig.DEFAULT_BASE_URL,
                            onClick = {
                                serverUrlInput = ApiConfig.DEFAULT_BASE_URL
                                testResultMsg = null
                            },
                            label = { Text("PC Wi-Fi", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = serverUrlInput == ApiConfig.USB_BASE_URL,
                            onClick = {
                                serverUrlInput = ApiConfig.USB_BASE_URL
                                testResultMsg = null
                            },
                            label = { Text("USB / Cable", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = serverUrlInput == ApiConfig.EMULATOR_BASE_URL,
                            onClick = {
                                serverUrlInput = ApiConfig.EMULATOR_BASE_URL
                                testResultMsg = null
                            },
                            label = { Text("Emulator", fontSize = 11.sp) }
                        )
                    }

                    if (!testResultMsg.isNullOrBlank()) {
                        Surface(
                            color = if (isTestSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResultMsg ?: "",
                                color = if (isTestSuccess) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            testResultMsg = null
                            val candidate = ApiConfig.sanitizeUrl(serverUrlInput)
                            ApiConfig.baseUrl = candidate
                            onTestConnection { res ->
                                isTesting = false
                                res.onSuccess { msg ->
                                    isTestSuccess = true
                                    testResultMsg = "Connection successful!\n$msg"
                                }.onFailure { err ->
                                    isTestSuccess = false
                                    testResultMsg = "Connection failed:\n${err.message ?: "Unknown error"}"
                                }
                            }
                        },
                        enabled = !isTesting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Blue)
                            Spacer(Modifier.width(8.dp))
                            Text("Testing Reachability...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Test Connection to PHP API", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sanitized = ApiConfig.sanitizeUrl(serverUrlInput)
                        onUpdateServerUrl(sanitized)
                        showServerConfigDialog = false
                    },
                    enabled = !isTesting
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showServerConfigDialog = false },
                    enabled = !isTesting
                ) {
                    Text("Close")
                }
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
