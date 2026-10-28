package com.example.credflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey
    val operationId: String,
    val operationType: String, // "ADD_CUSTOMER" or "RECORD_PAYMENT"
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

