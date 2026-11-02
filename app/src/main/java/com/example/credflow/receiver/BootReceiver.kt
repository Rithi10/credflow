package com.example.credflow.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.credflow.data.local.AppSettingsManager
import com.example.credflow.worker.ReminderScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val settings = AppSettingsManager(context)
            if (settings.dailyReminders) {
                ReminderScheduler.scheduleDailyReminder(
                    context,
                    settings.reminderHour,
                    settings.reminderMinute
                )
            }
            if (settings.overdueAlerts) {
                ReminderScheduler.scheduleOverdueAlerts(context)
            }
        }
    }
}
