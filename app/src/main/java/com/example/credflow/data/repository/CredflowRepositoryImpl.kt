package com.example.credflow.data.repository

import com.example.credflow.data.local.CustomerDao
import com.example.credflow.data.local.CustomerEntity
import com.example.credflow.data.local.PaymentDao
import com.example.credflow.data.local.PaymentEntity
import com.example.credflow.data.local.PendingOperationDao
import com.example.credflow.data.local.PendingOperationEntity
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.remote.dto.AddCustomerRequestDto
import com.example.credflow.data.remote.dto.RecordPaymentRequestDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun refreshCustomers(): Result<List<Customer>> {
        val remoteResult = apiService.getCustomers(sessionManager.authToken)
        return if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val entities = dtoList.map { CustomerEntity.fromDomainModel(it.toDomainModel(), isPending = false) }
            customerDao.insertAll(entities)
            Result.success(entities.map { it.toDomainModel() })
        } else {
            val cached = customerDao.getAllCustomersList().map { it.toDomainModel() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load customers from server"))
            }
        }
    }

    override suspend fun refreshPayments(): Result<List<Payment>> {
        val remoteResult = apiService.getPayments(0, sessionManager.authToken)
        return if (remoteResult.isSuccess) {
            val dtoList = remoteResult.getOrThrow()
            val entities = dtoList.map { dto ->
                PaymentEntity(
                    serverId = dto.id,
                    customerId = dto.customerId,
                    customerName = dto.customerName,
                    amount = dto.amount,
                    paymentDate = dto.paymentDate,
                    note = dto.note,
                    isPendingSync = false
                )
            }
            paymentDao.insertAll(entities)
            Result.success(entities.map { it.toDomainModel() })
        } else {
            Result.failure(remoteResult.exceptionOrNull() ?: Exception("Failed to load payments from server"))
        }
    }

    override suspend fun addCustomer(customer: Customer): Result<Customer> {
        val requestDto = AddCustomerRequestDto(
            name = customer.name,
            phone = customer.phone,
            amount = customer.amount,
            interest = customer.interest,
            cycle = customer.cycle,
            collectionDay = customer.day,
            totalDue = customer.totalDue,
            note = customer.note
        )

        val remoteResult = apiService.addCustomer(requestDto, sessionManager.authToken)
        return if (remoteResult.isSuccess) {
            val serverId = remoteResult.getOrThrow()
            val savedCustomer = customer.copy(id = serverId)
            customerDao.insertCustomer(CustomerEntity.fromDomainModel(savedCustomer, isPending = false))
            Result.success(savedCustomer)
        } else {
            // Offline queue fallback
            val tempId = -kotlin.math.abs((System.currentTimeMillis() % 1000000).toInt())
            val tempCustomer = customer.copy(id = tempId)
            customerDao.insertCustomer(CustomerEntity.fromDomainModel(tempCustomer, isPending = true))

            val op = PendingOperationEntity(
                operationId = UUID.randomUUID().toString(),
                operationType = "ADD_CUSTOMER",
                payloadJson = requestDto.toJson().toString()
            )
            pendingOperationDao.insertOperation(op)

            Result.success(tempCustomer)
        }
    }

    override suspend fun recordPayment(
        customerId: Int,
        customerName: String,
        amount: Double,
        method: String
    ): Result<Payment> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val idempotencyKey = UUID.randomUUID().toString()

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

        val isOnlineSuccess = remoteResult.isSuccess
        val paymentEntity = PaymentEntity(
            customerId = customerId,
            customerName = customerName,
            amount = amount,
            paymentDate = today,
            note = method,
            idempotencyKey = idempotencyKey,
            isPendingSync = !isOnlineSuccess
        )
        paymentDao.insertPayment(paymentEntity)

        // Update local customer entity balance
        val existingCustomer = customerDao.getCustomerById(customerId)
        if (existingCustomer != null) {
            val updatedPaid = existingCustomer.paid + amount
            val updatedDue = (existingCustomer.due - amount).coerceAtLeast(0.0)
            customerDao.insertCustomer(
                existingCustomer.copy(
                    paid = updatedPaid,
                    due = updatedDue
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

        return Result.success(paymentEntity.toDomainModel())
    }

    override suspend fun syncPendingOperations(): Result<Int> {
        val pendingList = pendingOperationDao.getAllPending()
        var syncedCount = 0

        for (op in pendingList) {
            try {
                when (op.operationType) {
                    "ADD_CUSTOMER" -> {
                        val json = JSONObject(op.payloadJson)
                        val dto = AddCustomerRequestDto(
                            name = json.optString("name"),
                            phone = json.optString("phone"),
                            amount = json.optDouble("amount"),
                            interest = json.optDouble("interest"),
                            cycle = json.optString("cycle"),
                            collectionDay = json.optString("collection_day"),
                            totalDue = json.optDouble("total_due"),
                            note = json.optString("note")
                        )
                        val res = apiService.addCustomer(dto, sessionManager.authToken)
                        if (res.isSuccess) {
                            val newId = res.getOrThrow()
                            pendingOperationDao.deleteOperation(op.operationId)
                            syncedCount++
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
            } catch (e: Exception) {
                // Continue with other operations
            }
        }

        return Result.success(syncedCount)
    }

    override suspend fun clearLocalData() {
        customerDao.clearAll()
        paymentDao.clearAll()
        pendingOperationDao.clearAll()
    }

    override suspend fun testConnection(): Result<String> {
        return apiService.testConnection()
    }
}

