package com.example.credflow.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(op: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE operationId = :opId")
    suspend fun deleteOperation(opId: String)

    @Query("DELETE FROM pending_operations")
    suspend fun clearAll()
}

