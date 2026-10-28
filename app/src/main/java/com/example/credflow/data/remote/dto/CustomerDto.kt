package com.example.credflow.data.remote.dto

import com.example.credflow.domain.model.Customer
import org.json.JSONObject

data class CustomerDto(
    val id: Int,
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double,
    val cycle: String,
    val collectionDay: String,
    val totalDue: Double,
    val paidAmount: Double,
    val note: String
) {
    fun toDomainModel(): Customer {
        val calculatedDue = (totalDue - paidAmount).coerceAtLeast(0.0)
        return Customer(
            id = id,
            name = name,
            phone = phone,
            amount = amount,
            interest = interest,
            cycle = cycle,
            day = collectionDay,
            totalDue = totalDue,
            paid = paidAmount,
            due = calculatedDue,
            note = note
        )
    }

    companion object {
        fun fromJson(json: JSONObject): CustomerDto {
            val id = json.optString("id").toIntOrNull() ?: json.optInt("id", 0)
            val name = json.optString("name", "")
            val phone = json.optString("phone", "")
            val amount = json.optString("amount").toDoubleOrNull() ?: json.optDouble("amount", 0.0)
            val interest = json.optString("interest").toDoubleOrNull() ?: json.optDouble("interest", 0.0)
            val cycle = json.optString("cycle", "Daily")
            val day = json.optString("collection_day", if (cycle == "Weekly") "Monday" else cycle)
            val totalDue = json.optString("total_due").toDoubleOrNull() ?: json.optDouble("total_due", amount)
            val paidAmount = json.optString("paid_amount").toDoubleOrNull() ?: json.optDouble("paid_amount", 0.0)
            val note = json.optString("note", "")

            return CustomerDto(
                id = id,
                name = name,
                phone = phone,
                amount = amount,
                interest = interest,
                cycle = cycle,
                collectionDay = day,
                totalDue = totalDue,
                paidAmount = paidAmount,
                note = note
            )
        }
    }
}

data class AddCustomerRequestDto(
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double = 0.0,
    val cycle: String,
    val collectionDay: String,
    val totalDue: Double = amount,
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("phone", phone)
        put("amount", amount)
        put("interest", interest)
        put("cycle", cycle)
        put("collection_day", collectionDay)
        put("total_due", totalDue)
        put("note", note)
    }
}

