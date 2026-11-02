package com.example.credflow.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_DAILY_REMINDER = "credflow_work_daily_reminder"
    private const val WORK_OVERDUE_ALERTS = "credflow_work_overdue_alerts"

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var targetTime = now.with(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59), 0))
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_DAILY_REMINDER)
    }

    fun scheduleOverdueAlerts(context: Context) {
        val now = LocalDateTime.now()
        // Schedule overdue check daily at 10:00 AM
        var targetTime = now.with(LocalTime.of(10, 0, 0))
        if (now.isAfter(targetTime)) {
            targetTime = targetTime.plusDays(1)
        }
        val initialDelayMinutes = Duration.between(now, targetTime).toMinutes().coerceAtLeast(1)

        val workRequest = PeriodicWorkRequestBuilder<OverdueAlertWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_OVERDUE_ALERTS,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelOverdueAlerts(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_OVERDUE_ALERTS)
    }
}
