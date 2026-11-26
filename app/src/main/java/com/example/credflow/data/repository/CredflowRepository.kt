package com.example.credflow.data.repository

import com.example.credflow.data.remote.dto.AuthResponseDto
import com.example.credflow.data.remote.dto.ChatResponseDto
import com.example.credflow.data.remote.dto.CollectionForecastDto
import com.example.credflow.data.remote.dto.NotificationsResponseDto
import com.example.credflow.data.remote.dto.ChangePasswordResponseDto
import com.example.credflow.data.remote.dto.CloseLoanRequestDto
import com.example.credflow.data.remote.dto.CloseLoanResponseDto
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.ExportReportDataDto
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import com.example.credflow.data.remote.dto.RepaymentPredictionDto
import com.example.credflow.data.remote.dto.ReportsDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface CredflowRepository {
    val customers: Flow<List<Customer>>
    val payments: Flow<List<Payment>>
    val pendingOperationsCount: Flow<Int>

    suspend fun refreshCustomers(): Result<List<Customer>>
    suspend fun refreshPayments(): Result<List<Payment>>
    suspend fun addCustomer(customer: Customer, idempotencyKey: String = UUID.randomUUID().toString()): Result<Customer>
    suspend fun recordPayment(customerId: Int, customerName: String, amount: Double, method: String): Result<Payment>
    suspend fun syncPendingOperations(): Result<Int>
    suspend fun clearLocalData()
    suspend fun testConnection(): Result<String>
    suspend fun getRepaymentPrediction(): Result<RepaymentPredictionDto>
    suspend fun askChatbot(question: String, lastCustomerId: Int? = null): Result<ChatResponseDto>
    suspend fun getCollectionForecast(period: String = "all"): Result<CollectionForecastDto>
    suspend fun getReports(rangePreset: String = "all", startDate: String? = null, endDate: String? = null): Result<ReportsDto>
    suspend fun getNotifications(filter: String = "all"): Result<NotificationsResponseDto>
    suspend fun markNotificationAsRead(notificationId: Int? = null, markAll: Boolean = false): Result<Int>
    suspend fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String): Result<ChangePasswordResponseDto>
    suspend fun updateProfile(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        businessAddress: String,
        gstinNumber: String
    ): Result<AuthResponseDto>
    suspend fun getCustomerLedger(customerId: Int): Result<CustomerLedgerDto>
    suspend fun getExportData(cycle: String = "all", startDate: String? = null, endDate: String? = null): Result<ExportReportDataDto>
    suspend fun getPaymentReceipt(paymentId: Int): Result<PaymentReceiptDto>
    suspend fun recordPaymentWithReceipt(customerId: Int, customerName: String, amount: Double, method: String, idempotencyKey: String): Result<Pair<Payment, PaymentReceiptDto?>>
    suspend fun closeLoan(request: CloseLoanRequestDto): Result<CloseLoanResponseDto>
    suspend fun searchCustomers(query: String = "", cycle: String = "all", status: String = "all", sort: String = "name_asc"): Result<List<Customer>>
}

