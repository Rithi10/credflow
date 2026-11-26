package com.example.credflow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.credflow.data.local.AppSettingsManager
import com.example.credflow.data.local.CredflowDatabase
import com.example.credflow.domain.model.LoanScheduleCalculator
import com.example.credflow.util.NotificationHelper
import java.time.LocalDate

class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = AppSettingsManager(context)
        if (!settings.dailyReminders) {
            return Result.success()
        }

        val today = LocalDate.now()
        val todayStr = today.toString()

        // Prevent duplicate notifications on the same day
        if (settings.lastReminderDate == todayStr) {
            return Result.success()
        }

        val database = CredflowDatabase.getDatabase(context)
        val customerEntities = database.customerDao().getAllCustomersList()
        val customers = customerEntities.map { it.toDomainModel() }

        val dueCustomers = customers.filter { LoanScheduleCalculator.isDueToday(it, today) }
        if (dueCustomers.isNotEmpty()) {
            val totalDue = dueCustomers.sumOf { it.due }
            val firstCustomer = dueCustomers.first()
            NotificationHelper.showDailyReminder(
                context = context,
                pendingCount = dueCustomers.size,
                totalDue = totalDue,
                firstName = firstCustomer.name
            )
            settings.lastReminderDate = todayStr
        }

        return Result.success()
    }
}
