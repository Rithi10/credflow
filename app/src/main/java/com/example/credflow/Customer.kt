package com.example.credflow

data class Customer(
    val name: String,
    val phone: String,
    val amount: Double,
    val cycle: String,
    val day: String,
    val due: Double = amount,
    val paid: Double = 0.0,
    val note: String = ""
)
