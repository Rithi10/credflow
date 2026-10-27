package com.example.credflow

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC")
    fun getPaymentsForCustomer(customerId: Int): Flow<List<PaymentEntity>>

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()
}
