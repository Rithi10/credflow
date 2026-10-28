package com.example.credflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, PaymentEntity::class, PendingOperationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CredflowDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun paymentDao(): PaymentDao
    abstract fun pendingOperationDao(): PendingOperationDao

    companion object {
        @Volatile
        private var INSTANCE: CredflowDatabase? = null

        fun getDatabase(context: Context): CredflowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CredflowDatabase::class.java,
                    "credflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

