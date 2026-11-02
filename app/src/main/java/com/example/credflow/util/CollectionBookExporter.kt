package com.example.credflow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.LoanScheduleCalculator
import com.example.credflow.domain.model.Payment
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ExportResult(
    val uri: Uri,
    val fileName: String,
    val rowCount: Int,
    val shareIntent: Intent,
    val viewIntent: Intent
)

object CollectionBookExporter {

    fun generateCsv(customers: List<Customer>, payments: List<Payment>): String {
        val today = LocalDate.now()
        val builder = StringBuilder()

        // 1. Header Row
        builder.append(
            "Customer ID,Name,Phone,Loan Amount,Interest %,Frequency,Due Amount,Total Paid,Outstanding Balance,Payment Status,Next Due Date,Remaining Weeks,Last Payment Date,Number of Payments\n"
        )

        // 2. Data Rows
        for (c in customers) {
            val customerPayments = payments.filter { it.customerId == c.id }
            val (isOverdue, _) = LoanScheduleCalculator.getOverdueInfo(c, today, payments)
            val paymentStatus = when {
                c.due <= 0.0 -> "PAID"
                isOverdue -> "OVERDUE"
                else -> "ACTIVE"
            }
            val nextDueDate = LoanScheduleCalculator.getNextDueDate(c, today, payments)
            val remainingWeeks = LoanScheduleCalculator.getRemainingCycles(c)
            val lastPaymentDate = customerPayments.map { it.paymentDate }.maxOrNull() ?: "None"
            val numberOfPayments = customerPayments.size

            builder.append(c.id).append(",")
            builder.append(escapeCsv(c.name)).append(",")
            builder.append(escapeCsv(c.phone)).append(",")
            builder.append(String.format(Locale.ROOT, "%.2f", c.totalDue)).append(",")
            builder.append(String.format(Locale.ROOT, "%.2f", c.interest)).append(",")
            builder.append(escapeCsv(c.cycle)).append(",")
            builder.append(String.format(Locale.ROOT, "%.2f", c.amount)).append(",")
            builder.append(String.format(Locale.ROOT, "%.2f", c.paid)).append(",")
            builder.append(String.format(Locale.ROOT, "%.2f", c.due)).append(",")
            builder.append(paymentStatus).append(",")
            builder.append(nextDueDate).append(",")
            builder.append(remainingWeeks).append(",")
            builder.append(escapeCsv(lastPaymentDate)).append(",")
            builder.append(numberOfPayments).append("\n")
        }

        return builder.toString()
    }

    fun exportAndSave(
        context: Context,
        customers: List<Customer>,
        payments: List<Payment>
    ): Result<ExportResult> {
        try {
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val fileName = "Credflow_Collection_Book_$dateStr.csv"
            val csvContent = generateCsv(customers, payments)
            val bytes = csvContent.toByteArray(Charsets.UTF_8)

            // Cache file for FileProvider sharing
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { it.write(bytes) }

            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // Save to Downloads via MediaStore if Android 10+
            var finalUri = fileProviderUri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val insertedUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (insertedUri != null) {
                    context.contentResolver.openOutputStream(insertedUri)?.use { it.write(bytes) }
                    finalUri = insertedUri
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileProviderUri)
                putExtra(Intent.EXTRA_SUBJECT, "Credflow Collection Book - $dateStr")
                putExtra(Intent.EXTRA_TEXT, "Credflow Collection Book exported on $dateStr (${customers.size} customers).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileProviderUri, "text/csv")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            return Result.success(
                ExportResult(
                    uri = finalUri,
                    fileName = fileName,
                    rowCount = customers.size,
                    shareIntent = Intent.createChooser(shareIntent, "Share Collection Book"),
                    viewIntent = viewIntent
                )
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
