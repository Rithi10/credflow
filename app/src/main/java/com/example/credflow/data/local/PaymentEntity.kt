package com.example.credflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.credflow.domain.model.Payment

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: Int,
    val serverId: Int = id,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String = "",
    val amount: Double,
    val paymentDate: String,
    val note: String,
    val receiptNumber: String = "",
    val totalLoanAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val idempotencyKey: String = "",
    val isPendingSync: Boolean = false
) {
    fun toDomainModel(): Payment = Payment(
        id = id,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        amount = amount,
        paymentDate = paymentDate,
        note = note,
        receiptNumber = receiptNumber,
        totalLoanAmount = totalLoanAmount,
        totalPaidAmount = totalPaidAmount,
        remainingBalance = remainingBalance
    )

    companion object {
        fun fromDomainModel(payment: Payment, isPending: Boolean = false, idempotencyKey: String = ""): PaymentEntity =
            PaymentEntity(
                id = payment.id,
                serverId = payment.id,
                customerId = payment.customerId,
                customerName = payment.customerName,
                customerPhone = payment.customerPhone,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                note = payment.note,
                receiptNumber = payment.receiptNumber,
                totalLoanAmount = payment.totalLoanAmount,
                totalPaidAmount = payment.totalPaidAmount,
                remainingBalance = payment.remainingBalance,
                idempotencyKey = idempotencyKey,
                isPendingSync = isPending
            )
    }
}
