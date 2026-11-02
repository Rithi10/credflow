package com.example.credflow.data.remote.dto

import com.example.credflow.domain.model.Customer
import org.json.JSONObject

data class CustomerDto(
    val id: Int,
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double,
    val interestType: String = "Flat Interest",
    val period: Int = 1,
    val periodUnit: String = "Months",
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 1,
    val firstDueDate: String = "",
    val finalDueDate: String = "",
    val cycle: String,
    val collectionDay: String,
    val totalDue: Double,
    val paidAmount: Double,
    val note: String,
    val createdAt: String = ""
) {
    fun toDomainModel(): Customer {
        val calculatedDue = (totalDue - paidAmount).coerceAtLeast(0.0)
        return Customer(
            id = id,
            name = name,
            phone = phone,
            amount = amount,
            interest = interest,
            interestType = interestType,
            period = period,
            periodUnit = periodUnit,
            installmentAmount = installmentAmount,
            totalInstallments = totalInstallments,
            firstDueDate = firstDueDate,
            finalDueDate = finalDueDate,
            cycle = cycle,
            day = collectionDay,
            totalDue = totalDue,
            paid = paidAmount,
            due = calculatedDue,
            note = note,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromJson(json: JSONObject): CustomerDto {
            val id = json.optString("id").toIntOrNull() ?: json.optInt("id", 0)
            val name = json.optString("name", "")
            val phone = json.optString("phone", "")
            val amount = json.optString("amount").toDoubleOrNull() ?: json.optDouble("amount", 0.0)
            val interest = json.optString("interest").toDoubleOrNull() ?: json.optDouble("interest", 0.0)
            val interestType = json.optString("interest_type", "Flat Interest")
            val period = json.optString("period").toIntOrNull() ?: json.optInt("period", 1)
            val periodUnit = json.optString("period_unit", "Months")
            val cycle = json.optString("cycle", "Daily")
            val day = json.optString("collection_day", if (cycle == "Weekly") "Monday" else cycle)
            val totalDue = json.optString("total_due").toDoubleOrNull() ?: json.optDouble("total_due", amount)
            val paidAmount = json.optString("paid_amount").toDoubleOrNull() ?: json.optDouble("paid_amount", 0.0)
            val totalInstallments = json.optString("total_installments").toIntOrNull() ?: json.optInt("total_installments", 1)
            val installmentAmount = json.optString("installment_amount").toDoubleOrNull()
                ?: json.optDouble("installment_amount", if (totalInstallments > 0) totalDue / totalInstallments else 0.0)
            val firstDueDate = json.optString("first_due_date", "")
            val finalDueDate = json.optString("final_due_date", "")
            val note = json.optString("note", "")
            val createdAt = json.optString("created_at", "")

            return CustomerDto(
                id = id,
                name = name,
                phone = phone,
                amount = amount,
                interest = interest,
                interestType = interestType,
                period = period,
                periodUnit = periodUnit,
                installmentAmount = installmentAmount,
                totalInstallments = totalInstallments,
                firstDueDate = firstDueDate,
                finalDueDate = finalDueDate,
                cycle = cycle,
                collectionDay = day,
                totalDue = totalDue,
                paidAmount = paidAmount,
                note = note,
                createdAt = createdAt
            )
        }
    }
}

data class AddCustomerRequestDto(
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double = 0.0,
    val interestType: String = "Flat Interest",
    val period: Int = 1,
    val periodUnit: String = "Months",
    val cycle: String,
    val collectionDay: String,
    val totalDue: Double = amount,
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 1,
    val firstDueDate: String = "",
    val note: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("phone", phone)
        put("amount", amount)
        put("interest", interest)
        put("interest_type", interestType)
        put("period", period)
        put("period_unit", periodUnit)
        put("cycle", cycle)
        put("collection_day", collectionDay)
        put("total_due", totalDue)
        put("installment_amount", installmentAmount)
        put("total_installments", totalInstallments)
        put("first_due_date", firstDueDate)
        put("note", note)
    }
}
