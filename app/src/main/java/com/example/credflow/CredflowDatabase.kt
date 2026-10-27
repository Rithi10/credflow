package com.example.credflow

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CustomerEntity::class, PaymentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CredflowDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: CredflowDatabase? = null

        fun getDatabase(context: Context): CredflowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CredflowDatabase::class.java,
                    "credflow_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
