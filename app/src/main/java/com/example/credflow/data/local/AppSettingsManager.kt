package com.example.credflow.data.local

import android.content.Context
import android.content.SharedPreferences

class AppSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("credflow_app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DAILY_REMINDERS = "key_daily_reminders"
        private const val KEY_REMINDER_HOUR = "key_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
        private const val KEY_OVERDUE_ALERTS = "key_overdue_alerts"
        private const val KEY_ACCEPT_CASH = "key_accept_cash"
        private const val KEY_ACCEPT_UPI = "key_accept_upi"
        private const val KEY_ACCEPT_BANK = "key_accept_bank"
        private const val KEY_DEFAULT_CYCLE = "key_default_cycle"
        private const val KEY_APP_LOCK = "key_app_lock"
        private const val KEY_LATE_FEE_RULE = "key_late_fee_rule"
        private const val KEY_LAST_REMINDER_DATE = "key_last_reminder_date"
        private const val KEY_LAST_ALERT_DATE = "key_last_alert_date"
    }

    var dailyReminders: Boolean
        get() = prefs.getBoolean(KEY_DAILY_REMINDERS, true)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_REMINDERS, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    var overdueAlerts: Boolean
        get() = prefs.getBoolean(KEY_OVERDUE_ALERTS, true)
        set(value) = prefs.edit().putBoolean(KEY_OVERDUE_ALERTS, value).apply()

    var acceptCash: Boolean
        get() = prefs.getBoolean(KEY_ACCEPT_CASH, true)
        set(value) = prefs.edit().putBoolean(KEY_ACCEPT_CASH, value).apply()

    var acceptUpi: Boolean
        get() = prefs.getBoolean(KEY_ACCEPT_UPI, true)
        set(value) = prefs.edit().putBoolean(KEY_ACCEPT_UPI, value).apply()

    var acceptBank: Boolean
        get() = prefs.getBoolean(KEY_ACCEPT_BANK, false)
        set(value) = prefs.edit().putBoolean(KEY_ACCEPT_BANK, value).apply()

    var defaultCycle: String
        get() = prefs.getString(KEY_DEFAULT_CYCLE, "Daily") ?: "Daily"
        set(value) = prefs.edit().putString(KEY_DEFAULT_CYCLE, value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK, value).apply()

    var lateFeeRule: String
        get() = prefs.getString(KEY_LATE_FEE_RULE, "No Charge") ?: "No Charge"
        set(value) = prefs.edit().putString(KEY_LATE_FEE_RULE, value).apply()

    var lastReminderDate: String
        get() = prefs.getString(KEY_LAST_REMINDER_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_REMINDER_DATE, value).apply()

    var lastAlertDate: String
        get() = prefs.getString(KEY_LAST_ALERT_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_ALERT_DATE, value).apply()
}

