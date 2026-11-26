package com.example.credflow.di

import android.content.Context
import android.os.Build
import com.example.credflow.BuildConfig
import com.example.credflow.data.local.AppSettingsManager
import com.example.credflow.data.local.CredflowDatabase
import com.example.credflow.data.local.UserSessionManager
import com.example.credflow.data.remote.ApiConfig
import com.example.credflow.data.remote.CredflowApiService
import com.example.credflow.data.repository.CredflowRepository
import com.example.credflow.data.repository.CredflowRepositoryImpl

class AppContainer(context: Context) {
    val database: CredflowDatabase = CredflowDatabase.getDatabase(context)
    val sessionManager: UserSessionManager = UserSessionManager(context)
    val appSettings: AppSettingsManager = AppSettingsManager(context)
    val apiService: CredflowApiService = CredflowApiService()

    init {
        val customUrl = sessionManager.customBaseUrl
        val configuredUrl = BuildConfig.CRED_FLOW_API_BASE_URL.trim()
        if (!customUrl.isNullOrBlank()) {
            ApiConfig.baseUrl = customUrl
        } else if (configuredUrl.isNotBlank()) {
            ApiConfig.baseUrl = configuredUrl
        } else if (isEmulator()) {
            ApiConfig.baseUrl = ApiConfig.EMULATOR_BASE_URL
            ApiConfig.aiBaseUrl = ApiConfig.EMULATOR_AI_URL
        } else {
            ApiConfig.baseUrl = ApiConfig.DEFAULT_BASE_URL
            ApiConfig.aiBaseUrl = ApiConfig.DEFAULT_AI_URL
        }
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    val repository: CredflowRepository = CredflowRepositoryImpl(
        customerDao = database.customerDao(),
        paymentDao = database.paymentDao(),
        pendingOperationDao = database.pendingOperationDao(),
        apiService = apiService,
        sessionManager = sessionManager
    )
}
