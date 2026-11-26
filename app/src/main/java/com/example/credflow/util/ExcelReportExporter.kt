package com.example.credflow.util

import android.content.Context
import com.example.credflow.data.remote.dto.ExportReportDataDto
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class ExcelCellValue {
    data class Text(val value: String) : ExcelCellValue()
    data class Number(val value: Double) : ExcelCellValue()
}

data class ExcelSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<ExcelCellValue>>
)

object ExcelReportExporter {
    const val MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun buildFileName(cycleFilter: String = "all", date: LocalDate = LocalDate.now()): String {
        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val suffix = if (cycleFilter.equals("all", ignoreCase = true) || cycleFilter.isBlank()) {
            ""
        } else {
            "_${cycleFilter.trim()}"
        }
        return "Credflow_Report${suffix}_$dateStr.xlsx"
    }

    fun buildSheets(data: ExportReportDataDto): List<ExcelSheet> {
        val summaryRows = listOf(
            listOf(ExcelCellValue.Text("Business Name"), ExcelCellValue.Text(data.business.businessName)),
            listOf(ExcelCellValue.Text("Owner Name"), ExcelCellValue.Text(data.business.ownerName)),
            listOf(ExcelCellValue.Text("Generated At"), ExcelCellValue.Text(data.generatedAt)),
            listOf(ExcelCellValue.Text("Cycle Filter"), ExcelCellValue.Text(data.cycleFilter)),
            listOf(ExcelCellValue.Text("Start Date"), ExcelCellValue.Text(data.startDate ?: "All")),
            listOf(ExcelCellValue.Text("End Date"), ExcelCellValue.Text(data.endDate ?: "All")),
            listOf(ExcelCellValue.Text("Total Customers"), ExcelCellValue.Number(data.summary.totalCustomers.toDouble())),
            listOf(ExcelCellValue.Text("Total Principal Loan"), ExcelCellValue.Number(data.summary.totalPrincipal)),
            listOf(ExcelCellValue.Text("Total Interest"), ExcelCellValue.Number(data.summary.totalInterest)),
            listOf(ExcelCellValue.Text("Total Repayment Due"), ExcelCellValue.Number(data.summary.totalRepayment)),
            listOf(ExcelCellValue.Text("Total Collected"), ExcelCellValue.Number(data.summary.totalCollected)),
            listOf(ExcelCellValue.Text("Total Outstanding Balance"), ExcelCellValue.Number(data.summary.totalOutstanding)),
            listOf(ExcelCellValue.Text("Total Overdue Amount"), ExcelCellValue.Number(data.summary.totalOverdueAmount)),
            listOf(ExcelCellValue.Text("Overdue Customers Count"), ExcelCellValue.Number(data.summary.overdueCustomersCount.toDouble())),
            listOf(ExcelCellValue.Text("Total Payments Count"), ExcelCellValue.Number(data.summary.totalPaymentsCount.toDouble())),
            listOf(ExcelCellValue.Text("Filtered Payments Total"), ExcelCellValue.Number(data.summary.filteredPaymentsTotal))
        )

        val customerHeaders = listOf(
            "Customer ID", "Name", "Phone", "Cycle", "Collection Day",
            "Loan Amount", "Interest %", "Interest Type", "Total Interest", "Total Repayment",
            "Total Paid", "Remaining Balance", "Installment Amount",
            "Total Installments", "Paid Installments", "Pending Installments", "Overdue Installments",
            "Overdue Amount", "Overdue Days", "Next Due Date", "Last Payment Date", "Payments Count", "Status"
        )
        val customerRows = data.customers.map { c ->
            listOf(
                ExcelCellValue.Number(c.id.toDouble()),
                ExcelCellValue.Text(c.name),
                ExcelCellValue.Text(c.phone),
                ExcelCellValue.Text(c.cycle),
                ExcelCellValue.Text(c.collectionDay),
                ExcelCellValue.Number(c.loanAmount),
                ExcelCellValue.Number(c.interestRate),
                ExcelCellValue.Text(c.interestType),
                ExcelCellValue.Number(c.totalInterest),
                ExcelCellValue.Number(c.totalDue),
                ExcelCellValue.Number(c.totalPaid),
                ExcelCellValue.Number(c.remainingBalance),
                ExcelCellValue.Number(c.installmentAmount),
                ExcelCellValue.Number(c.totalInstallments.toDouble()),
                ExcelCellValue.Number(c.paidInstallmentsCount.toDouble()),
                ExcelCellValue.Number(c.pendingInstallmentsCount.toDouble()),
                ExcelCellValue.Number(c.overdueInstallmentsCount.toDouble()),
                ExcelCellValue.Number(c.overdueAmount),
                ExcelCellValue.Number(c.overdueDays.toDouble()),
                ExcelCellValue.Text(c.nextDueDate),
                ExcelCellValue.Text(c.lastPaymentDate.ifBlank { "None" }),
                ExcelCellValue.Number(c.paymentsCount.toDouble()),
                ExcelCellValue.Text(c.status)
            )
        }

        val paymentHeaders = listOf(
            "Payment ID", "Customer ID", "Customer Name", "Phone", "Cycle",
            "Payment Date", "Amount", "Payment Method", "Recorded At"
        )
        val paymentRows = data.payments.map { p ->
            listOf(
                ExcelCellValue.Number(p.paymentId.toDouble()),
                ExcelCellValue.Number(p.customerId.toDouble()),
                ExcelCellValue.Text(p.customerName),
                ExcelCellValue.Text(p.customerPhone),
                ExcelCellValue.Text(p.cycle),
                ExcelCellValue.Text(p.paymentDate),
                ExcelCellValue.Number(p.amount),
                ExcelCellValue.Text(p.method),
                ExcelCellValue.Text(p.createdAt)
            )
        }

        val installmentHeaders = listOf(
            "Customer ID", "Customer Name", "Phone", "Cycle",
            "Installment #", "Due Date", "Installment Amount", "Paid Amount", "Unpaid Amount", "Status", "Days Overdue"
        )
        val installmentRows = data.pendingAndOverdueInstallments.map { inst ->
            listOf(
                ExcelCellValue.Number(inst.customerId.toDouble()),
                ExcelCellValue.Text(inst.customerName),
                ExcelCellValue.Text(inst.customerPhone),
                ExcelCellValue.Text(inst.cycle),
                ExcelCellValue.Number(inst.installmentNumber.toDouble()),
                ExcelCellValue.Text(inst.dueDate),
                ExcelCellValue.Number(inst.amount),
                ExcelCellValue.Number(inst.paidAmount),
                ExcelCellValue.Number(inst.unpaidAmount),
                ExcelCellValue.Text(inst.status),
                ExcelCellValue.Number(inst.daysOverdue.toDouble())
            )
        }

        return listOf(
            ExcelSheet("Summary", listOf("Metric", "Value"), summaryRows),
            ExcelSheet("Customers", customerHeaders, customerRows),
            ExcelSheet("Payments", paymentHeaders, paymentRows),
            ExcelSheet("Pending & Overdue", installmentHeaders, installmentRows)
        )
    }

    fun generateXlsxBytes(data: ExportReportDataDto): ByteArray {
        val sheets = buildSheets(data)
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size))
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets))
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheets.size))
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            sheets.forEachIndexed { index, sheet ->
                writeZipEntry(zip, "xl/worksheets/sheet${index + 1}.xml", buildSheetXml(sheet))
            }
        }
        return baos.toByteArray()
    }

    fun exportAndSave(context: Context, data: ExportReportDataDto): Result<ExportResult> {
        val fileName = buildFileName(data.cycleFilter)
        val bytes = generateXlsxBytes(data)
        return ExportFileSaver.saveAndCreateResult(
            context = context,
            fileName = fileName,
            mimeType = MIME_TYPE_XLSX,
            bytes = bytes,
            rowCount = data.customers.size + data.payments.size,
            shareSubject = "Credflow Excel Report (${data.customers.size} customers, ${data.payments.size} payments)",
            shareText = "Credflow Excel report exported on ${data.generatedAt} (${data.customers.size} customers, ${data.payments.size} payments).",
            chooserTitle = "Share Excel Report"
        )
    }

    private fun writeZipEntry(zip: ZipOutputStream, entryName: String, content: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
        append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
        for (i in 1..sheetCount) {
            append("""<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
        }
        append("""</Types>""")
    }

    private fun buildWorkbookXml(sheets: List<ExcelSheet>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
        append("""<sheets>""")
        sheets.forEachIndexed { idx, sheet ->
            val safeName = escapeXml(sanitizeSheetName(sheet.name))
            append("""<sheet name="$safeName" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>""")
        }
        append("""</sheets>""")
        append("""</workbook>""")
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
        for (i in 1..sheetCount) {
            append("""<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""")
        }
        append("""<Relationship Id="rId${sheetCount + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        append("""</Relationships>""")
    }

    private fun buildSheetXml(sheet: ExcelSheet): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<sheetData>""")

        // Row 1: Headers (style 1 = bold header)
        append("""<row r="1">""")
        sheet.headers.forEachIndexed { colIdx, header ->
            val ref = "${columnLetter(colIdx)}1"
            append("""<c r="$ref" t="inlineStr" s="1"><is><t>${escapeXml(header)}</t></is></c>""")
        }
        append("""</row>""")

        // Data rows
        sheet.rows.forEachIndexed { rowIdx, cells ->
            val excelRow = rowIdx + 2
            append("""<row r="$excelRow">""")
            cells.forEachIndexed { colIdx, cell ->
                val ref = "${columnLetter(colIdx)}$excelRow"
                when (cell) {
                    is ExcelCellValue.Text -> {
                        append("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${escapeXml(cell.value)}</t></is></c>""")
                    }
                    is ExcelCellValue.Number -> {
                        val numStr = if (cell.value % 1.0 == 0.0 && kotlin.math.abs(cell.value) < 1e12) {
                            cell.value.toLong().toString()
                        } else {
                            String.format(Locale.ROOT, "%.2f", cell.value)
                        }
                        append("""<c r="$ref" t="n"><v>$numStr</v></c>""")
                    }
                }
            }
            append("""</row>""")
        }

        append("""</sheetData>""")
        append("""</worksheet>""")
    }

    private fun sanitizeSheetName(name: String): String {
        return name.replace(Regex("[\\\\/*?:\\[\\]]"), " ").trim().take(31).ifBlank { "Sheet" }
    }

    private fun columnLetter(zeroBasedIndex: Int): String {
        var index = zeroBasedIndex
        val sb = StringBuilder()
        while (index >= 0) {
            val rem = index % 26
            sb.insert(0, ('A'.code + rem).toChar())
            index = index / 26 - 1
        }
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> if (ch.code == 0x9 || ch.code == 0xA || ch.code == 0xD || ch.code >= 0x20) {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    private const val ROOT_RELS_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""

    private const val STYLES_XML =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF173865"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs></styleSheet>"""
}

