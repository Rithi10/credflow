package com.example.credflow.di

import android.content.Context
import com.example.credflow.data.local.CredflowDatabase
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.repository.CredflowRepository
import com.example.credflow.data.repository.CredflowRepositoryImpl

class AppContainer(context: Context) {
    val database: CredflowDatabase = CredflowDatabase.getDatabase(context)
    val sessionManager: UserSessionManager = UserSessionManager(context)
    val apiService: CredflowApiService = CredflowApiService()

    val repository: CredflowRepository = CredflowRepositoryImpl(
        customerDao = database.customerDao(),
        paymentDao = database.paymentDao(),
        pendingOperationDao = database.pendingOperationDao(),
        apiService = apiService,
        sessionManager = sessionManager
    )
}

