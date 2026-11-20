package com.example.credflow.data.remote

import com.example.credflow.BuildConfig

object ApiConfig {
    private var currentUrl = BuildConfig.CRED_FLOW_API_BASE_URL
        .trim()
        .takeIf { it.isNotEmpty() }
        ?.let(::normalizeBaseUrl)
        ?: ""

    var baseUrl: String
        get() = currentUrl
        set(value) {
            currentUrl = normalizeBaseUrl(value)
        }

    private fun normalizeBaseUrl(value: String): String {
        val normalized = if (value.endsWith("/")) value else "$value/"
        val isHttps = normalized.startsWith("https://", ignoreCase = true)
        val isDebugHttp = BuildConfig.DEBUG &&
            normalized.startsWith("http://", ignoreCase = true)
        if (isHttps || isDebugHttp) {
            return normalized
        }

        throw IllegalArgumentException("API base URLs must use HTTPS outside debug builds.")
    }

    /**
     * GOOGLE OAUTH 2.0 WEB CLIENT ID
     */
    const val GOOGLE_WEB_CLIENT_ID: String = "930439631272-nt9uc5fe4cvks0utc7off579ri6l8sj3.apps.googleusercontent.com"

    var googleWebClientId: String
        get() = GOOGLE_WEB_CLIENT_ID
        set(_) {}

    fun getEndpoint(path: String): String {
        check(currentUrl.isNotBlank()) {
            "Configure the PHP API URL with credflowApiBaseUrl or CRED_FLOW_API_BASE_URL."
        }
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$baseUrl$cleanPath"
    }
}
