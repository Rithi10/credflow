package com.example.credflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.credflow.domain.model.Customer

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double = 0.0,
    val interestType: String = "Flat Interest",
    val period: Int = 1,
    val periodUnit: String = "Months",
    val installmentAmount: Double = 0.0,
    val totalInstallments: Int = 1,
    val firstDueDate: String = "",
    val finalDueDate: String = "",
    val cycle: String,
    val day: String,
    val totalDue: Double,
    val paid: Double = 0.0,
    val due: Double,
    val note: String = "",
    val createdAt: String = "",
    val loanStatus: String = "ACTIVE",
    val closedAt: String = "",
    val settlementAmount: Double = 0.0,
    val settlementType: String = "",
    val closureNote: String = "",
    val isPendingSync: Boolean = false
) {
    fun toDomainModel(): Customer = Customer(
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
        day = day,
        totalDue = totalDue,
        paid = paid,
        due = due,
        note = note,
        createdAt = createdAt,
        loanStatus = loanStatus,
        closedAt = closedAt,
        settlementAmount = settlementAmount,
        settlementType = settlementType,
        closureNote = closureNote
    )

    companion object {
        fun fromDomainModel(customer: Customer, isPending: Boolean = false): CustomerEntity =
            CustomerEntity(
                id = customer.id,
                name = customer.name,
                phone = customer.phone,
                amount = customer.amount,
                interest = customer.interest,
                interestType = customer.interestType,
                period = customer.period,
                periodUnit = customer.periodUnit,
                installmentAmount = customer.installmentAmount,
                totalInstallments = customer.totalInstallments,
                firstDueDate = customer.firstDueDate,
                finalDueDate = customer.finalDueDate,
                cycle = customer.cycle,
                day = customer.day,
                totalDue = customer.totalDue,
                paid = customer.paid,
                due = customer.due,
                note = customer.note,
                createdAt = customer.createdAt,
                loanStatus = customer.loanStatus,
                closedAt = customer.closedAt,
                settlementAmount = customer.settlementAmount,
                settlementType = customer.settlementType,
                closureNote = customer.closureNote,
                isPendingSync = isPending
            )
    }
}
