package com.example.credflow.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object CustomerLedgerPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_X = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 72f
    private const val FOOTER_Y = 812f
    private const val MAX_CONTENT_Y = 785f

    private val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())

    fun formatCurrency(value: Double): String =
        currencyFormat.format(value).replace("INR", "₹")

    fun sanitizeFilePart(name: String): String {
        val cleaned = name.trim().replace(Regex("[^A-Za-z0-9_-]+"), "_").trim('_')
        return cleaned.ifBlank { "Customer" }.take(32)
    }

    fun buildFileName(customerName: String, date: LocalDate = LocalDate.now()): String {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return "Credflow_Ledger_${sanitizeFilePart(customerName)}_$dateStr.pdf"
    }

    fun generatePdfBytes(ledger: CustomerLedgerDto): ByteArray {
        val document = PdfDocument()
        val navyColor = Color.rgb(23, 56, 101)
        val blueColor = Color.rgb(35, 93, 197)
        val greenColor = Color.rgb(20, 133, 107)
        val redColor = Color.rgb(198, 40, 40)
        val orangeColor = Color.rgb(183, 107, 18)
        val mutedColor = Color.rgb(97, 113, 135)
        val borderColor = Color.rgb(221, 229, 239)
        val lightBgColor = Color.rgb(243, 246, 250)

        val headerBgPaint = Paint().apply { color = navyColor; style = Paint.Style.FILL }
        val cardBgPaint = Paint().apply { color = lightBgColor; style = Paint.Style.FILL }
        val rowAltPaint = Paint().apply { color = Color.rgb(249, 251, 253); style = Paint.Style.FILL }
        val tableHeaderPaint = Paint().apply { color = blueColor; style = Paint.Style.FILL }
        val linePaint = Paint().apply { color = borderColor; strokeWidth = 1f }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val whiteSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(202, 217, 239)
            textSize = 10f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 9.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 9.5f
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            textSize = 8.5f
        }

        var pageNumber = 1
        var currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas: Canvas = currentPage.canvas
        var y = 32f

        fun finishCurrentPage() {
            canvas.drawLine(MARGIN_X, FOOTER_Y - 12f, PAGE_WIDTH - MARGIN_X, FOOTER_Y - 12f, linePaint)
            canvas.drawText(
                "Credflow · ${ledger.business.businessName.ifBlank { "Credflow Finance" }} · Generated ${ledger.generatedAt}",
                MARGIN_X,
                FOOTER_Y,
                footerPaint
            )
            val pageStr = "Page $pageNumber"
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN_X - footerPaint.measureText(pageStr), FOOTER_Y, footerPaint)
            document.finishPage(currentPage)
        }

        fun ensureSpace(neededHeight: Float, onNewPage: (() -> Unit)? = null) {
            if (y + neededHeight > MAX_CONTENT_Y) {
                finishCurrentPage()
                pageNumber++
                currentPage = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = currentPage.canvas
                y = 36f
                onNewPage?.invoke()
            }
        }

        // 1. Header Banner
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 74f, headerBgPaint)
        canvas.drawText(ledger.business.businessName.ifBlank { "Credflow Finance" }, MARGIN_X + 14f, y + 24f, titlePaint)
        canvas.drawText("CUSTOMER LOAN LEDGER", MARGIN_X + 14f, y + 42f, whiteSubPaint)
        val bizSub = listOf(ledger.business.ownerName, ledger.business.phone, ledger.business.gstinNumber)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        if (bizSub.isNotBlank()) {
            canvas.drawText(bizSub.take(75), MARGIN_X + 14f, y + 58f, whiteSubPaint)
        }
        val statusText = "STATUS: ${ledger.summary.status}"
        val statusWidth = titlePaint.measureText(statusText) * 0.65f
        val statusBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(statusText, PAGE_WIDTH - MARGIN_X - 14f - statusWidth - 20f, y + 24f, statusBadgePaint)
        y += 88f

        // 2. Customer & Loan Summary Box
        val c = ledger.customer
        val s = ledger.summary
        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 116f, cardBgPaint)
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, linePaint)
        canvas.drawLine(MARGIN_X, y + 116f, PAGE_WIDTH - MARGIN_X, y + 116f, linePaint)

        val col1X = MARGIN_X + 12f
        val col2X = MARGIN_X + 195f
        val col3X = MARGIN_X + 370f

        canvas.drawText("CUSTOMER", col1X, y + 18f, labelPaint)
        canvas.drawText(c.name.take(26), col1X, y + 32f, valuePaint)
        canvas.drawText("Phone: ${c.phone.ifBlank { "-" }}", col1X, y + 46f, bodyPaint)
        canvas.drawText("Cycle: ${c.cycle} (${c.collectionDay})", col1X, y + 60f, bodyPaint)
        canvas.drawText("Period: ${c.period} ${c.periodUnit}", col1X, y + 74f, bodyPaint)
        canvas.drawText("Next Due: ${s.nextDueDate.ifBlank { "-" }}", col1X, y + 88f, bodyPaint)
        canvas.drawText("Installments: ${s.paidInstallmentsCount}/${s.totalInstallments} paid", col1X, y + 102f, bodyPaint)

        canvas.drawText("LOAN & INTEREST", col2X, y + 18f, labelPaint)
        canvas.drawText("Principal: ${formatCurrency(s.loanAmount)}", col2X, y + 34f, bodyPaint)
        canvas.drawText("Interest: ${s.interestRate}% (${s.interestType})", col2X, y + 49f, bodyPaint)
        canvas.drawText("Total Interest: ${formatCurrency(s.totalInterest)}", col2X, y + 64f, bodyPaint)
        canvas.drawText("Total Due: ${formatCurrency(s.totalRepayment)}", col2X, y + 79f, valuePaint)
        canvas.drawText("EMI: ${formatCurrency(s.installmentAmount)}", col2X, y + 94f, bodyPaint)

        canvas.drawText("BALANCE SUMMARY", col3X, y + 18f, labelPaint)
        val paidPaint = Paint(valuePaint).apply { color = greenColor }
        val duePaint = Paint(valuePaint).apply { color = if (s.remainingBalance > 0) redColor else greenColor }
        val overduePaint = Paint(bodyPaint).apply { color = if (s.overdueAmount > 0) redColor else mutedColor }
        canvas.drawText("Paid: ${formatCurrency(s.totalPaid)}", col3X, y + 34f, paidPaint)
        canvas.drawText("Balance: ${formatCurrency(s.remainingBalance)}", col3X, y + 50f, duePaint)
        canvas.drawText("Pending Inst: ${s.pendingInstallmentsCount} (${formatCurrency(s.pendingAmount)})", col3X, y + 66f, bodyPaint)
        canvas.drawText("Overdue Inst: ${s.overdueInstallmentsCount} (${formatCurrency(s.overdueAmount)})", col3X, y + 82f, overduePaint)
        if (s.overdueDays > 0) {
            canvas.drawText("Overdue by ${s.overdueDays} day(s)", col3X, y + 98f, overduePaint)
        }
        y += 132f

        // 3. Overdue & Pending Installments Section
        val attentionItems = ledger.overdueInstallments + ledger.pendingInstallments
        ensureSpace(44f)
        canvas.drawText("Pending & Overdue Installments (${attentionItems.size})", MARGIN_X, y, sectionPaint)
        y += 10f

        if (attentionItems.isEmpty()) {
            y += 14f
            canvas.drawText("All installments are paid in full. No pending or overdue installments.", MARGIN_X, y, bodyPaint)
            y += 18f
        } else {
            fun drawAttentionHeader() {
                canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 20f, tableHeaderPaint)
                canvas.drawText("#", MARGIN_X + 8f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Due Date", MARGIN_X + 44f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Inst. Amt", MARGIN_X + 145f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Paid", MARGIN_X + 235f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Unpaid", MARGIN_X + 315f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Status", MARGIN_X + 400f, y + 13f, tableHeaderTextPaint)
                y += 22f
            }
            drawAttentionHeader()
            attentionItems.forEachIndexed { idx, inst ->
                ensureSpace(20f) { drawAttentionHeader() }
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 18f, rowAltPaint)
                }
                val stPaint = Paint(bodyPaint).apply {
                    color = if (inst.isOverdue) redColor else orangeColor
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val statusLabel = if (inst.isOverdue && inst.daysOverdue > 0) "${inst.status} (${inst.daysOverdue}d)" else inst.status
                canvas.drawText("#${inst.installmentNumber}", MARGIN_X + 8f, y + 12f, bodyPaint)
                canvas.drawText(inst.dueDate, MARGIN_X + 44f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.amount), MARGIN_X + 145f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.paidAmount), MARGIN_X + 235f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.unpaidAmount), MARGIN_X + 315f, y + 12f, bodyPaint)
                canvas.drawText(statusLabel.take(20), MARGIN_X + 400f, y + 12f, stPaint)
                y += 18f
            }
            y += 14f
        }

        // 4. Complete Payment History Section
        ensureSpace(44f)
        canvas.drawText("Payment History (${ledger.payments.size})", MARGIN_X, y, sectionPaint)
        y += 10f

        if (ledger.payments.isEmpty()) {
            y += 14f
            canvas.drawText("No payments recorded for this customer yet.", MARGIN_X, y, bodyPaint)
            y += 18f
        } else {
            fun drawPaymentHeader() {
                canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 20f, tableHeaderPaint)
                canvas.drawText("Payment ID", MARGIN_X + 8f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Payment Date", MARGIN_X + 100f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Mode / Note", MARGIN_X + 225f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Amount", MARGIN_X + 410f, y + 13f, tableHeaderTextPaint)
                y += 22f
            }
            drawPaymentHeader()
            ledger.payments.forEachIndexed { idx, p ->
                ensureSpace(20f) { drawPaymentHeader() }
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 18f, rowAltPaint)
                }
                canvas.drawText("#${p.id}", MARGIN_X + 8f, y + 12f, bodyPaint)
                canvas.drawText(p.paymentDate, MARGIN_X + 100f, y + 12f, bodyPaint)
                canvas.drawText(p.note.take(26), MARGIN_X + 225f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(p.amount), MARGIN_X + 410f, y + 12f, Paint(bodyPaint).apply { color = greenColor })
                y += 18f
            }
            y += 14f
        }

        // 5. Full Installment Schedule Section
        ensureSpace(44f)
        canvas.drawText("Complete Installment Schedule (${ledger.installments.size})", MARGIN_X, y, sectionPaint)
        y += 10f

        if (ledger.installments.isEmpty()) {
            y += 14f
            canvas.drawText("No installments scheduled.", MARGIN_X, y, bodyPaint)
        } else {
            fun drawScheduleHeader() {
                canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 20f, tableHeaderPaint)
                canvas.drawText("#", MARGIN_X + 8f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Due Date", MARGIN_X + 44f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Amount", MARGIN_X + 145f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Paid", MARGIN_X + 235f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Unpaid", MARGIN_X + 315f, y + 13f, tableHeaderTextPaint)
                canvas.drawText("Status", MARGIN_X + 400f, y + 13f, tableHeaderTextPaint)
                y += 22f
            }
            drawScheduleHeader()
            ledger.installments.forEachIndexed { idx, inst ->
                ensureSpace(20f) { drawScheduleHeader() }
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 18f, rowAltPaint)
                }
                val stColor = when {
                    inst.status.equals("Paid", true) -> greenColor
                    inst.isOverdue -> redColor
                    inst.paidAmount > 0 -> orangeColor
                    else -> mutedColor
                }
                canvas.drawText("#${inst.installmentNumber}", MARGIN_X + 8f, y + 12f, bodyPaint)
                canvas.drawText(inst.dueDate, MARGIN_X + 44f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.amount), MARGIN_X + 145f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.paidAmount), MARGIN_X + 235f, y + 12f, bodyPaint)
                canvas.drawText(formatCurrency(inst.unpaidAmount), MARGIN_X + 315f, y + 12f, bodyPaint)
                canvas.drawText(inst.status.take(20), MARGIN_X + 400f, y + 12f, Paint(bodyPaint).apply { color = stColor })
                y += 18f
            }
        }

        finishCurrentPage()

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    fun exportAndSave(context: Context, ledger: CustomerLedgerDto): Result<ExportResult> {
        val fileName = buildFileName(ledger.customer.name)
        val bytes = generatePdfBytes(ledger)
        return ExportFileSaver.saveAndCreateResult(
            context = context,
            fileName = fileName,
            mimeType = "application/pdf",
            bytes = bytes,
            rowCount = ledger.installments.size + ledger.payments.size,
            shareSubject = "Credflow Customer Ledger - ${ledger.customer.name}",
            shareText = "Customer Ledger PDF for ${ledger.customer.name} (Total Paid: ${formatCurrency(ledger.summary.totalPaid)}, Balance: ${formatCurrency(ledger.summary.remainingBalance)}).",
            chooserTitle = "Share Customer Ledger PDF"
        )
    }
}

