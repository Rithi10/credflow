package com.example.credflow.data.remote.dto

import org.json.JSONObject

data class CloseLoanRequestDto(
    val customerId: Int,
    val settlementType: String = "FULL",
    val settlementAmount: Double,
    val paymentMethod: String = "Cash",
    val paymentDate: String,
    val note: String = "",
    val closeLoan: Boolean = true,
    val idempotencyKey: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("customer_id", customerId)
        put("settlement_type", settlementType)
        put("settlement_amount", settlementAmount)
        put("payment_method", paymentMethod)
        put("payment_date", paymentDate)
        put("note", note)
        put("close_loan", closeLoan)
        if (idempotencyKey.isNotBlank()) {
            put("idempotency_key", idempotencyKey)
        }
    }
}

data class CloseLoanResponseDto(
    val success: Boolean = false,
    val customerId: Int = 0,
    val loanStatus: String = "ACTIVE",
    val loanClosed: Boolean = false,
    val closedAt: String = "",
    val settlementType: String = "",
    val settlementAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentId: Int = 0,
    val receipt: PaymentReceiptDto? = null,
    val business: BusinessInfoDto = BusinessInfoDto(),
    val message: String = "",
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): CloseLoanResponseDto {
            val error = json.optString("error").takeIf { it.isNotBlank() }
            val businessObj = json.optJSONObject("business")
            val receiptObj = json.optJSONObject("receipt")
            return CloseLoanResponseDto(
                success = json.optBoolean("success", error == null),
                customerId = json.optInt("customer_id", 0),
                loanStatus = json.optString("loan_status", "ACTIVE"),
                loanClosed = json.optBoolean("loan_closed", false),
                closedAt = json.optString("closed_at", "").takeIf { it != "null" } ?: "",
                settlementType = json.optString("settlement_type", ""),
                settlementAmount = json.optDouble("settlement_amount", 0.0),
                paidAmount = json.optDouble("paid_amount", 0.0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                paymentId = json.optInt("payment_id", 0),
                receipt = receiptObj?.let { PaymentReceiptDto.fromJson(it, businessObj) },
                business = BusinessInfoDto.fromJson(businessObj),
                message = json.optString("message", ""),
                error = error
            )
        }
    }
}

data class PaymentReceiptResponseDto(
    val success: Boolean = false,
    val receipt: PaymentReceiptDto = PaymentReceiptDto(),
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): PaymentReceiptResponseDto {
            val error = json.optString("error").takeIf { it.isNotBlank() }
            val businessObj = json.optJSONObject("business")
            val receiptObj = json.optJSONObject("receipt")
            return PaymentReceiptResponseDto(
                success = json.optBoolean("success", error == null),
                receipt = PaymentReceiptDto.fromJson(receiptObj, businessObj),
                error = error
            )
        }
    }
}

data class SearchCustomersResponseDto(
    val success: Boolean = false,
    val totalCount: Int = 0,
    val customers: List<CustomerDto> = emptyList(),
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): SearchCustomersResponseDto {
            val error = json.optString("error").takeIf { it.isNotBlank() }
            val arr = json.optJSONArray("customers")
            val list = buildList {
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { add(CustomerDto.fromJson(it)) }
                    }
                }
            }
            return SearchCustomersResponseDto(
                success = json.optBoolean("success", error == null),
                totalCount = json.optInt("total_count", list.size),
                customers = list,
                error = error
            )
        }
    }
}

