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
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_BUSINESS_NAME = "business_name"
        private const val KEY_BUSINESS_ADDRESS = "business_address"
        private const val KEY_GSTIN_NUMBER = "gstin_number"
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
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userEmailOrPhone: String
        get() = prefs.getString(KEY_USER_EMAIL_OR_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL_OR_PHONE, value).apply()

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var businessName: String
        get() = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_NAME, value).apply()

    var businessAddress: String
        get() = prefs.getString(KEY_BUSINESS_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BUSINESS_ADDRESS, value).apply()

    var gstinNumber: String
        get() = prefs.getString(KEY_GSTIN_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GSTIN_NUMBER, value).apply()

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

    fun saveProfile(name: String, bName: String, phone: String, email: String, address: String, gstin: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_BUSINESS_NAME, bName)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_BUSINESS_ADDRESS, address)
            .putString(KEY_GSTIN_NUMBER, gstin)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_ID)
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_EMAIL_OR_PHONE)
            .apply()
    }
}
