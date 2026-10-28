package com.example.credflow.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserSessionManager(context: Context) {
    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "credflow_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("credflow_auth_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL_OR_PHONE = "user_identity"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_CUSTOM_BASE_URL = "custom_base_url"
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false) && !authToken.isNullOrBlank()
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, 0)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var authToken: String?
        get() = prefs.getString(KEY_AUTH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_AUTH_TOKEN, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Ramesh Kumar") ?: "Ramesh Kumar"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmailOrPhone: String
        get() = prefs.getString(KEY_USER_EMAIL_OR_PHONE, "9876543210") ?: "9876543210"
        set(value) = prefs.edit().putString(KEY_USER_EMAIL_OR_PHONE, value).apply()

    var businessName: String
        get() = prefs.getString(KEY_BUSINESS_NAME, "Credflow Finance") ?: "Credflow Finance"
        set(value) = prefs.edit().putString(KEY_BUSINESS_NAME, value).apply()

    var customBaseUrl: String?
        get() = prefs.getString(KEY_CUSTOM_BASE_URL, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_BASE_URL, value).apply()

    fun saveSession(userId: Int, token: String, name: String, identity: String, businessName: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL_OR_PHONE, identity)
            .putString(KEY_BUSINESS_NAME, businessName)
            .apply()
    }

    fun clearSession() {
        val savedBaseUrl = customBaseUrl
        prefs.edit().clear().apply()
        if (savedBaseUrl != null) {
            customBaseUrl = savedBaseUrl
        }
    }
}

