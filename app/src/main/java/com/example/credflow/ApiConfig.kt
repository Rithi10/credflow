package com.example.credflow

object ApiConfig {
    val BASE_URL: String
        get() = com.example.credflow.data.remote.ApiConfig.baseUrl

    val AI_BASE_URL: String
        get() = com.example.credflow.data.remote.ApiConfig.aiBaseUrl
}
