package com.example.credflow.domain.model

data class Customer(
    val id: Int = 0,
    val name: String,
    val phone: String,
    val amount: Double,
    val interest: Double = 0.0,
    val cycle: String,
    val day: String,
    val totalDue: Double = amount,
    val paid: Double = 0.0,
    val due: Double = (totalDue - paid).coerceAtLeast(0.0),
    val note: String = ""
)

