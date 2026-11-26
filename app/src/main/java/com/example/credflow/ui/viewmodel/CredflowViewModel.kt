package com.example.credflow.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.remote.SessionExpiredException
import com.example.credflow.data.remote.dto.CloseLoanRequestDto
import com.example.credflow.data.remote.dto.CloseLoanResponseDto
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.ExportReportDataDto
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import com.example.credflow.data.repository.CredflowRepository
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.InstallmentScheduleItem
import com.example.credflow.domain.model.LoanCalculator
import com.example.credflow.domain.model.Payment
import com.example.credflow.rupees
import com.example.credflow.util.LogoutCoordinator
import com.example.credflow.util.OfflineChatHelper
import com.example.credflow.util.PhoneUtils
import com.example.credflow.util.ScheduleUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CredflowViewModel(
    private val repository: CredflowRepository,
    private val sessionManager: UserSessionManager,
    private val apiService: CredflowApiService,
    private val appSettings: com.example.credflow.data.local.AppSettingsManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CredflowUiState(
            businessName = sessionManager.businessName,
            ownerName = sessionManager.userName,
            ownerPhone = sessionManager.userPhone.ifBlank {
                if (!sessionManager.userEmailOrPhone.contains("@")) sessionManager.userEmailOrPhone else ""
            },
            ownerEmail = sessionManager.userEmail.ifBlank {
                if (sessionManager.userEmailOrPhone.contains("@")) sessionManager.userEmailOrPhone else ""
            },
            businessAddress = sessionManager.businessAddress,
            gstinNumber = sessionManager.gstinNumber,
            acceptCash = appSettings?.acceptCash ?: true,
            acceptUpi = appSettings?.acceptUpi ?: true,
            acceptBank = appSettings?.acceptBank ?: false,
            dailyReminders = appSettings?.dailyReminders ?: true,
            reminderHour = appSettings?.reminderHour ?: 9,
            reminderMinute = appSettings?.reminderMinute ?: 0,
            overdueAlerts = appSettings?.overdueAlerts ?: true,
            defaultCycle = appSettings?.defaultCycle ?: "Daily",
            lateFeeRule = appSettings?.lateFeeRule ?: "No Charge"
        )
    )
    val uiState: StateFlow<CredflowUiState> = _uiState.asStateFlow()

    private val logoutCoordinator = LogoutCoordinator()
    private var syncJob: Job? = null
    private var validateSessionJob: Job? = null

    init {
        // Collect customers from Room cache
        viewModelScope.launch {
            repository.customers.collect { customerList ->
                _uiState.update { current ->
                    val selected = if (current.selectedCustomer != null) {
                        customerList.find { it.id == current.selectedCustomer.id } ?: current.selectedCustomer
                    } else null
                    current.copy(
                        customers = customerList,
                        selectedCustomer = selected
                    )
                }
            }
        }

        // Collect payments from Room cache - deduplicate by id
        viewModelScope.launch {
            repository.payments.collect { paymentList ->
                val deduplicated = paymentList.distinctBy { it.id }
                _uiState.update { it.copy(payments = deduplicated) }
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
        validateSessionJob?.cancel()
        validateSessionJob = viewModelScope.launch {
            val res = apiService.validateSession(token)
            res.onSuccess { auth ->
                sessionManager.saveProfile(
                    name = auth.name,
                    bName = auth.businessName,
                    phone = auth.phone,
                    email = auth.email,
                    address = auth.businessAddress,
                    gstin = auth.gstinNumber
                )
                _uiState.update {
                    it.copy(
                        ownerName = auth.name.ifBlank { it.ownerName },
                        businessName = auth.businessName.ifBlank { it.businessName },
                        ownerPhone = auth.phone.ifBlank { it.ownerPhone },
                        ownerEmail = auth.email.ifBlank { it.ownerEmail },
                        businessAddress = auth.businessAddress,
                        gstinNumber = auth.gstinNumber
                    )
                }
            }.onFailure { err ->
                if (err is com.example.credflow.data.remote.SessionExpiredException) {
                    repository.clearLocalData()
                    sessionManager.clearSession()
                    _uiState.update {
                        it.copy(
                            customers = emptyList(),
                            payments = emptyList(),
                            userMessage = "Session expired. Please log in again."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isOffline = true,
                            userMessage = "Offline mode: Showing cached data from device."
                        )
                    }
                }
            }
        }
    }

    fun refreshData() {
        if (!sessionManager.isLoggedIn || _uiState.value.isSyncing || logoutCoordinator.isLoggingOut()) return
        _uiState.update { it.copy(isSyncing = true, refreshError = null) }
        syncJob = viewModelScope.launch {
            logoutCoordinator.awaitPendingLocalCleanup()
            if (!sessionManager.isLoggedIn) {
                _uiState.update { it.copy(isSyncing = false) }
                return@launch
            }
            // Sync pending offline operations first
            repository.syncPendingOperations()

            // Fetch live customers & payments
            val customerRes = repository.refreshCustomers()
            val paymentRes = repository.refreshPayments()

            val failureErr = customerRes.exceptionOrNull() ?: paymentRes.exceptionOrNull()
            if (failureErr is SessionExpiredException) {
                _uiState.update { it.copy(isSyncing = false) }
                handleSessionExpired()
                return@launch
            }

            val isOffline = customerRes.isFailure && paymentRes.isFailure
            val errMsg = failureErr?.message?.takeIf { it.isNotBlank() }
                ?: if (customerRes.isFailure || paymentRes.isFailure) "Unable to reach server. Check your connection." else null
            val msg = when {
                customerRes.isSuccess && paymentRes.isSuccess -> "Synced with MySQL (${customerRes.getOrNull()?.size ?: 0} customers)."
                customerRes.isSuccess -> "Customers synced, but payments failed to refresh."
                else -> errMsg
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    isOffline = isOffline,
                    refreshError = if (customerRes.isFailure) errMsg else null,
                    userMessage = msg
                )
            }
            if (customerRes.isSuccess) {
                loadCollectionForecast()
                loadReports()
                loadNotifications()
            }
        }
    }

    fun clearRefreshError() {
        _uiState.update { it.copy(refreshError = null) }
    }

    fun refreshAiData() {
        if (_uiState.value.isRefreshingAi) return
        _uiState.update { it.copy(isRefreshingAi = true, refreshError = null) }
        viewModelScope.launch {
            val customerRes = repository.refreshCustomers()
            val paymentRes = repository.refreshPayments()
            val err = customerRes.exceptionOrNull() ?: paymentRes.exceptionOrNull()
            if (err is SessionExpiredException) {
                _uiState.update { it.copy(isRefreshingAi = false) }
                handleSessionExpired()
                return@launch
            }
            val currentPeriod = (_uiState.value.forecastState as? ForecastUiState.Success)?.selectedPeriod ?: "all"
            runRepaymentPrediction()
            loadCollectionForecast(currentPeriod)
            _uiState.update {
                it.copy(
                    isRefreshingAi = false,
                    isOffline = customerRes.isFailure && paymentRes.isFailure,
                    userMessage = if (customerRes.isSuccess) "AI predictions and forecasts refreshed." else (err?.message ?: "Could not refresh backend data for AI.")
                )
            }
        }
    }

    private val _installments = MutableStateFlow<List<InstallmentScheduleItem>>(emptyList())
    val installments: StateFlow<List<InstallmentScheduleItem>> = _installments.asStateFlow()

    fun selectCustomer(customer: Customer?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
        if (customer != null && customer.id > 0) {
            loadInstallments(customer.id)
        } else {
            _installments.value = emptyList()
        }
    }

    fun loadInstallments(customerId: Int) {
        viewModelScope.launch {
            val res = apiService.getInstallments(customerId, sessionManager.authToken)
            res.onSuccess { list ->
                _installments.value = list
            }.onFailure {
                val cust = _uiState.value.selectedCustomer
                if (cust != null && cust.id == customerId) {
                    if (cust.isClosed) {
                        _installments.value = emptyList()
                    } else {
                        val calc = LoanCalculator.calculateLoan(
                            principal = cust.amount,
                            interestRate = cust.interest,
                            period = cust.period,
                            periodUnit = cust.periodUnit,
                            frequency = cust.cycle,
                            collectionDay = cust.day,
                            firstDueDateStr = cust.firstDueDate
                        )
                        _installments.value = calc.schedule
                    }
                }
            }
        }
    }

    fun addCustomer(
        name: String,
        phone: String,
        amountStr: String,
        interestStr: String = "0",
        interestType: String = "Flat Interest",
        periodStr: String = "1",
        periodUnit: String = "Months",
        cycle: String,
        day: String,
        firstDueDate: String = "",
        note: String,
        onSuccess: (Customer) -> Unit
    ) {
        if (_uiState.value.isSavingCustomer) return

        val parsedAmount = amountStr.toDoubleOrNull()
        if (name.isBlank() || phone.isBlank()) {
            _uiState.update { it.copy(userMessage = "Customer name and phone number are required.") }
            return
        }
        val normalizedPhone = PhoneUtils.normalizePhone(phone)
        if (normalizedPhone.length < 6) {
            _uiState.update { it.copy(userMessage = "Enter a valid phone number.") }
            return
        }
        val duplicate = _uiState.value.customers.firstOrNull {
            PhoneUtils.phonesMatch(it.phone, phone)
        }
        if (duplicate != null) {
            _uiState.update {
                it.copy(userMessage = "Customer '${duplicate.name}' with this phone number already exists.")
            }
            return
        }
        if (parsedAmount == null || parsedAmount <= 0.0) {
            _uiState.update { it.copy(userMessage = "Loan amount (Principal) must be greater than zero.") }
            return
        }
        val parsedInterest = interestStr.toDoubleOrNull() ?: 0.0
        if (parsedInterest < 0.0) {
            _uiState.update { it.copy(userMessage = "Interest rate cannot be negative.") }
            return
        }
        val parsedPeriod = periodStr.toIntOrNull() ?: 1
        if (parsedPeriod <= 0) {
            _uiState.update { it.copy(userMessage = "Repayment period must be at least 1.") }
            return
        }

        val normalizedCycle = ScheduleUtils.normalizeCycle(cycle)
        val collectionDay = when (normalizedCycle) {
            "Daily" -> "Daily"
            "Weekly" -> {
                val resolvedDay = ScheduleUtils.normalizeWeeklyDay(day, firstDueDate)
                if (resolvedDay.isBlank()) {
                    _uiState.update { it.copy(userMessage = "Please select a collection weekday (Monday to Sunday).") }
                    return
                }
                resolvedDay
            }
            else -> {
                val resolvedMonthly = ScheduleUtils.normalizeMonthlyDay(day, firstDueDate)
                if (resolvedMonthly.isBlank()) {
                    _uiState.update { it.copy(userMessage = "Please select a monthly due day or first due date.") }
                    return
                }
                resolvedMonthly
            }
        }
        val calc = LoanCalculator.calculateLoan(
            principal = parsedAmount,
            interestRate = parsedInterest,
            period = parsedPeriod,
            periodUnit = periodUnit,
            frequency = normalizedCycle,
            collectionDay = collectionDay,
            firstDueDateStr = firstDueDate
        )

        val newCustomer = Customer(
            name = name.trim(),
            phone = phone.trim(),
            amount = calc.principal,
            interest = calc.interestRate,
            interestType = interestType,
            period = parsedPeriod,
            periodUnit = periodUnit,
            installmentAmount = calc.installmentAmount,
            totalInstallments = calc.numberOfInstallments,
            firstDueDate = calc.firstDueDate,
            finalDueDate = calc.finalDueDate,
            cycle = normalizedCycle,
            day = collectionDay,
            totalDue = calc.totalRepayment,
            paid = 0.0,
            due = calc.totalRepayment,
            note = note.trim()
        )

        val idempotencyKey = UUID.randomUUID().toString()
        _uiState.update { it.copy(isLoading = true, isSavingCustomer = true) }
        viewModelScope.launch {
            val res = repository.addCustomer(newCustomer, idempotencyKey)
            _uiState.update { it.copy(isLoading = false, isSavingCustomer = false) }

            res.onSuccess { saved ->
                _uiState.update {
                    it.copy(
                        selectedCustomer = saved,
                        lastCustomerId = if (saved.id > 0) saved.id else it.lastCustomerId,
                        userMessage = if (saved.id > 0) "Customer and loan schedule saved to MySQL." else "Saved locally (queued for sync)."
                    )
                }
                if (saved.id > 0) {
                    loadInstallments(saved.id)
                }
                runRepaymentPrediction()
                loadCollectionForecast()
                loadReports()
                onSuccess(saved)
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                } else {
                    _uiState.update { it.copy(userMessage = err.message ?: "Error saving customer.") }
                }
            }
        }
    }

    fun recordPayment(
        customer: Customer,
        amountStr: String,
        method: String,
        onSuccess: (Customer) -> Unit
    ) {
        if (_uiState.value.isRecordingPayment) return
        if (customer.isClosed) {
            _uiState.update { it.copy(userMessage = "This loan is already CLOSED. No further payments allowed.") }
            return
        }
        val value = amountStr.toDoubleOrNull()
        if (value == null || value <= 0) {
            _uiState.update { it.copy(userMessage = "Enter a valid collection amount.") }
            return
        }
        if (value > customer.due + 0.001) {
            _uiState.update {
                it.copy(userMessage = "Payment of ₹${String.format("%.2f", value)} exceeds remaining balance of ₹${String.format("%.2f", customer.due)}.")
            }
            return
        }

        val idempotencyKey = UUID.randomUUID().toString()
        _uiState.update { it.copy(isLoading = true, isRecordingPayment = true, lastPaymentReceipt = null) }
        viewModelScope.launch {
            val res = repository.recordPaymentWithReceipt(customer.id, customer.name, value, method, idempotencyKey)
            _uiState.update { it.copy(isLoading = false, isRecordingPayment = false) }

            res.onSuccess { (payment, serverReceipt) ->
                val newPaid = customer.paid + value
                val newDue = (customer.due - value).coerceAtLeast(0.0)
                val updatedCustomer = customer.copy(
                    paid = newPaid,
                    due = newDue
                )
                val effectiveReceipt = serverReceipt ?: PaymentReceiptDto(
                    paymentId = payment.id,
                    customerId = customer.id,
                    receiptNumber = payment.receiptNumber.ifBlank { "CF-LOCAL-${kotlin.math.abs(payment.id)}" },
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    paymentAmount = value,
                    paymentDate = payment.paymentDate,
                    paymentMethod = method,
                    totalLoanAmount = customer.totalDue,
                    totalPaidAmount = newPaid,
                    remainingBalance = newDue
                )
                _uiState.update { current ->
                    val updatedList = current.customers.map { c ->
                        if (c.id == customer.id) updatedCustomer else c
                    }
                    val updatedPayments = (listOf(payment) + current.payments).distinctBy { it.id }
                    current.copy(
                        customers = updatedList,
                        payments = updatedPayments,
                        paidCustomer = updatedCustomer,
                        selectedCustomer = updatedCustomer,
                        lastCustomerId = customer.id,
                        lastPaymentReceipt = effectiveReceipt,
                        userMessage = "Collection of ${rupees(value)} recorded via $method."
                    )
                }
                loadInstallments(customer.id)
                refreshData()
                runRepaymentPrediction()
                loadCollectionForecast()
                loadReports()
                loadNotifications()
                onSuccess(updatedCustomer)
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                _uiState.update { it.copy(userMessage = "Error recording payment: ${err.message}") }
            }
        }
    }

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        executeCustomerSearch(debounceMs = 250L)
    }

    fun updateSearchCycleFilter(cycle: String) {
        _uiState.update { it.copy(searchCycleFilter = cycle) }
        executeCustomerSearch(debounceMs = 0L)
    }

    fun updateSearchStatusFilter(status: String) {
        _uiState.update { it.copy(searchStatusFilter = status) }
        executeCustomerSearch(debounceMs = 0L)
    }

    fun updateSearchSortOption(sort: String) {
        _uiState.update { it.copy(searchSortOption = sort) }
        executeCustomerSearch(debounceMs = 0L)
    }

    fun clearSearchFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchCycleFilter = "All",
                searchStatusFilter = "All",
                searchSortOption = "name_asc"
            )
        }
        executeCustomerSearch(debounceMs = 0L)
    }

    fun executeCustomerSearch(debounceMs: Long = 0L) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            val current = _uiState.value
            _uiState.update { it.copy(isSearching = true, searchError = null) }
            val result = repository.searchCustomers(
                query = current.searchQuery,
                cycle = if (current.searchCycleFilter.equals("All", true)) "all" else current.searchCycleFilter,
                status = if (current.searchStatusFilter.equals("All", true)) "all" else current.searchStatusFilter,
                sort = current.searchSortOption
            )
            result.onSuccess { list ->
                _uiState.update {
                    it.copy(
                        searchResults = list,
                        isSearching = false,
                        searchError = null,
                        hasRemoteSearchResults = true
                    )
                }
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        searchError = err.message ?: "Failed to search customers from MySQL."
                    )
                }
            }
        }
    }

    fun fetchPaymentReceipt(paymentId: Int, onResult: (Result<PaymentReceiptDto>) -> Unit) {
        if (paymentId <= 0) {
            onResult(Result.failure(IllegalArgumentException("Receipt is only available after payment syncs to MySQL.")))
            return
        }
        viewModelScope.launch {
            val res = repository.getPaymentReceipt(paymentId)
            res.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
            }
            onResult(res)
        }
    }

    fun clearLoanClosureError() {
        _uiState.update { it.copy(loanClosureError = null) }
    }

    fun closeLoan(
        customer: Customer,
        settlementType: String,
        settlementAmountStr: String,
        paymentMethod: String,
        note: String,
        closeLoanFlag: Boolean,
        onSuccess: (CloseLoanResponseDto) -> Unit
    ) {
        if (_uiState.value.isClosingLoan) return
        if (customer.isClosed) {
            _uiState.update { it.copy(loanClosureError = "This loan is already CLOSED.") }
            return
        }

        val amount = settlementAmountStr.toDoubleOrNull() ?: 0.0
        if (amount < 0.0) {
            _uiState.update { it.copy(loanClosureError = "Settlement amount cannot be negative.") }
            return
        }
        if (customer.due > 0.001 && amount <= 0.0) {
            _uiState.update { it.copy(loanClosureError = "Enter a valid settlement amount greater than zero.") }
            return
        }
        if (amount > customer.due + 0.001) {
            _uiState.update {
                it.copy(loanClosureError = "Settlement amount exceeds remaining balance of ${rupees(customer.due)}.")
            }
            return
        }

        val remainingAfter = (customer.due - amount).coerceAtLeast(0.0)
        if (closeLoanFlag && remainingAfter > 0.001) {
            _uiState.update {
                it.copy(
                    loanClosureError = "Cannot close loan while ${rupees(remainingAfter)} remains unpaid. Enter the full remaining balance to close, or record a Partial Settlement."
                )
            }
            return
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val request = CloseLoanRequestDto(
            customerId = customer.id,
            settlementType = settlementType,
            settlementAmount = amount,
            paymentMethod = paymentMethod,
            paymentDate = today,
            note = note.trim(),
            closeLoan = closeLoanFlag,
            idempotencyKey = UUID.randomUUID().toString()
        )

        _uiState.update { it.copy(isClosingLoan = true, loanClosureError = null) }
        viewModelScope.launch {
            val result = repository.closeLoan(request)
            _uiState.update { it.copy(isClosingLoan = false) }
            result.onSuccess { resp ->
                val updatedCustomer = customer.copy(
                    paid = resp.paidAmount,
                    due = resp.remainingBalance,
                    loanStatus = resp.loanStatus,
                    closedAt = resp.closedAt,
                    settlementAmount = resp.settlementAmount,
                    settlementType = resp.settlementType,
                    closureNote = note.trim()
                )
                _uiState.update { current ->
                    current.copy(
                        customers = current.customers.map { if (it.id == customer.id) updatedCustomer else it },
                        selectedCustomer = updatedCustomer,
                        paidCustomer = updatedCustomer,
                        lastCustomerId = customer.id,
                        lastPaymentReceipt = resp.receipt ?: current.lastPaymentReceipt,
                        loanClosureError = null,
                        userMessage = resp.message.ifBlank { "Loan settlement recorded." }
                    )
                }
                loadInstallments(customer.id)
                loadCustomerLedger(customer.id)
                refreshData()
                runRepaymentPrediction()
                loadCollectionForecast()
                loadReports()
                loadNotifications()
                onSuccess(resp)
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                _uiState.update {
                    it.copy(loanClosureError = err.message ?: "Failed to close loan.")
                }
            }
        }
    }

    fun askAi(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return

        val prevCustId = _uiState.value.lastCustomerId
        android.util.Log.d("CredflowChat", "[ViewModel] askAi called | Question: '$trimmed' | Context CustId: $prevCustId")

        _uiState.update {
            it.copy(
                chatState = ChatUiState.Loading,
                lastAskedQuestion = trimmed
            )
        }

        viewModelScope.launch {
            val result = repository.askChatbot(trimmed, prevCustId)
            if (result.isSuccess) {
                val dto = result.getOrThrow()
                android.util.Log.d(
                    "CredflowChat",
                    "[ViewModel] Received LIVE response | Intent: ${dto.intent} | Lang: ${dto.language} | Live=true"
                )
                _uiState.update { current ->
                    current.copy(
                        chatState = ChatUiState.Success(
                            answer = dto.answer,
                            intent = dto.intent,
                            source = dto.source.ifBlank { "Credflow Live MySQL Financial Assistant" },
                            language = dto.language,
                            isOfflineFallback = false
                        ),
                        lastCustomerId = dto.customerId ?: current.lastCustomerId,
                        aiAnswer = dto.answer
                    )
                }
                if (dto.dataChanged) {
                    refreshData()
                    dto.customerId?.let { cid ->
                        loadInstallments(cid)
                        loadCustomerLedger(cid)
                    }
                    runRepaymentPrediction()
                    loadCollectionForecast()
                    loadReports()
                    loadNotifications()
                }
            } else {
                val exception = result.exceptionOrNull()
                if (exception is com.example.credflow.data.remote.SessionExpiredException) {
                    handleSessionExpired()
                }
                val errMsg = exception?.message?.takeIf { it.isNotBlank() }
                    ?: "Unable to connect to Credflow AI assistant (${com.example.credflow.data.remote.ApiConfig.baseUrl} / ${com.example.credflow.data.remote.ApiConfig.aiBaseUrl}). Check Wi-Fi and verify Apache & FastAPI are running on port ${com.example.credflow.data.remote.ApiConfig.AI_SERVICE_PORT}."
                android.util.Log.e("CredflowChat", "[ViewModel] Chat error displayed to user (no offline fallback): $errMsg")
                _uiState.update { current ->
                    current.copy(
                        chatState = ChatUiState.Error(
                            message = errMsg,
                            lastQuestion = trimmed
                        ),
                        aiAnswer = errMsg
                    )
                }
            }
        }
    }

    fun retryChat() {
        val lastQuestion = _uiState.value.lastAskedQuestion
        if (lastQuestion.isNotBlank()) {
            askAi(lastQuestion)
        }
    }

    private fun computeOfflineChatFallback(question: String): String {
        return OfflineChatHelper.computeFallbackReply(
            question = question,
            customers = _uiState.value.customers,
            payments = _uiState.value.payments
        )
    }

    fun login(identity: String, pass: String, onSuccess: () -> Unit) {
        val cleanIdentity = identity.trim()
        val cleanPass = pass.trim()

        if (cleanIdentity.isBlank() || cleanPass.isBlank()) {
            _uiState.update { it.copy(authError = "Enter a valid email/phone and password.") }
            return
        }

        logoutCoordinator.resetForNextSession()
        _uiState.update { it.copy(isAuthenticating = true, isLoggingOut = false, authError = null) }

        viewModelScope.launch {
            val res = apiService.login(cleanIdentity, cleanPass)
            res.onSuccess { auth ->
                logoutCoordinator.awaitPendingLocalCleanup()
                val userEmail = auth.email.ifBlank { if (cleanIdentity.contains("@")) cleanIdentity else "" }
                val userPhone = auth.phone.ifBlank { if (!cleanIdentity.contains("@")) cleanIdentity else "" }
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = auth.name.ifBlank { "User" },
                    identity = cleanIdentity,
                    businessName = auth.businessName.ifBlank { "Credflow Finance" },
                    phone = userPhone,
                    email = userEmail,
                    address = auth.businessAddress,
                    gstin = auth.gstinNumber
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = auth.name.ifBlank { it.ownerName },
                        ownerEmail = userEmail,
                        ownerPhone = userPhone,
                        businessName = auth.businessName.ifBlank { it.businessName },
                        businessAddress = auth.businessAddress,
                        gstinNumber = auth.gstinNumber,
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

        logoutCoordinator.resetForNextSession()
        _uiState.update { it.copy(isAuthenticating = true, isLoggingOut = false, authError = null) }

        viewModelScope.launch {
            val res = apiService.register(cleanName, cleanBusiness, cleanIdentity, cleanPass)
            res.onSuccess { auth ->
                logoutCoordinator.awaitPendingLocalCleanup()
                val regEmail = auth.email.ifBlank { if (cleanIdentity.contains("@")) cleanIdentity else "" }
                val regPhone = auth.phone.ifBlank { if (!cleanIdentity.contains("@")) cleanIdentity else "" }
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = cleanName,
                    identity = cleanIdentity,
                    businessName = cleanBusiness,
                    phone = regPhone,
                    email = regEmail,
                    address = auth.businessAddress,
                    gstin = auth.gstinNumber
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = cleanName,
                        ownerPhone = regPhone,
                        ownerEmail = regEmail,
                        businessName = cleanBusiness,
                        businessAddress = auth.businessAddress,
                        gstinNumber = auth.gstinNumber,
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
        logoutCoordinator.resetForNextSession()
        _uiState.update { it.copy(isAuthenticating = true, isLoggingOut = false, authError = null) }
        viewModelScope.launch {
            val res = apiService.googleAuth(idToken)
            res.onSuccess { auth ->
                logoutCoordinator.awaitPendingLocalCleanup()
                val userEmail = auth.email.ifBlank { if (auth.name.contains("@")) auth.name else "" }
                sessionManager.saveSession(
                    userId = auth.userId,
                    token = auth.token,
                    name = auth.name,
                    identity = userEmail.ifBlank { auth.name },
                    businessName = auth.businessName,
                    phone = auth.phone,
                    email = userEmail,
                    address = auth.businessAddress,
                    gstin = auth.gstinNumber
                )
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = null,
                        ownerName = auth.name,
                        ownerPhone = auth.phone,
                        ownerEmail = if (userEmail.isNotBlank()) userEmail else it.ownerEmail,
                        businessName = auth.businessName,
                        businessAddress = auth.businessAddress,
                        gstinNumber = auth.gstinNumber,
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

    fun updateServerUrl(url: String) {
        val sanitized = com.example.credflow.data.remote.ApiConfig.sanitizeUrl(url)
        com.example.credflow.data.remote.ApiConfig.baseUrl = sanitized
        sessionManager.customBaseUrl = sanitized
        _uiState.update { it.copy(authError = null, userMessage = "Server URL updated to $sanitized") }
    }

    fun testServerConnection(onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = apiService.testConnection()
            onComplete(res)
        }
    }

    fun logout(onSuccess: () -> Unit) {
        logoutCoordinator.performLogout(
            scope = viewModelScope,
            readToken = { sessionManager.authToken },
            cancelActiveSync = {
                syncJob?.cancel()
                validateSessionJob?.cancel()
            },
            clearSessionImmediate = {
                sessionManager.clearSession()
            },
            clearUiStateImmediate = {
                _installments.value = emptyList()
                _uiState.update {
                    CredflowUiState(
                        acceptCash = appSettings?.acceptCash ?: true,
                        acceptUpi = appSettings?.acceptUpi ?: true,
                        acceptBank = appSettings?.acceptBank ?: false,
                        dailyReminders = appSettings?.dailyReminders ?: true,
                        reminderHour = appSettings?.reminderHour ?: 9,
                        reminderMinute = appSettings?.reminderMinute ?: 0,
                        overdueAlerts = appSettings?.overdueAlerts ?: true,
                        defaultCycle = appSettings?.defaultCycle ?: "Daily",
                        lateFeeRule = appSettings?.lateFeeRule ?: "No Charge",
                        userMessage = "Logged out successfully."
                    )
                }
            },
            onNavigateToLogin = onSuccess,
            clearLocalCacheSuspend = {
                repository.clearLocalData()
            },
            callRemoteLogoutSuspend = { token ->
                apiService.logout(token)
            }
        )
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

    fun saveProfile(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        businessAddress: String,
        gstinNumber: String,
        onComplete: (Result<Unit>) -> Unit
    ) {
        val cleanBusiness = businessName.trim()
        val cleanOwner = ownerName.trim()
        val cleanPhone = ownerPhone.trim()
        val cleanEmail = ownerEmail.trim()
        val cleanAddress = businessAddress.trim()
        val cleanGstin = gstinNumber.trim()

        if (cleanBusiness.isBlank() || cleanOwner.isBlank()) {
            onComplete(Result.failure(IllegalArgumentException("Business name and owner name are required.")))
            return
        }

        viewModelScope.launch {
            val res = repository.updateProfile(
                businessName = cleanBusiness,
                ownerName = cleanOwner,
                ownerPhone = cleanPhone,
                ownerEmail = cleanEmail,
                businessAddress = cleanAddress,
                gstinNumber = cleanGstin
            )
            res.onSuccess { auth ->
                val savedBusiness = auth.businessName.ifBlank { cleanBusiness }
                val savedOwner = auth.name.ifBlank { cleanOwner }
                val savedPhone = auth.phone.ifBlank { cleanPhone }
                val savedEmail = auth.email.ifBlank { cleanEmail }
                val savedAddress = auth.businessAddress.ifBlank { cleanAddress }
                val savedGstin = auth.gstinNumber.ifBlank { cleanGstin }

                _uiState.update {
                    it.copy(
                        businessName = savedBusiness,
                        ownerName = savedOwner,
                        ownerPhone = savedPhone,
                        ownerEmail = savedEmail,
                        businessAddress = savedAddress,
                        gstinNumber = savedGstin,
                        userMessage = "Business profile saved to MySQL."
                    )
                }
                onComplete(Result.success(Unit))
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                val msg = err.message ?: "Failed to save profile."
                _uiState.update { it.copy(userMessage = msg) }
                onComplete(Result.failure(Exception(msg)))
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
        saveProfile(
            businessName = businessName,
            ownerName = ownerName,
            ownerPhone = ownerPhone,
            ownerEmail = ownerEmail,
            businessAddress = businessAddress,
            gstinNumber = gstinNumber
        ) {}
    }

    fun updateSettings(
        acceptCash: Boolean,
        acceptUpi: Boolean,
        acceptBank: Boolean,
        dailyReminders: Boolean,
        overdueAlerts: Boolean,
        defaultCycle: String,
        lateFeeRule: String,
        reminderHour: Int = _uiState.value.reminderHour,
        reminderMinute: Int = _uiState.value.reminderMinute
    ) {
        appSettings?.apply {
            this.acceptCash = acceptCash
            this.acceptUpi = acceptUpi
            this.acceptBank = acceptBank
            this.dailyReminders = dailyReminders
            this.overdueAlerts = overdueAlerts
            this.defaultCycle = defaultCycle
            this.lateFeeRule = lateFeeRule
            this.reminderHour = reminderHour
            this.reminderMinute = reminderMinute
        }
        _uiState.update {
            it.copy(
                acceptCash = acceptCash,
                acceptUpi = acceptUpi,
                acceptBank = acceptBank,
                dailyReminders = dailyReminders,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                overdueAlerts = overdueAlerts,
                defaultCycle = defaultCycle,
                lateFeeRule = lateFeeRule,
                userMessage = "Settings saved successfully."
            )
        }
    }

    fun prepareDataForExport(onReady: (List<Customer>, List<Payment>) -> Unit) {
        viewModelScope.launch {
            repository.refreshCustomers()
            repository.refreshPayments()
            val currentCustomers = _uiState.value.customers
            val currentPayments = _uiState.value.payments
            onReady(currentCustomers, currentPayments)
        }
    }

    fun resetServerData(onComplete: (Result<String>) -> Unit) {
        val token = sessionManager.authToken
        if (token.isNullOrBlank()) {
            onComplete(Result.failure(IllegalStateException("Not authenticated")))
            return
        }
        viewModelScope.launch {
            val res = apiService.resetServerAccountData(token)
            if (res.isSuccess) {
                repository.clearLocalData()
                _uiState.update {
                    it.copy(
                        customers = emptyList(),
                        payments = emptyList(),
                        userMessage = "All account data reset on server and device."
                    )
                }
            }
            onComplete(res)
        }
    }

    fun clearData() {
        viewModelScope.launch {
            repository.clearLocalData()
            _uiState.update {
                it.copy(
                    customers = emptyList(),
                    payments = emptyList(),
                    userMessage = "All local cached records cleared on this device."
                )
            }
        }
    }

    fun runRepaymentPrediction() {
        viewModelScope.launch {
            _uiState.update { it.copy(predictionState = PredictionUiState.Loading) }
            val result = repository.getRepaymentPrediction()
            _uiState.update { current ->
                if (result.isSuccess) {
                    val dto = result.getOrThrow()
                    val newState = when {
                        dto.repaymentProbability != null -> {
                            PredictionUiState.Success(
                                repaymentProbability = dto.repaymentProbability,
                                predictionType = dto.predictionType,
                                predictionTypeDisplay = dto.predictionTypeDisplay,
                                overallRiskTier = dto.overallRiskTier,
                                overallReason = dto.overallReason,
                                confidence = dto.confidence,
                                predictedInstallments = dto.predictedInstallments,
                                totalCustomers = dto.totalCustomers,
                                historicalRows = dto.historicalRows,
                                requiredTrainingRows = dto.requiredTrainingRows,
                                modelVersion = dto.modelVersion,
                                message = dto.message,
                                disclaimer = dto.disclaimer,
                                customerPredictions = dto.customerPredictions
                            )
                        }
                        dto.isNoActiveInstallments -> {
                            PredictionUiState.NoActiveInstallments(
                                message = dto.message.ifBlank {
                                    "There are no outstanding customer balances to predict."
                                }
                            )
                        }
                        dto.error != null -> {
                            PredictionUiState.Error(dto.error)
                        }
                        else -> {
                            PredictionUiState.Error(dto.message.ifBlank { "Unable to calculate repayment prediction." })
                        }
                    }
                    current.copy(predictionState = newState)
                } else {
                    val err = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                        ?: "Failed to connect to repayment prediction AI service at ${com.example.credflow.data.remote.ApiConfig.aiBaseUrl}. Verify FastAPI is running on port ${com.example.credflow.data.remote.ApiConfig.AI_SERVICE_PORT}."
                    current.copy(predictionState = PredictionUiState.Error(err))
                }
            }
            val failedErr = result.exceptionOrNull()
            if (failedErr is SessionExpiredException) {
                handleSessionExpired()
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun loadCollectionForecast(period: String = "all") {
        viewModelScope.launch {
            _uiState.update { it.copy(forecastState = ForecastUiState.Loading) }
            val result = repository.getCollectionForecast(period)
            _uiState.update { current ->
                if (result.isSuccess) {
                    val dto = result.getOrThrow()
                    val currentSelectedPeriod = (current.forecastState as? ForecastUiState.Success)?.selectedPeriod ?: "tomorrow"
                    val newState = when {
                        dto.isNoActiveInstallments -> {
                            ForecastUiState.Empty(
                                message = dto.message ?: "No active loan installments scheduled in your portfolio."
                            )
                        }
                        dto.error != null && !dto.isSuccess -> {
                            ForecastUiState.Error(dto.error)
                        }
                        else -> {
                            ForecastUiState.Success(
                                forecast = dto,
                                selectedPeriod = currentSelectedPeriod
                            )
                        }
                    }
                    current.copy(forecastState = newState)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to connect to forecast service."
                    current.copy(forecastState = ForecastUiState.Error(err))
                }
            }
        }
    }

    fun selectForecastPeriod(period: String) {
        _uiState.update { current ->
            val state = current.forecastState
            if (state is ForecastUiState.Success) {
                current.copy(forecastState = state.copy(selectedPeriod = period))
            } else {
                current
            }
        }
    }

    fun retryForecast() {
        loadCollectionForecast()
    }

    fun loadReports(
        rangePreset: String = "all",
        startDate: String? = null,
        endDate: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(reportsState = ReportsUiState.Loading) }
            val result = repository.getReports(rangePreset, startDate, endDate)
            _uiState.update { current ->
                if (result.isSuccess) {
                    val dto = result.getOrThrow()
                    val existingSuccess = current.reportsState as? ReportsUiState.Success
                    val currentTab = existingSuccess?.selectedChartTab ?: "daily"
                    val newState = ReportsUiState.Success(
                        reports = dto,
                        selectedChartTab = currentTab,
                        selectedPreset = rangePreset,
                        customStartDate = startDate,
                        customEndDate = endDate
                    )
                    current.copy(reportsState = newState)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to connect to reports service."
                    current.copy(reportsState = ReportsUiState.Error(err))
                }
            }
        }
    }

    fun selectReportsChartTab(tab: String) {
        _uiState.update { current ->
            val state = current.reportsState
            if (state is ReportsUiState.Success) {
                current.copy(reportsState = state.copy(selectedChartTab = tab))
            } else {
                current
            }
        }
    }

    fun selectReportsPreset(preset: String, startDate: String? = null, endDate: String? = null) {
        loadReports(rangePreset = preset, startDate = startDate, endDate = endDate)
    }

    fun retryReports() {
        val currentPreset = (_uiState.value.reportsState as? ReportsUiState.Success)?.selectedPreset ?: "all"
        val start = (_uiState.value.reportsState as? ReportsUiState.Success)?.customStartDate
        val end = (_uiState.value.reportsState as? ReportsUiState.Success)?.customEndDate
        loadReports(currentPreset, start, end)
    }

    fun loadNotifications(filter: String = "all") {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsState = NotificationsUiState.Loading) }
            val result = repository.getNotifications(filter)
            _uiState.update { current ->
                if (result.isSuccess) {
                    val dto = result.getOrThrow()
                    current.copy(
                        notificationsState = NotificationsUiState.Success(
                            notifications = dto.notifications,
                            unreadCount = dto.unreadCount,
                            totalCount = dto.totalCount,
                            selectedFilter = filter
                        ),
                        unreadNotificationsCount = dto.unreadCount
                    )
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Failed to load notifications."
                    current.copy(notificationsState = NotificationsUiState.Error(err))
                }
            }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        viewModelScope.launch {
            val result = repository.markNotificationAsRead(notificationId = notificationId)
            if (result.isSuccess) {
                _uiState.update { current ->
                    val state = current.notificationsState
                    if (state is NotificationsUiState.Success) {
                        val updatedList = state.notifications.map {
                            if (it.id == notificationId) it.copy(isRead = true) else it
                        }
                        val newUnread = updatedList.count { !it.isRead }
                        current.copy(
                            notificationsState = state.copy(
                                notifications = updatedList,
                                unreadCount = newUnread
                            ),
                            unreadNotificationsCount = newUnread
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val result = repository.markNotificationAsRead(markAll = true)
            if (result.isSuccess) {
                _uiState.update { current ->
                    val state = current.notificationsState
                    if (state is NotificationsUiState.Success) {
                        val updatedList = state.notifications.map { it.copy(isRead = true) }
                        current.copy(
                            notificationsState = state.copy(
                                notifications = updatedList,
                                unreadCount = 0
                            ),
                            unreadNotificationsCount = 0
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    fun selectNotificationFilter(filter: String) {
        loadNotifications(filter)
    }

    fun retryNotifications() {
        val currentFilter = (_uiState.value.notificationsState as? NotificationsUiState.Success)?.selectedFilter ?: "all"
        loadNotifications(currentFilter)
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        _uiState.update { it.copy(isChangingPassword = true, changePasswordError = null, changePasswordSuccess = null) }
        viewModelScope.launch {
            val result = repository.changePassword(currentPassword, newPassword, confirmPassword)
            if (result.isSuccess) {
                val dto = result.getOrThrow()
                val msg = dto.message.ifBlank { "Password changed successfully." }
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        changePasswordSuccess = msg,
                        changePasswordError = null,
                        userMessage = msg
                    )
                }
                onResult(true, msg)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to change password."
                _uiState.update {
                    it.copy(
                        isChangingPassword = false,
                        changePasswordError = err,
                        changePasswordSuccess = null
                    )
                }
                onResult(false, err)
            }
        }
    }

    fun loadCustomerLedger(customerId: Int) {
        if (customerId <= 0) {
            _uiState.update {
                it.copy(
                    customerLedgerState = CustomerLedgerUiState.Error(
                        message = "Please select a synced customer to view their live MySQL ledger.",
                        customerId = customerId
                    )
                )
            }
            return
        }

        val currentTab = (_uiState.value.customerLedgerState as? CustomerLedgerUiState.Success)?.selectedTab ?: "schedule"
        _uiState.update {
            it.copy(customerLedgerState = CustomerLedgerUiState.Loading(customerId))
        }

        viewModelScope.launch {
            val result = repository.getCustomerLedger(customerId)
            result.onSuccess { ledger ->
                _uiState.update {
                    it.copy(
                        customerLedgerState = CustomerLedgerUiState.Success(
                            ledger = ledger,
                            selectedTab = currentTab
                        )
                    )
                }
            }.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                _uiState.update {
                    it.copy(
                        customerLedgerState = CustomerLedgerUiState.Error(
                            message = err.message ?: "Failed to load customer ledger from MySQL.",
                            customerId = customerId
                        )
                    )
                }
            }
        }
    }

    fun selectLedgerTab(tab: String) {
        _uiState.update { current ->
            val state = current.customerLedgerState
            if (state is CustomerLedgerUiState.Success) {
                current.copy(customerLedgerState = state.copy(selectedTab = tab))
            } else {
                current
            }
        }
    }

    fun prepareCustomerLedgerForExport(
        customerId: Int,
        label: String = "Generating PDF ledger...",
        onReady: (Result<CustomerLedgerDto>) -> Unit
    ) {
        if (customerId <= 0) {
            val err = IllegalArgumentException("Customer must be synced with the server before exporting PDF.")
            _uiState.update { it.copy(exportState = ExportUiState.Error(err.message!!), userMessage = err.message) }
            onReady(Result.failure(err))
            return
        }
        _uiState.update { it.copy(exportState = ExportUiState.Exporting(label)) }
        viewModelScope.launch {
            val result = repository.getCustomerLedger(customerId)
            result.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                val msg = err.message ?: "Failed to fetch customer ledger for PDF export."
                _uiState.update { it.copy(exportState = ExportUiState.Error(msg), userMessage = msg) }
            }
            onReady(result)
        }
    }

    fun prepareReportExportData(
        cycle: String = "all",
        startDate: String? = null,
        endDate: String? = null,
        label: String = "Preparing Excel report...",
        onReady: (Result<ExportReportDataDto>) -> Unit
    ) {
        _uiState.update { it.copy(exportState = ExportUiState.Exporting(label)) }
        viewModelScope.launch {
            val result = repository.getExportData(cycle, startDate, endDate)
            result.onFailure { err ->
                if (err is SessionExpiredException) {
                    handleSessionExpired()
                }
                val msg = err.message ?: "Failed to fetch report data from MySQL."
                _uiState.update { it.copy(exportState = ExportUiState.Error(msg), userMessage = msg) }
            }
            onReady(result)
        }
    }

    fun setExportSuccess(fileName: String, summary: String) {
        _uiState.update {
            it.copy(
                exportState = ExportUiState.Success(fileName = fileName, summary = summary),
                userMessage = summary
            )
        }
    }

    fun setExportError(message: String) {
        _uiState.update {
            it.copy(
                exportState = ExportUiState.Error(message),
                userMessage = message
            )
        }
    }

    fun clearExportState() {
        _uiState.update { it.copy(exportState = ExportUiState.Idle) }
    }

    private suspend fun handleSessionExpired() {
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

    companion object {
        fun provideFactory(
            repository: CredflowRepository,
            sessionManager: UserSessionManager,
            apiService: CredflowApiService,
            appSettings: com.example.credflow.data.local.AppSettingsManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CredflowViewModel(repository, sessionManager, apiService, appSettings) as T
            }
        }
    }
}

