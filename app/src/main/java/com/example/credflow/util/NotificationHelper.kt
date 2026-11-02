package com.example.credflow.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.credflow.MainActivity
import com.example.credflow.R
import com.example.credflow.domain.model.Customer

object NotificationHelper {
    const val CHANNEL_REMINDERS_ID = "credflow_daily_reminders"
    const val CHANNEL_REMINDERS_NAME = "Daily Collection Reminders"

    const val CHANNEL_ALERTS_ID = "credflow_overdue_alerts"
    const val CHANNEL_ALERTS_NAME = "Overdue Customer Alerts"

    const val EXTRA_TARGET_SCREEN = "extra_target_screen"
    const val EXTRA_CUSTOMER_ID = "extra_customer_id"

    const val NOTIFICATION_ID_DAILY = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                CHANNEL_REMINDERS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for customers whose microfinance collection is due today"
                enableVibration(true)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                CHANNEL_ALERTS_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when customers become overdue on scheduled payments"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(alertsChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showDailyReminder(
        context: Context,
        pendingCount: Int,
        totalDue: Double,
        firstName: String
    ) {
        if (!hasNotificationPermission(context) || pendingCount <= 0) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_SCREEN, "DailyCustomers")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Credflow · Daily Collection Reminder"
        val contentText = if (pendingCount == 1) {
            "$firstName is due today: ₹${String.format("%.2f", totalDue)}"
        } else {
            "$firstName and ${pendingCount - 1} other(s) are due today. Total: ₹${String.format("%.2f", totalDue)}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, notification)
    }

    fun showOverdueAlert(
        context: Context,
        customer: Customer,
        overdueDays: Long
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CUSTOMER_ID, customer.id)
            putExtra(EXTRA_TARGET_SCREEN, "CustomerDetails")
        }

        val notificationId = 2000 + customer.id
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Overdue Alert: ${customer.name}"
        val contentText = "₹${String.format("%.2f", customer.due)} overdue by $overdueDays day(s). Outstanding: ₹${String.format("%.2f", customer.due)}."

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
