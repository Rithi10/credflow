package com.example.credflow.data.repository

import com.example.credflow.data.local.CustomerDao
import com.example.credflow.data.local.CustomerEntity
import com.example.credflow.data.local.PaymentDao
import com.example.credflow.data.local.PaymentEntity
import com.example.credflow.data.local.PendingOperationDao
import com.example.credflow.data.local.PendingOperationEntity
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.remote.DuplicateCustomerException
import com.example.credflow.data.remote.SessionExpiredException
import com.example.credflow.data.remote.ValidationException
import com.example.credflow.data.remote.dto.AddCustomerRequestDto
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
import com.example.credflow.data.remote.dto.RecordPaymentRequestDto
import com.example.credflow.data.remote.dto.RepaymentPredictionDto
import com.example.credflow.data.remote.dto.ReportsDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import com.example.credflow.util.PhoneUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CredflowRepositoryImpl(
    private val customerDao: CustomerDao,
    private val paymentDao: PaymentDao,
    private val pendingOperationDao: PendingOperationDao,
    private val apiService: CredflowApiService,
    private val sessionManager: UserSessionManager
) : CredflowRepository {

    override val customers: Flow<List<Customer>> =
        customerDao.getAllCustomers().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override val payments: Flow<List<Payment>> =
        paymentDao.getAllPayments().map { entities ->
            entities.map { it.toDomainModel() }
        }

    override val pendingOperationsCount: Flow<Int> =
        pendingOperationDao.getPendingCount()

    private val syncMutex = Mutex()

    override suspend fun refreshCustomers(): Result<List<Customer>> {
        val tokenAtStart = sessionManager.authToken
        if (tokenAtStart.isNullOrBlank()) {
            return Result.failure(SessionExpiredException("Not authenticated"))
        }
        val remoteResult = apiService.getCustomers(tokenAtStart)
        if (sessionManager.authToken != tokenAtStart || sessionManager.authToken.isNullOrBlank()) {
            return Result.failure(SessionExpiredException("Session changed during sync"))
        }
        return if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val entities = dtoList
                .distinctBy { it.id }
                .map { CustomerEntity.fromDomainModel(it.toDomainModel(), isPending = false) }
            val serverPhones = entities.mapNotNull {
                PhoneUtils.normalizePhone(it.phone).takeIf { p -> p.length >= 6 }
            }.toSet()
            val staleTempCustomers = customerDao.getPendingCustomers()
                .filter { pending ->
                    pending.id < 0 && PhoneUtils.normalizePhone(pending.phone) in serverPhones
                }
            for (stale in staleTempCustomers) {
                customerDao.deleteCustomer(stale.id)
            }
            customerDao.syncCustomers(entities)
            Result.success(entities.map { it.toDomainModel() })
        } else {
            Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load customers from server"))
        }
    }

    override suspend fun refreshPayments(): Result<List<Payment>> {
        val tokenAtStart = sessionManager.authToken
        if (tokenAtStart.isNullOrBlank()) {
            return Result.failure(SessionExpiredException("Not authenticated"))
        }
        val remoteResult = apiService.getPayments(0, tokenAtStart)
        if (sessionManager.authToken != tokenAtStart || sessionManager.authToken.isNullOrBlank()) {
            return Result.failure(SessionExpiredException("Session changed during sync"))
        }
        return if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val entities = dtoList
                .distinctBy { it.id }
                .map { dto ->
                    PaymentEntity(
                        id = dto.id,
                        serverId = dto.id,
                        customerId = dto.customerId,
                        customerName = dto.customerName,
                        customerPhone = dto.customerPhone,
                        amount = dto.amount,
                        paymentDate = dto.paymentDate,
                        note = dto.note,
                        receiptNumber = dto.receiptNumber,
                        totalLoanAmount = dto.totalLoanAmount,
                        totalPaidAmount = dto.totalPaidAmount,
                        remainingBalance = dto.remainingBalance,
                        isPendingSync = false
                    )
                }
            paymentDao.syncPayments(entities)
            Result.success(entities.map { it.toDomainModel() })
        } else {
            Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load payments from server"))
        }
    }

    override suspend fun addCustomer(customer: Customer, idempotencyKey: String): Result<Customer> = syncMutex.withLock {
        val existingMatch = customerDao.getAllCustomersList().firstOrNull { existing ->
            PhoneUtils.phonesMatch(existing.phone, customer.phone)
        }
        if (existingMatch != null) {
            return@withLock Result.failure(
                DuplicateCustomerException(
                    "Customer '${existingMatch.name}' with phone number ${customer.phone} already exists.",
                    existingMatch.id
                )
            )
        }

        val requestDto = AddCustomerRequestDto(
            name = customer.name,
            phone = customer.phone,
            amount = customer.amount,
            interest = customer.interest,
            interestType = customer.interestType,
            period = customer.period,
            periodUnit = customer.periodUnit,
            cycle = customer.cycle,
            collectionDay = customer.day,
            totalDue = customer.totalDue,
            installmentAmount = customer.installmentAmount,
            totalInstallments = customer.totalInstallments,
            firstDueDate = customer.firstDueDate,
            note = customer.note,
            idempotencyKey = idempotencyKey
        )

        val remoteResult = apiService.addCustomer(requestDto, sessionManager.authToken)
        return@withLock if (remoteResult.isSuccess) {
            val serverId = remoteResult.getOrThrow()
            val savedCustomer = customer.copy(id = serverId)
            customerDao.insertCustomer(CustomerEntity.fromDomainModel(savedCustomer, isPending = false))
            Result.success(savedCustomer)
        } else {
            val error = remoteResult.exceptionOrNull()
            if (error is DuplicateCustomerException || error is ValidationException || error is SessionExpiredException) {
                Result.failure(error)
            } else {
                // Offline queue fallback only on network/server outage
                val tempId = -kotlin.math.abs((System.currentTimeMillis() % 1_000_000L).toInt()).coerceAtLeast(1)
                val tempCustomer = customer.copy(id = tempId)
                customerDao.insertCustomer(CustomerEntity.fromDomainModel(tempCustomer, isPending = true))

                val payload = requestDto.toJson().apply {
                    put("temp_customer_id", tempId)
                }
                val op = PendingOperationEntity(
                    operationId = idempotencyKey,
                    operationType = "ADD_CUSTOMER",
                    payloadJson = payload.toString()
                )
                pendingOperationDao.insertOperation(op)

                Result.success(tempCustomer)
            }
        }
    }

    override suspend fun recordPayment(
        customerId: Int,
        customerName: String,
        amount: Double,
        method: String
    ): Result<Payment> {
        return recordPaymentWithReceipt(
            customerId = customerId,
            customerName = customerName,
            amount = amount,
            method = method,
            idempotencyKey = UUID.randomUUID().toString()
        ).map { it.first }
    }

    override suspend fun recordPaymentWithReceipt(
        customerId: Int,
        customerName: String,
        amount: Double,
        method: String,
        idempotencyKey: String
    ): Result<Pair<Payment, PaymentReceiptDto?>> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val requestDto = RecordPaymentRequestDto(
            customerId = customerId,
            amount = amount,
            paymentDate = today,
            note = method,
            idempotencyKey = idempotencyKey
        )

        val remoteResult = if (customerId > 0) {
            apiService.recordPayment(requestDto, sessionManager.authToken)
        } else {
            Result.failure(Exception("Customer ID is pending sync"))
        }

        if (remoteResult.isFailure) {
            val err = remoteResult.exceptionOrNull()
            // Never queue offline fallback if server explicitly rejected validation/closed loan or expired auth
            if (err is com.example.credflow.data.remote.SessionExpiredException) {
                return Result.failure(err)
            }
            val msg = err?.message ?: ""
            if (msg.contains("CLOSED", ignoreCase = true) || msg.contains("exceeds", ignoreCase = true)) {
                return Result.failure(err!!)
            }
        }

        val isOnlineSuccess = remoteResult.isSuccess
        val serverResponse = remoteResult.getOrNull()
        val serverReceipt = serverResponse?.receipt
        val assignedId = if (isOnlineSuccess && (serverResponse?.paymentId ?: 0) > 0) {
            serverResponse!!.paymentId
        } else {
            -kotlin.math.abs((System.currentTimeMillis() % 1000000).toInt())
        }

        val existingCustomer = customerDao.getCustomerById(customerId)
        val paymentEntity = PaymentEntity(
            id = assignedId,
            serverId = if (isOnlineSuccess) assignedId else 0,
            customerId = customerId,
            customerName = customerName,
            customerPhone = serverReceipt?.customerPhone ?: (existingCustomer?.phone ?: ""),
            amount = amount,
            paymentDate = serverReceipt?.paymentDate?.ifBlank { today } ?: today,
            note = method,
            receiptNumber = serverReceipt?.receiptNumber ?: "",
            totalLoanAmount = serverReceipt?.totalLoanAmount ?: (existingCustomer?.totalDue ?: 0.0),
            totalPaidAmount = serverReceipt?.totalPaidAmount ?: (serverResponse?.paidAmount ?: 0.0),
            remainingBalance = serverReceipt?.remainingBalance ?: (serverResponse?.outstandingBalance ?: 0.0),
            idempotencyKey = idempotencyKey,
            isPendingSync = !isOnlineSuccess
        )
        paymentDao.insertPayment(paymentEntity)

        if (existingCustomer != null) {
            val updatedPaid = if (serverResponse != null && serverResponse.paidAmount > 0) {
                serverResponse.paidAmount
            } else {
                existingCustomer.paid + amount
            }
            val updatedDue = if (serverResponse != null) {
                serverResponse.outstandingBalance
            } else {
                (existingCustomer.due - amount).coerceAtLeast(0.0)
            }
            val updatedLoanStatus = serverResponse?.loanStatus ?: existingCustomer.loanStatus
            customerDao.insertCustomer(
                existingCustomer.copy(
                    paid = updatedPaid,
                    due = updatedDue,
                    loanStatus = updatedLoanStatus
                )
            )
        }

        if (!isOnlineSuccess) {
            val op = PendingOperationEntity(
                operationId = idempotencyKey,
                operationType = "RECORD_PAYMENT",
                payloadJson = requestDto.toJson().toString()
            )
            pendingOperationDao.insertOperation(op)
        }

        return Result.success(Pair(paymentEntity.toDomainModel(), serverReceipt))
    }

    override suspend fun syncPendingOperations(): Result<Int> = syncMutex.withLock {
        val pendingList = pendingOperationDao.getAllPending()
        var syncedCount = 0

        for (op in pendingList) {
            try {
                when (op.operationType) {
                    "ADD_CUSTOMER" -> {
                        val json = JSONObject(op.payloadJson)
                        val opIdempotencyKey = json.optString("idempotency_key").ifBlank { op.operationId }
                        val tempCustomerId = json.optInt("temp_customer_id", 0)
                        val dto = AddCustomerRequestDto(
                            name = json.optString("name"),
                            phone = json.optString("phone"),
                            amount = json.optDouble("amount"),
                            interest = json.optDouble("interest"),
                            interestType = json.optString("interest_type", "Flat Interest"),
                            period = json.optInt("period", 1),
                            periodUnit = json.optString("period_unit", "Months"),
                            cycle = json.optString("cycle"),
                            collectionDay = json.optString("collection_day"),
                            totalDue = json.optDouble("total_due"),
                            installmentAmount = json.optDouble("installment_amount"),
                            totalInstallments = json.optInt("total_installments", 1),
                            firstDueDate = json.optString("first_due_date"),
                            note = json.optString("note"),
                            idempotencyKey = opIdempotencyKey
                        )
                        val res = apiService.addCustomer(dto, sessionManager.authToken)
                        if (res.isSuccess || res.exceptionOrNull() is DuplicateCustomerException || res.exceptionOrNull() is ValidationException) {
                            if (tempCustomerId < 0) {
                                customerDao.deleteCustomer(tempCustomerId)
                            } else {
                                customerDao.getPendingCustomers()
                                    .filter { it.id < 0 && PhoneUtils.phonesMatch(it.phone, dto.phone) }
                                    .forEach { customerDao.deleteCustomer(it.id) }
                            }
                            pendingOperationDao.deleteOperation(op.operationId)
                            if (res.isSuccess) {
                                syncedCount++
                            }
                        }
                    }
                    "RECORD_PAYMENT" -> {
                        val json = JSONObject(op.payloadJson)
                        val dto = RecordPaymentRequestDto(
                            customerId = json.optInt("customer_id"),
                            amount = json.optDouble("amount"),
                            paymentDate = json.optString("payment_date"),
                            note = json.optString("note"),
                            idempotencyKey = op.operationId
                        )
                        val res = apiService.recordPayment(dto, sessionManager.authToken)
                        if (res.isSuccess) {
                            pendingOperationDao.deleteOperation(op.operationId)
                            syncedCount++
                        }
                    }
                }
            } catch (_: Exception) {
                // Continue with other operations
            }
        }

        return@withLock Result.success(syncedCount)
    }

    override suspend fun clearLocalData() {
        customerDao.clearAll()
        paymentDao.clearAll()
        pendingOperationDao.clearAll()
    }

    override suspend fun testConnection(): Result<String> {
        return apiService.testConnection()
    }

    override suspend fun getRepaymentPrediction(): Result<RepaymentPredictionDto> {
        return apiService.predictRepayment(sessionManager.authToken)
    }

    override suspend fun askChatbot(question: String, lastCustomerId: Int?): Result<ChatResponseDto> {
        return apiService.askChatbot(question, sessionManager.authToken, lastCustomerId)
    }

    override suspend fun getCollectionForecast(period: String): Result<CollectionForecastDto> {
        return apiService.getCollectionForecast(period, sessionManager.authToken)
    }

    override suspend fun getReports(
        rangePreset: String,
        startDate: String?,
        endDate: String?
    ): Result<ReportsDto> {
        return apiService.getReports(rangePreset, startDate, endDate, sessionManager.authToken)
    }

    override suspend fun getNotifications(filter: String): Result<NotificationsResponseDto> {
        return apiService.getNotifications(filter, authToken = sessionManager.authToken)
    }

    override suspend fun markNotificationAsRead(notificationId: Int?, markAll: Boolean): Result<Int> {
        return apiService.markNotificationAsRead(notificationId, markAll, sessionManager.authToken)
    }

    override suspend fun updateProfile(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        businessAddress: String,
        gstinNumber: String
    ): Result<AuthResponseDto> {
        val res = apiService.updateProfile(
            businessName = businessName,
            ownerName = ownerName,
            ownerPhone = ownerPhone,
            ownerEmail = ownerEmail,
            businessAddress = businessAddress,
            gstinNumber = gstinNumber,
            authToken = sessionManager.authToken
        )
        res.onSuccess { auth ->
            sessionManager.saveProfile(
                name = auth.name,
                bName = auth.businessName,
                phone = auth.phone,
                email = auth.email,
                address = auth.businessAddress,
                gstin = auth.gstinNumber
            )
        }
        return res
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<ChangePasswordResponseDto> {
        val res = apiService.changePassword(currentPassword, newPassword, confirmPassword, sessionManager.authToken)
        res.getOrNull()?.newAuthToken?.let { newToken ->
            if (newToken.isNotBlank()) {
                sessionManager.authToken = newToken
            }
        }
        return res
    }

    override suspend fun getCustomerLedger(customerId: Int): Result<CustomerLedgerDto> {
        return apiService.getCustomerLedger(customerId, sessionManager.authToken)
    }

    override suspend fun getExportData(
        cycle: String,
        startDate: String?,
        endDate: String?
    ): Result<ExportReportDataDto> {
        return apiService.getExportData(cycle, startDate, endDate, sessionManager.authToken)
    }

    override suspend fun getPaymentReceipt(paymentId: Int): Result<PaymentReceiptDto> {
        return apiService.getPaymentReceipt(paymentId, sessionManager.authToken)
    }

    override suspend fun closeLoan(request: CloseLoanRequestDto): Result<CloseLoanResponseDto> {
        val result = apiService.closeLoan(request, sessionManager.authToken)
        result.onSuccess { resp ->
            val existing = customerDao.getCustomerById(request.customerId)
            if (existing != null) {
                customerDao.insertCustomer(
                    existing.copy(
                        paid = resp.paidAmount,
                        due = resp.remainingBalance,
                        loanStatus = resp.loanStatus,
                        closedAt = resp.closedAt,
                        settlementAmount = resp.settlementAmount,
                        settlementType = resp.settlementType,
                        closureNote = request.note
                    )
                )
            }
            val receipt = resp.receipt
            if (resp.paymentId > 0 && receipt != null) {
                paymentDao.insertPayment(
                    PaymentEntity(
                        id = resp.paymentId,
                        serverId = resp.paymentId,
                        customerId = request.customerId,
                        customerName = receipt.customerName,
                        customerPhone = receipt.customerPhone,
                        amount = receipt.paymentAmount,
                        paymentDate = receipt.paymentDate,
                        note = receipt.paymentMethod,
                        receiptNumber = receipt.receiptNumber,
                        totalLoanAmount = receipt.totalLoanAmount,
                        totalPaidAmount = receipt.totalPaidAmount,
                        remainingBalance = receipt.remainingBalance,
                        idempotencyKey = request.idempotencyKey,
                        isPendingSync = false
                    )
                )
            }
        }
        return result
    }

    override suspend fun searchCustomers(
        query: String,
        cycle: String,
        status: String,
        sort: String
    ): Result<List<Customer>> {
        return apiService.searchCustomers(query, cycle, status, sort, sessionManager.authToken)
            .map { resp -> resp.customers.map { it.toDomainModel() } }
    }
}

