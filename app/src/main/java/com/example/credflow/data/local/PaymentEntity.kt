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
    val amount: Double,
    val paymentDate: String,
    val note: String,
    val idempotencyKey: String = "",
    val isPendingSync: Boolean = false
) {
    fun toDomainModel(): Payment = Payment(
        id = id,
        customerId = customerId,
        customerName = customerName,
        amount = amount,
        paymentDate = paymentDate,
        note = note
    )

    companion object {
        fun fromDomainModel(payment: Payment, isPending: Boolean = false, idempotencyKey: String = ""): PaymentEntity =
            PaymentEntity(
                id = payment.id,
                serverId = payment.id,
                customerId = payment.customerId,
                customerName = payment.customerName,
                amount = payment.amount,
                paymentDate = payment.paymentDate,
                note = payment.note,
                idempotencyKey = idempotencyKey,
                isPendingSync = isPending
            )
    }
}
