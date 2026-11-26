package com.example.credflow.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

object PaymentReceiptPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 40f

    private val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())

    fun formatCurrency(value: Double): String =
        currencyFormat.format(value).replace("INR", "₹")

    fun sanitizeReceiptCode(code: String): String {
        val cleaned = code.trim().replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_')
        return cleaned.ifBlank { "Receipt" }.take(40)
    }

    fun buildFileName(receiptNumber: String, paymentId: Int = 0): String {
        val code = if (receiptNumber.isNotBlank()) {
            sanitizeReceiptCode(receiptNumber)
        } else {
            "CF_${paymentId.coerceAtLeast(1)}"
        }
        return "Credflow_Receipt_$code.pdf"
    }

    fun buildShareMessage(receipt: PaymentReceiptDto): String {
        val bizName = receipt.business.businessName.ifBlank { "Credflow Finance" }
        return buildString {
            append("$bizName - Payment Receipt\n")
            append("Receipt No: ${receipt.receiptNumber}\n")
            append("Customer: ${receipt.customerName}")
            if (receipt.customerPhone.isNotBlank()) {
                append(" (${receipt.customerPhone})")
            }
            append("\n")
            append("Amount Paid: ${formatCurrency(receipt.paymentAmount)}\n")
            append("Payment Date: ${receipt.paymentDate}\n")
            append("Payment Method: ${receipt.paymentMethod}\n")
            append("Total Loan Amount: ${formatCurrency(receipt.totalLoanAmount)}\n")
            append("Total Paid: ${formatCurrency(receipt.totalPaidAmount)}\n")
            append("Remaining Balance: ${formatCurrency(receipt.remainingBalance)}")
        }
    }

    fun generatePdfBytes(receipt: PaymentReceiptDto): ByteArray {
        val document = PdfDocument()
        val navyColor = Color.rgb(23, 56, 101)
        val blueColor = Color.rgb(35, 93, 197)
        val greenColor = Color.rgb(20, 133, 107)
        val redColor = Color.rgb(198, 40, 40)
        val mutedColor = Color.rgb(97, 113, 135)
        val borderColor = Color.rgb(221, 229, 239)
        val lightBg = Color.rgb(243, 246, 250)
        val greenBg = Color.rgb(232, 245, 233)

        val headerBgPaint = Paint().apply { color = navyColor; style = Paint.Style.FILL }
        val cardBgPaint = Paint().apply { color = lightBg; style = Paint.Style.FILL }
        val highlightBgPaint = Paint().apply { color = greenBg; style = Paint.Style.FILL }
        val linePaint = Paint().apply { color = borderColor; strokeWidth = 1f }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 217, 239)
            textSize = 10.5f
        }
        val receiptTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 10.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bigAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 9f
        }

        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas
        var y = 40f

        // 1. Header Banner
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 86f, headerBgPaint)
        val bizName = receipt.business.businessName.ifBlank { "Credflow Finance" }
        canvas.drawText(bizName, MARGIN_X + 16f, y + 28f, titlePaint)
        canvas.drawText("OFFICIAL PAYMENT RECEIPT", MARGIN_X + 16f, y + 48f, whiteSubPaint)
        val bizContact = listOf(receipt.business.ownerName, receipt.business.phone, receipt.business.gstinNumber)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (bizContact.isNotBlank()) {
            canvas.drawText(bizContact.take(72), MARGIN_X + 16f, y + 66f, whiteSubPaint)
        }

        val recCodeText = "Receipt #${receipt.receiptNumber}"
        val recWidth = receiptTagPaint.measureText(recCodeText)
        canvas.drawText(recCodeText, PAGE_WIDTH - MARGIN_X - 16f - recWidth, y + 30f, receiptTagPaint)
        canvas.drawText(
            "Date: ${receipt.paymentDate}",
            PAGE_WIDTH - MARGIN_X - 16f - whiteSubPaint.measureText("Date: ${receipt.paymentDate}"),
            y + 50f,
            whiteSubPaint
        )
        y += 104f

        // 2. Highlighted Payment Amount Banner
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 76f, highlightBgPaint)
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, linePaint)
        canvas.drawLine(MARGIN_X, y + 76f, PAGE_WIDTH - MARGIN_X, y + 76f, linePaint)
        canvas.drawText("AMOUNT RECEIVED", MARGIN_X + 16f, y + 24f, labelPaint)
        canvas.drawText(formatCurrency(receipt.paymentAmount), MARGIN_X + 16f, y + 54f, bigAmountPaint)
        val methodLabel = "Method: ${receipt.paymentMethod.ifBlank { "Cash" }}"
        canvas.drawText(
            methodLabel,
            PAGE_WIDTH - MARGIN_X - 16f - valuePaint.measureText(methodLabel),
            y + 44f,
            Paint(valuePaint).apply { color = blueColor }
        )
        y += 96f

        // 3. Customer & Receipt Details Card
        canvas.drawText("Customer & Transaction Details", MARGIN_X, y, sectionPaint)
        y += 10f
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 122f, cardBgPaint)
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, linePaint)
        canvas.drawLine(MARGIN_X, y + 122f, PAGE_WIDTH - MARGIN_X, y + 122f, linePaint)

        fun drawRow(rowY: Float, label: String, value: String, vPaint: Paint = valuePaint) {
            canvas.drawText(label, MARGIN_X + 16f, rowY, labelPaint)
            val w = vPaint.measureText(value)
            canvas.drawText(value, PAGE_WIDTH - MARGIN_X - 16f - w, rowY, vPaint)
        }

        drawRow(y + 24f, "Receipt Number", receipt.receiptNumber.ifBlank { "-" })
        drawRow(y + 46f, "Customer Name", receipt.customerName.ifBlank { "-" })
        drawRow(y + 68f, "Phone Number", receipt.customerPhone.ifBlank { "N/A" })
        drawRow(y + 90f, "Payment Date", receipt.paymentDate.ifBlank { "-" })
        drawRow(y + 112f, "Payment Method", receipt.paymentMethod.ifBlank { "Cash" })
        y += 144f

        // 4. Loan & Balance Summary Card
        canvas.drawText("Loan Balance Summary", MARGIN_X, y, sectionPaint)
        y += 10f
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 102f, cardBgPaint)
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, linePaint)
        canvas.drawLine(MARGIN_X, y + 102f, PAGE_WIDTH - MARGIN_X, y + 102f, linePaint)

        drawRow(y + 26f, "Total Loan Amount", formatCurrency(receipt.totalLoanAmount))
        drawRow(
            y + 52f,
            "Total Amount Paid (to date)",
            formatCurrency(receipt.totalPaidAmount),
            Paint(valuePaint).apply { color = greenColor }
        )
        drawRow(
            y + 80f,
            "Remaining Balance",
            formatCurrency(receipt.remainingBalance),
            Paint(valuePaint).apply {
                color = if (receipt.remainingBalance > 0.001) redColor else greenColor
                textSize = 12.5f
            }
        )
        y += 130f

        // 5. Confirmation Note & Footer
        canvas.drawText(
            "Thank you for your payment. This is a system-generated receipt from Credflow.",
            MARGIN_X,
            y,
            footerPaint
        )
        canvas.drawLine(MARGIN_X, PAGE_HEIGHT - 56f, PAGE_WIDTH - MARGIN_X, PAGE_HEIGHT - 56f, linePaint)
        canvas.drawText(
            "$bizName · Receipt #${receipt.receiptNumber}",
            MARGIN_X,
            PAGE_HEIGHT - 38f,
            footerPaint
        )

        document.finishPage(page)
        val out = ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    fun exportAndSave(context: Context, receipt: PaymentReceiptDto): Result<ExportResult> {
        val fileName = buildFileName(receipt.receiptNumber, receipt.paymentId)
        val bytes = generatePdfBytes(receipt)
        val shareText = buildShareMessage(receipt)
        return ExportFileSaver.saveAndCreateResult(
            context = context,
            fileName = fileName,
            mimeType = "application/pdf",
            bytes = bytes,
            rowCount = 1,
            shareSubject = "Payment Receipt ${receipt.receiptNumber} - ${receipt.customerName}",
            shareText = shareText,
            chooserTitle = "Share Payment Receipt"
        )
    }
}

