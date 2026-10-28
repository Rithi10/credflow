package com.example.credflow.data.remote.dto

import com.example.credflow.domain.model.Payment
import org.json.JSONObject

data class PaymentDto(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val amount: Double,
    val paymentDate: String,
    val note: String
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
        fun fromJson(json: JSONObject): PaymentDto {
            val id = json.optString("id").toIntOrNull() ?: json.optInt("id", 0)
            val customerId = json.optString("customer_id").toIntOrNull() ?: json.optInt("customer_id", 0)
            val customerName = json.optString("customer_name", "Customer #$customerId")
            val amount = json.optString("amount").toDoubleOrNull() ?: json.optDouble("amount", 0.0)
            val paymentDate = json.optString("payment_date", "")
            val note = json.optString("note", "Cash")

            return PaymentDto(
                id = id,
                customerId = customerId,
                customerName = customerName,
                amount = amount,
                paymentDate = paymentDate,
                note = note
            )
        }
    }
}

data class RecordPaymentRequestDto(
    val customerId: Int,
    val amount: Double,
    val paymentDate: String,
    val note: String = "Cash",
    val idempotencyKey: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("customer_id", customerId)
        put("amount", amount)
        put("payment_date", paymentDate)
        put("note", note)
        if (idempotencyKey.isNotBlank()) {
            put("idempotency_key", idempotencyKey)
        }
    }
}

