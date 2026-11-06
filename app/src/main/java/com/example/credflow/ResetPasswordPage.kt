package com.example.credflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResetPasswordPage(
    onUpdatePassword: (String, String, (Result<String>) -> Unit) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Reset Your Password", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Navy)
        Text(
            "Verify your current password before choosing a new one.",
            color = Muted,
            fontSize = 13.sp
        )

        PasswordInput(
            value = currentPassword,
            onValueChange = { currentPassword = it; errorMessage = null; successMessage = null },
            label = "Current Password",
            visible = showCurrentPassword,
            onToggleVisibility = { showCurrentPassword = !showCurrentPassword }
        )
        PasswordInput(
            value = newPassword,
            onValueChange = { newPassword = it; errorMessage = null; successMessage = null },
            label = "New Password",
            visible = showNewPassword,
            onToggleVisibility = { showNewPassword = !showNewPassword }
        )
        PasswordInput(
            value = confirmPassword,
            onValueChange = { confirmPassword = it; errorMessage = null; successMessage = null },
            label = "Confirm New Password",
            visible = showConfirmPassword,
            onToggleVisibility = { showConfirmPassword = !showConfirmPassword }
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }
        successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }

        Button(
            onClick = {
                val validationError = validatePasswordChange(
                    currentPassword,
                    newPassword,
                    confirmPassword
                )
                if (validationError != null) {
                    errorMessage = validationError
                    return@Button
                }

                isSubmitting = true
                errorMessage = null
                successMessage = null
                onUpdatePassword(currentPassword, newPassword) { result ->
                    isSubmitting = false
                    result.onSuccess { message ->
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        successMessage = message
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Could not update the password."
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Update Password")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

internal fun validatePasswordChange(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String
): String? = when {
    currentPassword.isBlank() -> "Enter your current password."
    newPassword.isBlank() -> "Enter a new password."
    newPassword.length < 6 -> "New password must be at least 6 characters."
    newPassword.toByteArray(Charsets.UTF_8).size > 72 ->
        "New password must be no more than 72 UTF-8 bytes."
    newPassword == currentPassword -> "New password must differ from the current password."
    confirmPassword.isBlank() -> "Confirm your new password."
    newPassword != confirmPassword -> "New password and confirmation do not match."
    else -> null
}

@Composable
private fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Hide $label" else "Show $label"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
