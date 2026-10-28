package com.example.credflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.repository.CredflowRepository
import com.example.credflow.domain.model.Customer
import com.example.credflow.rupees
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CredflowViewModel(
    private val repository: CredflowRepository,
    private val sessionManager: UserSessionManager,
    private val apiService: CredflowApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CredflowUiState(
            businessName = sessionManager.businessName,
            ownerName = sessionManager.userName,
            ownerPhone = if (sessionManager.userEmailOrPhone.contains("@")) "9876543210" else sessionManager.userEmailOrPhone,
            ownerEmail = if (sessionManager.userEmailOrPhone.contains("@")) sessionManager.userEmailOrPhone else "ramesh@credflow.com"
        )
    )
    val uiState: StateFlow<CredflowUiState> = _uiState.asStateFlow()

    init {
        // Collect customers from Room cache
        viewModelScope.launch {
            repository.customers.collect { customerList ->
                _uiState.update { it.copy(customers = customerList) }
            }
        }

        // Collect payments from Room cache
        viewModelScope.launch {
            repository.payments.collect { paymentList ->
                _uiState.update { it.copy(payments = paymentList) }
            }
        }

        // Collect pending operations count
        viewModelScope.launch {
            repository.pendingOperationsCount.collect { count ->
                _uiState.update { it.copy(pendingOperationsCount = count) }
            }
        }

        // Fetch fresh data from PHP/MySQL server on startup only if authenticated
        if (sessionManager.isLoggedIn) {
            refreshData()
            validateSession()
        }
    }

    private fun validateSession() {
        val token = sessionManager.authToken ?: return
        viewModelScope.launch {
            val res = apiService.validateSession(token)
            if (res.isFailure) {
                repository.clearLocalData()
                sessionManager.clearSession()
                _uiState.update {
                    it.copy(
                        customers = emptyList(),
                        payments = emptyList(),
                        userMessage = "Session expired. Please log in again."
                    )
                }
            }
        }
    }

    fun refreshData() {
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            // Sync pending offline operations first
            repository.syncPendingOperations()

            // Fetch live customers & payments
            val customerRes = repository.refreshCustomers()
            val paymentRes = repository.refreshPayments()

            val isOffline = customerRes.isFailure && paymentRes.isFailure
            val msg = when {
                customerRes.isSuccess -> "Synced with MySQL (${customerRes.getOrNull()?.size ?: 0} customers)."
                isOffline -> "Offline mode: Showing cached data from device."
                else -> "Partial sync completed."
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isOffline = isOffline,
                    userMessage = msg
                )
            }
        }
    }

    fun selectCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun addCustomer(
        name: String,
        phone: String,
        amountStr: String,
        cycle: String,
        day: String,
        note: String,
        onSuccess: (Customer) -> Unit
    ) {
        val parsedAmount = amountStr.toDoubleOrNull()
        if (name.isBlank() || phone.isBlank() || parsedAmount == null || parsedAmount <= 0) {
            _uiState.update { it.copy(userMessage = "Enter customer name, phone and a valid amount.") }
            return
        }

        val collectionDay = if (cycle == "Daily") "Daily" else if (cycle == "Monthly") "Monthly" else day
        val newCustomer = Customer(
            name = name.trim(),
            phone = phone.trim(),
            amount = parsedAmount,
            cycle = cycle,
            day = collectionDay,
            totalDue = parsedAmount,
            note = note.trim()
        )

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val res = repository.addCustomer(newCustomer)
            _uiState.update { it.copy(isLoading = false) }

            res.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        selectedCustomer = saved,
                        userMessage = if (saved.id > 0) "Customer saved to MySQL database." else "Saved locally (queued for sync)."
                    )
                }
                onSuccess(saved)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Error saving customer: ${err.message}") }
            }
        }
    }

    fun recordPayment(
        customer: Customer,
        amountStr: String,
        method: String,
        onSuccess: (Customer) -> Unit
    ) {
        val value = amountStr.toDoubleOrNull()
        if (value == null || value <= 0) {
            _uiState.update { it.copy(userMessage = "Enter a valid collection amount.") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val res = repository.recordPayment(customer.id, customer.name, value, method)
            _uiState.update { it.copy(isLoading = false) }

            res.onSuccess { payment ->
                val updatedCustomer = customer.copy(
                    paid = customer.paid + value,
                    due = (customer.due - value).coerceAtLeast(0.0)
                )
                _uiState.update {
                    it.copy(
                        paidCustomer = updatedCustomer,
                        selectedCustomer = updatedCustomer,
                        userMessage = "Collection of ${rupees(value)} recorded via $method."
                    )
                }
                onSuccess(updatedCustomer)
            }.onFailure { err ->
                _uiState.update { it.copy(userMessage = "Error recording payment: ${err.message}") }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun askAi(question: String) {
        val q = question.lowercase()
        val customers = _uiState.value.customers
        val payments = _uiState.value.payments

        val answer = when {
            "overdue" in q || "pending" in q -> {
                val pendingCount = customers.count { it.due > 0 }
                val pendingAmount = customers.sumOf { it.due }
                "There are $pendingCount customers with an outstanding balance of ${rupees(pendingAmount)} across your active accounts."
            }
            "today" in q || "daily" in q -> {
                val daily = customers.filter { it.cycle == "Daily" }
                "Daily customers: ${daily.size}. Remaining due: ${rupees(daily.sumOf { it.due })}."
            }
            "week" in q || "weekly" in q -> {
                val weekly = customers.filter { it.cycle == "Weekly" }
                "Weekly customers: ${weekly.size}. Remaining due: ${rupees(weekly.sumOf { it.due })}."
            }
            "month" in q || "monthly" in q -> {
                val monthly = customers.filter { it.cycle == "Monthly" }
                "Monthly customers: ${monthly.size}. Remaining due: ${rupees(monthly.sumOf { it.due })}."
            }
            "collect" in q || "paid" in q -> {
                "Total collections recorded: ${payments.size}. Total amount collected: ${rupees(customers.sumOf { it.paid })}."
            }
            else -> {
                "Try asking: “How much is pending?”, “Show weekly customers”, or “How much remains for daily customers?” This information is computed from your live MySQL records."
            }
        }
        _uiState.update { it.copy(aiAnswer = answer) }
    }

    fun login(identity: String, pass: String, onSuccess: () -> Unit) {
        val cleanIdentity = identity.trim()
        val cleanPass = pass.trim()

        if (cleanIdentity.isBlank() || cleanPass.isBlank()) {
            _uiState.update { it.copy(authError = "Enter a valid email/phone and password.") }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true, authError = null) }

        viewModelScope.launch {
            val res = apiService.login(cleanIdentity, cleanPass)
            res.onSuccess { auth ->
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = auth.name.ifBlank { "User" },
                    identity = cleanIdentity,
                    businessName = auth.businessName.ifBlank { "Credflow Finance" }
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = auth.name.ifBlank { it.ownerName },
                        businessName = auth.businessName.ifBlank { it.businessName },
                        userMessage = "Welcome back, ${auth.name}!"
                    )
                }
                refreshData()
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = err.message ?: "Login failed. Please check your credentials."
                    )
                }
            }
        }
    }

    fun signUp(name: String, businessName: String, identity: String, pass: String, onSuccess: () -> Unit) {
        val cleanName = name.trim()
        val cleanIdentity = identity.trim()
        val cleanPass = pass.trim()
        val cleanBusiness = businessName.trim().ifBlank { "$cleanName's Collections" }

        if (cleanName.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter your full name.") }
            return
        }
        if (cleanIdentity.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter an email address or mobile number.") }
            return
        }
        if (cleanPass.length < 6) {
            _uiState.update { it.copy(authError = "Password must be at least 6 characters.") }
            return
        }

        _uiState.update { it.copy(isAuthenticating = true, authError = null) }

        viewModelScope.launch {
            val res = apiService.register(cleanName, cleanBusiness, cleanIdentity, cleanPass)
            res.onSuccess { auth ->
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = cleanName,
                    identity = cleanIdentity,
                    businessName = cleanBusiness
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = cleanName,
                        businessName = cleanBusiness,
                        userMessage = "Account registered successfully!"
                    )
                }
                refreshData()
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = err.message ?: "Registration failed."
                    )
                }
            }
        }
    }

    fun googleSignInWithToken(idToken: String, onSuccess: () -> Unit) {
        _uiState.update { it.copy(isAuthenticating = true, authError = null) }
        viewModelScope.launch {
            val res = apiService.googleAuth(idToken)
            res.onSuccess { auth ->
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = auth.name,
                    identity = "",
                    businessName = auth.businessName
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = auth.name,
                        businessName = auth.businessName,
                        userMessage = "Signed in with Google successfully!"
                    )
                }
                refreshData()
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = err.message ?: "Google Sign-In verification failed on server."
                    )
                }
            }
        }
    }

    fun setAuthError(message: String) {
        _uiState.update { it.copy(isAuthenticating = false, authError = message) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val token = sessionManager.authToken
            apiService.logout(token)
            repository.clearLocalData()
            sessionManager.clearSession()
            _uiState.update {
                CredflowUiState(
                    businessName = "Credflow Finance",
                    ownerName = "User",
                    ownerPhone = "",
                    ownerEmail = "",
                    customers = emptyList(),
                    payments = emptyList(),
                    userMessage = "Logged out successfully."
                )
            }
            onSuccess()
        }
    }

    fun googleSignIn(name: String, email: String, onSuccess: () -> Unit) {
        val bName = "${name.substringBefore(" ")}'s Finance Book"
        val oauthPass = "google_auth_${email.lowercase().trim().hashCode()}"
        viewModelScope.launch {
            val res = apiService.register(name, bName, email, oauthPass)
            if (res.isSuccess) {
                val auth = res.getOrThrow()
                sessionManager.saveSession(auth.userId, auth.token, name, email, bName)
                _uiState.update {
                    it.copy(
                        ownerName = name,
                        ownerEmail = email,
                        businessName = bName,
                        userMessage = "Signed in as $email"
                    )
                }
                refreshData()
                onSuccess()
            } else {
                val loginRes = apiService.login(email, oauthPass)
                if (loginRes.isSuccess) {
                    val auth = loginRes.getOrThrow()
                    sessionManager.saveSession(auth.userId, auth.token, auth.name.ifBlank { name }, email, auth.businessName.ifBlank { bName })
                    _uiState.update {
                        it.copy(
                            ownerName = auth.name.ifBlank { name },
                            ownerEmail = email,
                            businessName = auth.businessName.ifBlank { bName },
                            userMessage = "Signed in as $email"
                        )
                    }
                    refreshData()
                    onSuccess()
                } else {
                    _uiState.update { it.copy(userMessage = "Could not sign in with Google account. Check server connection.") }
                }
            }
        }
    }

    fun updateProfile(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        businessAddress: String,
        gstinNumber: String
    ) {
        _uiState.update {
            it.copy(
                businessName = businessName,
                ownerName = ownerName,
                ownerPhone = ownerPhone,
                ownerEmail = ownerEmail,
                businessAddress = businessAddress,
                gstinNumber = gstinNumber,
                userMessage = "Business profile saved successfully."
            )
        }
    }

    fun updateSettings(
        acceptCash: Boolean,
        acceptUpi: Boolean,
        acceptBank: Boolean,
        dailyReminders: Boolean,
        overdueAlerts: Boolean,
        defaultCycle: String,
        appLockEnabled: Boolean,
        lateFeeRule: String
    ) {
        _uiState.update {
            it.copy(
                acceptCash = acceptCash,
                acceptUpi = acceptUpi,
                acceptBank = acceptBank,
                dailyReminders = dailyReminders,
                overdueAlerts = overdueAlerts,
                defaultCycle = defaultCycle,
                appLockEnabled = appLockEnabled,
                lateFeeRule = lateFeeRule,
                userMessage = "Settings saved successfully."
            )
        }
    }

    fun clearData() {
        viewModelScope.launch {
            repository.clearLocalData()
            _uiState.update {
                it.copy(
                    customers = emptyList(),
                    payments = emptyList(),
                    userMessage = "All local cached records cleared."
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            repository: CredflowRepository,
            sessionManager: UserSessionManager,
            apiService: CredflowApiService
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CredflowViewModel(repository, sessionManager, apiService) as T
            }
        }
    }
}

