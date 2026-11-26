package com.example.credflow

import com.example.credflow.data.remote.dto.BusinessInfoDto
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.ExportCustomerRowDto
import com.example.credflow.data.remote.dto.ExportInstallmentRowDto
import com.example.credflow.data.remote.dto.ExportPaymentRowDto
import com.example.credflow.data.remote.dto.ExportReportDataDto
import com.example.credflow.data.remote.dto.ExportReportSummaryDto
import com.example.credflow.data.remote.dto.LedgerCustomerDto
import com.example.credflow.data.remote.dto.LedgerInstallmentDto
import com.example.credflow.data.remote.dto.LedgerPaymentDto
import com.example.credflow.data.remote.dto.LedgerSummaryDto
import com.example.credflow.ui.viewmodel.CustomerLedgerUiState
import com.example.credflow.ui.viewmodel.ExportUiState
import com.example.credflow.util.CustomerLedgerPdfExporter
import com.example.credflow.util.ExcelReportExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream

class LedgerDtoAndExportTest {

    @Test
    fun customerLedgerDtoAndStateHoldCompleteMetrics() {
        val installments = listOf(
            LedgerInstallmentDto(
                id = 1,
                customerId = 42,
                installmentNumber = 1,
                dueDate = "2026-11-10",
                amount = 2750.0,
                paidAmount = 2750.0,
                unpaidAmount = 0.0,
                status = "Paid",
                isOverdue = false,
                isPending = false,
                daysOverdue = 0
            ),
            LedgerInstallmentDto(
                id = 2,
                customerId = 42,
                installmentNumber = 2,
                dueDate = "2026-11-17",
                amount = 2750.0,
                paidAmount = 1000.0,
                unpaidAmount = 1750.0,
                status = "Overdue (Partial)",
                isOverdue = true,
                isPending = false,
                daysOverdue = 9
            ),
            LedgerInstallmentDto(
                id = 3,
                customerId = 42,
                installmentNumber = 3,
                dueDate = "2026-11-27",
                amount = 2750.0,
                paidAmount = 0.0,
                unpaidAmount = 2750.0,
                status = "Pending",
                isOverdue = false,
                isPending = true,
                daysOverdue = 0
            )
        )

        val payments = listOf(
            LedgerPaymentDto(
                id = 102,
                customerId = 42,
                customerName = "Arjun Kumar",
                amount = 1000.0,
                paymentDate = "2026-11-18",
                note = "UPI"
            ),
            LedgerPaymentDto(
                id = 101,
                customerId = 42,
                customerName = "Arjun Kumar",
                amount = 2750.0,
                paymentDate = "2026-11-10",
                note = "Cash"
            )
        )

        val ledger = CustomerLedgerDto(
            success = true,
            generatedAt = "2026-11-26 09:00:00",
            business = BusinessInfoDto(ownerName = "Alice", businessName = "Alice Finance"),
            customer = LedgerCustomerDto(
                id = 42,
                userId = 7,
                name = "Arjun Kumar",
                phone = "9876543210",
                amount = 10000.0,
                interest = 10.0,
                totalInterest = 1000.0,
                totalDue = 11000.0,
                paidAmount = 3750.0,
                remainingBalance = 7250.0,
                cycle = "Weekly",
                collectionDay = "Monday",
                status = "OVERDUE"
            ),
            summary = LedgerSummaryDto(
                loanAmount = 10000.0,
                interestRate = 10.0,
                totalInterest = 1000.0,
                totalRepayment = 11000.0,
                totalPaid = 3750.0,
                remainingBalance = 7250.0,
                installmentAmount = 2750.0,
                totalInstallments = 4,
                paidInstallmentsCount = 1,
                pendingInstallmentsCount = 2,
                overdueInstallmentsCount = 1,
                pendingAmount = 5500.0,
                overdueAmount = 1750.0,
                nextDueDate = "2026-11-17",
                overdueDays = 9,
                paymentsCount = 2,
                status = "OVERDUE"
            ),
            installments = installments,
            pendingInstallments = installments.filter { it.isPending },
            overdueInstallments = installments.filter { it.isOverdue },
            payments = payments
        )

        val state = CustomerLedgerUiState.Success(ledger = ledger, selectedTab = "dues")
        assertTrue(state.ledger.success)
        assertEquals("Arjun Kumar", state.ledger.customer.name)
        assertEquals(10000.0, state.ledger.summary.loanAmount, 0.001)
        assertEquals(1000.0, state.ledger.summary.totalInterest, 0.001)
        assertEquals(3750.0, state.ledger.summary.totalPaid, 0.001)
        assertEquals(7250.0, state.ledger.summary.remainingBalance, 0.001)
        assertEquals(1, state.ledger.overdueInstallments.size)
        assertEquals(1, state.ledger.pendingInstallments.size)
        assertEquals(2, state.ledger.payments.size)
        assertEquals("dues", state.selectedTab)

        val errState = CustomerLedgerUiState.Error("Customer not found or unauthorized.", 42)
        assertEquals(42, errState.customerId)
        assertFalse(CustomerLedgerDto(success = false, error = errState.message).success)
    }

    @Test
    fun excelReportExporterGeneratesValidXlsxZipWithAllFourSheets() {
        val reportData = ExportReportDataDto(
            success = true,
            generatedAt = "2026-11-26 09:30:00",
            cycleFilter = "all",
            business = BusinessInfoDto(ownerName = "Alice", businessName = "Alice & Co <Finance>"),
            summary = ExportReportSummaryDto(
                totalCustomers = 1,
                totalPrincipal = 10000.0,
                totalInterest = 1000.0,
                totalRepayment = 11000.0,
                totalCollected = 3750.0,
                totalOutstanding = 7250.0,
                totalOverdueAmount = 1750.0,
                overdueCustomersCount = 1,
                totalPaymentsCount = 1,
                filteredPaymentsTotal = 3750.0
            ),
            customers = listOf(
                ExportCustomerRowDto(
                    id = 42,
                    name = "Arjun & Sons",
                    phone = "9876543210",
                    cycle = "Weekly",
                    collectionDay = "Monday",
                    loanAmount = 10000.0,
                    interestRate = 10.0,
                    interestType = "Flat Interest",
                    totalInterest = 1000.0,
                    totalDue = 11000.0,
                    totalPaid = 3750.0,
                    remainingBalance = 7250.0,
                    installmentAmount = 2750.0,
                    totalInstallments = 4,
                    paidInstallmentsCount = 1,
                    pendingInstallmentsCount = 2,
                    overdueInstallmentsCount = 1,
                    overdueAmount = 1750.0,
                    overdueDays = 9,
                    nextDueDate = "2026-11-17",
                    lastPaymentDate = "2026-11-18",
                    paymentsCount = 1,
                    status = "OVERDUE"
                )
            ),
            payments = listOf(
                ExportPaymentRowDto(
                    paymentId = 101,
                    customerId = 42,
                    customerName = "Arjun & Sons",
                    customerPhone = "9876543210",
                    cycle = "Weekly",
                    paymentDate = "2026-11-18",
                    amount = 3750.0,
                    method = "UPI",
                    createdAt = "2026-11-18 14:22:00"
                )
            ),
            pendingAndOverdueInstallments = listOf(
                ExportInstallmentRowDto(
                    customerId = 42,
                    customerName = "Arjun & Sons",
                    customerPhone = "9876543210",
                    cycle = "Weekly",
                    installmentNumber = 2,
                    dueDate = "2026-11-17",
                    amount = 2750.0,
                    paidAmount = 1000.0,
                    unpaidAmount = 1750.0,
                    status = "Overdue (Partial)",
                    daysOverdue = 9
                )
            )
        )

        val bytes = ExcelReportExporter.generateXlsxBytes(reportData)
        assertTrue(bytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes().toString(Charsets.UTF_8)
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet3.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet4.xml"))

        val workbookXml = entries.getValue("xl/workbook.xml")
        assertTrue(workbookXml.contains("Summary"))
        assertTrue(workbookXml.contains("Customers"))
        assertTrue(workbookXml.contains("Payments"))
        assertTrue(workbookXml.contains("Pending &amp; Overdue"))

        val summarySheet = entries.getValue("xl/worksheets/sheet1.xml")
        assertTrue(summarySheet.contains("Alice &amp; Co &lt;Finance&gt;"))

        val customersSheet = entries.getValue("xl/worksheets/sheet2.xml")
        assertTrue(customersSheet.contains("Arjun &amp; Sons"))
        assertTrue(customersSheet.contains("7250"))

        val paymentsSheet = entries.getValue("xl/worksheets/sheet3.xml")
        assertTrue(paymentsSheet.contains("2026-11-18"))
        assertTrue(paymentsSheet.contains("UPI"))

        val duesSheet = entries.getValue("xl/worksheets/sheet4.xml")
        assertTrue(duesSheet.contains("Overdue (Partial)"))
    }

    @Test
    fun pdfAndExcelFileNamesAndExportStateAreValid() {
        val pdfName = CustomerLedgerPdfExporter.buildFileName("Arjun / Kumar & Co.", LocalDate.of(2026, 11, 26))
        assertEquals("Credflow_Ledger_Arjun_Kumar_Co_2026-11-26.pdf", pdfName)

        val xlsxName = ExcelReportExporter.buildFileName("Weekly", LocalDate.of(2026, 11, 26))
        assertEquals("Credflow_Report_Weekly_2026-11-26.xlsx", xlsxName)

        val formatted = CustomerLedgerPdfExporter.formatCurrency(1250.5)
        assertTrue(formatted.contains("1,250.50"))

        val exportSuccess = ExportUiState.Success(fileName = xlsxName, summary = "Saved")
        assertEquals(xlsxName, exportSuccess.fileName)
    }
}
