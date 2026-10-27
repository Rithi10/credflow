package com.example.credflow

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String,
    val amount: Double,
    val cycle: String,
    val day: String,
    val due: Double,
    val paid: Double = 0.0,
    val note: String = ""
)
