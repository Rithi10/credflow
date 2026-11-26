package com.example.credflow.data.remote.dto

import com.example.credflow.domain.model.Payment
import org.json.JSONObject

data class PaymentDto(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String = "",
    val amount: Double,
    val paymentDate: String,
    val note: String,
    val receiptNumber: String = "",
    val totalLoanAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0
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
        fun fromJson(json: JSONObject): PaymentDto {
            val id = json.optString("id").toIntOrNull() ?: json.optInt("id", 0)
            val customerId = json.optString("customer_id").toIntOrNull() ?: json.optInt("customer_id", 0)
            val customerName = json.optString("customer_name", "Customer #$customerId")
            val customerPhone = json.optString("customer_phone", "")
            val amount = json.optString("amount").toDoubleOrNull() ?: json.optDouble("amount", 0.0)
            val paymentDate = json.optString("payment_date", "")
            val note = json.optString("note", "Cash")
            val receiptNumber = json.optString("receipt_number", "")
            val totalLoanAmount = json.optDouble("total_loan_amount", 0.0)
            val totalPaidAmount = json.optDouble("total_paid_amount", 0.0)
            val remainingBalance = json.optDouble("remaining_balance", 0.0)

            return PaymentDto(
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
        }
    }
}

data class PaymentReceiptDto(
    val id: Int = 0,
    val paymentId: Int = 0,
    val customerId: Int = 0,
    val receiptNumber: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val paymentAmount: Double = 0.0,
    val paymentDate: String = "",
    val paymentMethod: String = "Cash",
    val totalLoanAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val createdAt: String = "",
    val business: BusinessInfoDto = BusinessInfoDto()
) {
    companion object {
        fun fromJson(json: JSONObject?, businessJson: JSONObject? = null): PaymentReceiptDto {
            if (json == null) return PaymentReceiptDto()
            return PaymentReceiptDto(
                id = json.optInt("id", 0),
                paymentId = json.optInt("payment_id", 0),
                customerId = json.optInt("customer_id", 0),
                receiptNumber = json.optString("receipt_number", ""),
                customerName = json.optString("customer_name", ""),
                customerPhone = json.optString("customer_phone", ""),
                paymentAmount = json.optDouble("payment_amount", 0.0),
                paymentDate = json.optString("payment_date", ""),
                paymentMethod = json.optString("payment_method", "Cash"),
                totalLoanAmount = json.optDouble("total_loan_amount", 0.0),
                totalPaidAmount = json.optDouble("total_paid_amount", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                createdAt = json.optString("created_at", ""),
                business = BusinessInfoDto.fromJson(businessJson)
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

data class RecordPaymentResponseDto(
    val paymentId: Int,
    val paidAmount: Double,
    val outstandingBalance: Double,
    val remainingInstallments: Int = 0,
    val totalInstallments: Int = 1,
    val loanStatus: String = "ACTIVE",
    val receipt: PaymentReceiptDto? = null,
    val message: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): RecordPaymentResponseDto {
            val receiptObj = json.optJSONObject("receipt")
            val businessObj = json.optJSONObject("business")
            return RecordPaymentResponseDto(
                paymentId = json.optString("payment_id").toIntOrNull() ?: json.optInt("payment_id", 0),
                paidAmount = json.optString("paid_amount").toDoubleOrNull() ?: json.optDouble("paid_amount", 0.0),
                outstandingBalance = json.optString("outstanding_balance").toDoubleOrNull() ?: json.optDouble("outstanding_balance", 0.0),
                remainingInstallments = json.optString("remaining_installments").toIntOrNull() ?: json.optInt("remaining_installments", 0),
                totalInstallments = json.optString("total_installments").toIntOrNull() ?: json.optInt("total_installments", 1),
                loanStatus = json.optString("loan_status", "ACTIVE"),
                receipt = receiptObj?.let { PaymentReceiptDto.fromJson(it, businessObj) },
                message = json.optString("message", "")
            )
        }
    }
}
