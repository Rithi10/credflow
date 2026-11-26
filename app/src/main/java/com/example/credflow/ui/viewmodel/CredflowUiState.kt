package com.example.credflow.ui.viewmodel

import com.example.credflow.data.remote.dto.CollectionForecastDto
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.CustomerRiskDto
import com.example.credflow.data.remote.dto.NotificationItemDto
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import com.example.credflow.data.remote.dto.ReportsDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment

sealed class PredictionUiState {
    object Idle : PredictionUiState()
    object Loading : PredictionUiState()
    data class Success(
        val repaymentProbability: Double,
        val predictionType: String,
        val predictionTypeDisplay: String,
        val overallRiskTier: String,
        val overallReason: String,
        val confidence: String,
        val predictedInstallments: Int,
        val totalCustomers: Int,
        val historicalRows: Int,
        val requiredTrainingRows: Int,
        val modelVersion: String,
        val message: String,
        val disclaimer: String,
        val customerPredictions: List<CustomerRiskDto> = emptyList()
    ) : PredictionUiState() {
        val isMlModel: Boolean
            get() = predictionType == "ml_model"
    }
    data class NoActiveInstallments(
        val message: String
    ) : PredictionUiState()
    data class Error(
        val message: String
    ) : PredictionUiState()
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    data class Success(
        val answer: String,
        val intent: String = "",
        val source: String = "Credflow Live MySQL Financial Assistant",
        val language: String = "ENGLISH",
        val isOfflineFallback: Boolean = false
    ) : ChatUiState()
    data class Error(
        val message: String,
        val lastQuestion: String = ""
    ) : ChatUiState()
}

sealed class ForecastUiState {
    object Idle : ForecastUiState()
    object Loading : ForecastUiState()
    data class Success(
        val forecast: CollectionForecastDto,
        val selectedPeriod: String = "tomorrow"
    ) : ForecastUiState()
    data class Empty(
        val message: String
    ) : ForecastUiState()
    data class Error(
        val message: String
    ) : ForecastUiState()
}

sealed class ReportsUiState {
    object Idle : ReportsUiState()
    object Loading : ReportsUiState()
    data class Success(
        val reports: ReportsDto,
        val selectedChartTab: String = "daily",
        val selectedPreset: String = "all",
        val customStartDate: String? = null,
        val customEndDate: String? = null
    ) : ReportsUiState()
    data class Error(
        val message: String
    ) : ReportsUiState()
}

sealed class NotificationsUiState {
    object Idle : NotificationsUiState()
    object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<NotificationItemDto>,
        val unreadCount: Int = 0,
        val totalCount: Int = 0,
        val selectedFilter: String = "all"
    ) : NotificationsUiState()
    data class Error(
        val message: String
    ) : NotificationsUiState()
}

sealed class CustomerLedgerUiState {
    object Idle : CustomerLedgerUiState()
    data class Loading(val customerId: Int = 0) : CustomerLedgerUiState()
    data class Success(
        val ledger: CustomerLedgerDto,
        val selectedTab: String = "schedule"
    ) : CustomerLedgerUiState()
    data class Error(
        val message: String,
        val customerId: Int = 0
    ) : CustomerLedgerUiState()
}

sealed class ExportUiState {
    object Idle : ExportUiState()
    data class Exporting(val label: String) : ExportUiState()
    data class Success(
        val fileName: String,
        val summary: String
    ) : ExportUiState()
    data class Error(val message: String) : ExportUiState()
}

data class CredflowUiState(
    val customers: List<Customer> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val pendingOperationsCount: Int = 0,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val refreshError: String? = null,
    val isRefreshingAi: Boolean = false,
    val isSavingCustomer: Boolean = false,
    val isOffline: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isLoggingOut: Boolean = false,
    val authError: String? = null,
    val userMessage: String? = null,
    val selectedCustomer: Customer? = null,
    val paidCustomer: Customer? = null,
    val lastPaymentReceipt: PaymentReceiptDto? = null,
    val isRecordingPayment: Boolean = false,
    val isClosingLoan: Boolean = false,
    val loanClosureError: String? = null,
    val searchQuery: String = "",
    val searchCycleFilter: String = "All",
    val searchStatusFilter: String = "All",
    val searchSortOption: String = "name_asc",
    val searchResults: List<Customer> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val hasRemoteSearchResults: Boolean = false,
    val aiAnswer: String = "",
    val chatState: ChatUiState = ChatUiState.Idle,
    val lastAskedQuestion: String = "",
    val lastCustomerId: Int? = null,
    val businessName: String = "",
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val businessAddress: String = "",
    val gstinNumber: String = "",
    val acceptCash: Boolean = true,
    val acceptUpi: Boolean = true,
    val acceptBank: Boolean = false,
    val dailyReminders: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val overdueAlerts: Boolean = true,
    val defaultCycle: String = "Daily",
    val lateFeeRule: String = "No Charge",
    val predictionState: PredictionUiState = PredictionUiState.Idle,
    val forecastState: ForecastUiState = ForecastUiState.Idle,
    val reportsState: ReportsUiState = ReportsUiState.Idle,
    val notificationsState: NotificationsUiState = NotificationsUiState.Idle,
    val unreadNotificationsCount: Int = 0,
    val customerLedgerState: CustomerLedgerUiState = CustomerLedgerUiState.Idle,
    val exportState: ExportUiState = ExportUiState.Idle,
    val isChangingPassword: Boolean = false,
    val changePasswordError: String? = null,
    val changePasswordSuccess: String? = null
) {
    val totalOutstanding: Double
        get() = customers.sumOf { it.due }

    val totalCollected: Double
        get() = customers.sumOf { it.paid }

    val customersCount: Int
        get() = customers.size

    val paymentEntriesCount: Int
        get() = payments.size

    val filteredCustomers: List<Customer>
        get() = if (hasRemoteSearchResults) {
            searchResults
        } else if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
}

