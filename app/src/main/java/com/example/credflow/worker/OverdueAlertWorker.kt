package com.example.credflow.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.credflow.data.local.AppSettingsManager
import com.example.credflow.data.local.CredflowDatabase
import com.example.credflow.domain.model.LoanScheduleCalculator
import com.example.credflow.util.NotificationHelper
import java.time.LocalDate

class OverdueAlertWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = AppSettingsManager(context)
        if (!settings.overdueAlerts) {
            return Result.success()
        }

        val today = LocalDate.now()
        val todayStr = today.toString()

        // Prevent duplicate alerts on the same day
        if (settings.lastAlertDate == todayStr) {
            return Result.success()
        }

        val database = CredflowDatabase.getDatabase(context)
        val customerEntities = database.customerDao().getAllCustomersList()
        val paymentEntities = database.paymentDao().getAllPaymentsList()

        val customers = customerEntities.map { it.toDomainModel() }
        val payments = paymentEntities.map { it.toDomainModel() }

        var alertedCount = 0
        for (customer in customers) {
            val (isOverdue, overdueDays) = LoanScheduleCalculator.getOverdueInfo(customer, today, payments)
            if (isOverdue && overdueDays > 0) {
                NotificationHelper.showOverdueAlert(context, customer, overdueDays)
                alertedCount++
            }
        }

        if (alertedCount > 0) {
            settings.lastAlertDate = todayStr
        }

        return Result.success()
    }
}
