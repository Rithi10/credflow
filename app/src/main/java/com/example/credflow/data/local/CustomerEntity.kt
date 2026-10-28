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
    val cycle: String,
    val day: String,
    val totalDue: Double,
    val paid: Double = 0.0,
    val due: Double,
    val note: String = "",
    val isPendingSync: Boolean = false
) {
    fun toDomainModel(): Customer = Customer(
        id = id,
        name = name,
        phone = phone,
        amount = amount,
        interest = interest,
        cycle = cycle,
        day = day,
        totalDue = totalDue,
        paid = paid,
        due = due,
        note = note
    )

    companion object {
        fun fromDomainModel(customer: Customer, isPending: Boolean = false): CustomerEntity =
            CustomerEntity(
                id = customer.id,
                name = customer.name,
                phone = customer.phone,
                amount = customer.amount,
                interest = customer.interest,
                cycle = customer.cycle,
                day = customer.day,
                totalDue = customer.totalDue,
                paid = customer.paid,
                due = customer.due,
                note = customer.note,
                isPendingSync = isPending
            )
    }
}

