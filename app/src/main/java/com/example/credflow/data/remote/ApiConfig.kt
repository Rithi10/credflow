package com.example.credflow.data.remote

object ApiConfig {
    // Reachable PC LAN IP on local Wi-Fi:
    const val PC_LAN_IP = "172.23.176.211"

    // PHP API URLs (Apache / MySQL backend):
    const val DEFAULT_BASE_URL = "http://$PC_LAN_IP/credflow_api/"
    // Android Studio Emulator on host PC:
    const val EMULATOR_BASE_URL = "http://10.0.2.2/credflow_api/"
    // USB Debugging via adb reverse (adb reverse tcp:80 tcp:80):
    const val USB_BASE_URL = "http://127.0.0.1/credflow_api/"

    // AI Service URLs (FastAPI for Chatbot & Repayment Prediction, separate from PHP API URL):
    const val AI_SERVICE_PORT = 8000
    const val DEFAULT_AI_URL = "http://$PC_LAN_IP:$AI_SERVICE_PORT/"
    const val EMULATOR_AI_URL = "http://10.0.2.2:$AI_SERVICE_PORT/"

    private var currentUrl: String = DEFAULT_BASE_URL
    private var currentAiUrl: String = DEFAULT_AI_URL

    var baseUrl: String
        get() = currentUrl
        set(value) {
            val sanitized = sanitizeUrl(value)
            currentUrl = sanitized
            currentAiUrl = deriveAiUrlFromBaseUrl(sanitized)
        }

    var aiBaseUrl: String
        get() = currentAiUrl
        set(value) {
            currentAiUrl = sanitizeAiUrl(value)
        }

    fun sanitizeUrl(url: String): String {
        var clean = url.trim()
        if (clean.isBlank()) return DEFAULT_BASE_URL
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        if (!clean.contains("/credflow_api/")) {
            clean = "${clean}credflow_api/"
        }
        return clean
    }

    fun sanitizeAiUrl(url: String): String {
        var clean = url.trim()
        if (clean.isBlank()) return DEFAULT_AI_URL
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        if (!clean.endsWith("/")) {
            clean = "$clean/"
        }
        return clean
    }

    fun deriveAiUrlFromBaseUrl(apiBaseUrl: String): String {
        return try {
            val uri = java.net.URI(apiBaseUrl)
            val scheme = uri.scheme?.takeIf { it.isNotBlank() } ?: "http"
            val host = uri.host?.takeIf { it.isNotBlank() } ?: PC_LAN_IP
            val resolvedHost = if (host == "127.0.0.1" || host.equals("localhost", ignoreCase = true)) {
                PC_LAN_IP
            } else {
                host
            }
            "$scheme://$resolvedHost:$AI_SERVICE_PORT/"
        } catch (_: Exception) {
            DEFAULT_AI_URL
        }
    }

    /**
     * GOOGLE OAUTH 2.0 WEB CLIENT ID
     *
     * IMPORTANT:
     * This MUST be the "Web application" OAuth Client ID from Google Cloud Console,
     * NOT the Android client ID.
     * Android Credential Manager requires the Web client ID as the `serverClientId`
     * to obtain an OpenID Connect (OIDC) ID token for server-side verification.
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

    fun getAiEndpoint(path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$aiBaseUrl$cleanPath"
    }
}
