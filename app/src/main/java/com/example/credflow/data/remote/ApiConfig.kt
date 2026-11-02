package com.example.credflow.data.remote

import com.example.credflow.BuildConfig

object ApiConfig {
    // Physical device on local Wi-Fi:
    const val DEFAULT_BASE_URL = "http://172.23.176.211/credflow_api/"
    // Android Studio Emulator on host PC:
    const val EMULATOR_BASE_URL = "http://10.0.2.2/credflow_api/"

    private var currentUrl: String = normalizeBaseUrl(DEFAULT_BASE_URL)

    var baseUrl: String
        get() = currentUrl
        set(value) {
            currentUrl = normalizeBaseUrl(value)
        }

    private fun normalizeBaseUrl(value: String): String {
        val normalized = if (value.endsWith("/")) value else "$value/"
        if (BuildConfig.DEBUG || normalized.startsWith("https://", ignoreCase = true)) {
            return normalized
        }

        if (normalized.startsWith("http://", ignoreCase = true)) {
            return normalized.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        }

        throw IllegalArgumentException("Production API URLs must use HTTPS.")
    }

    /**
     * GOOGLE OAUTH 2.0 WEB CLIENT ID
     *
     * IMPORTANT:
     * This MUST be the "Web application" OAuth Client ID from Google Cloud Console,
     * NOT the Android client ID.
     * Android Credential Manager requires the Web client ID as the `serverClientId`
     * to obtain an OpenID Connect (OIDC) ID token for server-side verification.
     *
     * How to obtain:
     * 1. Open Google Cloud Console (https://console.cloud.google.com/) -> APIs & Services -> Credentials
     * 2. Find or create an OAuth 2.0 Client ID with Application type: "Web application"
     * 3. Paste the Client ID inside the quotes below:
     */
    const val GOOGLE_WEB_CLIENT_ID: String = "930439631272-nt9uc5fe4cvks0utc7off579ri6l8sj3.apps.googleusercontent.com"

    // Backward-compatible alias
    var googleWebClientId: String
        get() = GOOGLE_WEB_CLIENT_ID
        set(_) {}

    fun getEndpoint(path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$baseUrl$cleanPath"
    }
}

