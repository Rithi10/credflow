package com.example.credflow.ui.viewmodel

import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment

data class CredflowUiState(
    val customers: List<Customer> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val pendingOperationsCount: Int = 0,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isOffline: Boolean = false,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val userMessage: String? = null,
    val selectedCustomer: Customer? = null,
    val paidCustomer: Customer? = null,
    val searchQuery: String = "",
    val aiAnswer: String = "",
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
    val appLockEnabled: Boolean = false,
    val lateFeeRule: String = "No Charge",
    val isPredictingAi: Boolean = false,
    val aiPredictionProbability: Double? = null,
    val aiPredictionError: String? = null
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
        get() = if (searchQuery.isBlank()) {
            customers
        } else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
}
