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
    val businessName: String = "Credflow Finance",
    val ownerName: String = "Ramesh Kumar",
    val ownerPhone: String = "9876543210",
    val ownerEmail: String = "ramesh@credflow.com",
    val businessAddress: String = "12 Brigade Road, Bengaluru",
    val gstinNumber: String = "29ABCDE1234F1Z5",
    val acceptCash: Boolean = true,
    val acceptUpi: Boolean = true,
    val acceptBank: Boolean = false,
    val dailyReminders: Boolean = true,
    val overdueAlerts: Boolean = true,
    val defaultCycle: String = "Daily",
    val appLockEnabled: Boolean = false,
    val lateFeeRule: String = "No Charge"
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

