package com.example.credflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY id DESC")
    suspend fun getAllPaymentsList(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY id DESC")
    fun getPaymentsForCustomer(customerId: Int): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE isPendingSync = 0")
    suspend fun clearSyncedPayments()

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Transaction
    suspend fun syncPayments(payments: List<PaymentEntity>) {
        clearSyncedPayments()
        insertAll(payments)
    }

    @Query("DELETE FROM payments")
    suspend fun clearAll()
}
