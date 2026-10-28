package com.example.credflow.data.repository

import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface CredflowRepository {
    val customers: Flow<List<Customer>>
    val payments: Flow<List<Payment>>
    val pendingOperationsCount: Flow<Int>

    suspend fun refreshCustomers(): Result<List<Customer>>
    suspend fun refreshPayments(): Result<List<Payment>>
    suspend fun addCustomer(customer: Customer): Result<Customer>
    suspend fun recordPayment(customerId: Int, customerName: String, amount: Double, method: String): Result<Payment>
    suspend fun syncPendingOperations(): Result<Int>
    suspend fun clearLocalData()
    suspend fun testConnection(): Result<String>
}

