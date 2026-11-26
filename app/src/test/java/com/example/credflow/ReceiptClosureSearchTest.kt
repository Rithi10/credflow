package com.example.credflow

import com.example.credflow.data.remote.dto.BusinessInfoDto
import com.example.credflow.data.remote.dto.CloseLoanResponseDto
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.domain.model.Payment
import com.example.credflow.ui.viewmodel.CredflowUiState
import com.example.credflow.util.PaymentReceiptPdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptClosureSearchTest {

    @Test
    fun paymentReceiptExporterBuildsDeterministicNameAndShareSummary() {
        val receipt = PaymentReceiptDto(
            id = 5,
            paymentId = 101,
            customerId = 42,
            receiptNumber = "CF-20261126-000101",
            customerName = "Arjun Kumar",
            customerPhone = "9876543210",
            paymentAmount = 2500.0,
            paymentDate = "2026-11-26",
            paymentMethod = "UPI",
            totalLoanAmount = 11000.0,
            totalPaidAmount = 5000.0,
            remainingBalance = 6000.0,
            business = BusinessInfoDto(businessName = "Alice Finance", ownerName = "Alice")
        )

        val fileName1 = PaymentReceiptPdfExporter.buildFileName(receipt.receiptNumber, receipt.paymentId)
        val fileName2 = PaymentReceiptPdfExporter.buildFileName(receipt.receiptNumber, receipt.paymentId)
        assertEquals("Credflow_Receipt_CF-20261126-000101.pdf", fileName1)
        assertEquals(fileName1, fileName2)

        val shareMsg = PaymentReceiptPdfExporter.buildShareMessage(receipt)
        assertTrue(shareMsg.contains("Alice Finance"))
        assertTrue(shareMsg.contains("CF-20261126-000101"))
        assertTrue(shareMsg.contains("Arjun Kumar"))
        assertTrue(shareMsg.contains("9876543210"))
        assertTrue(shareMsg.contains("2026-11-26"))
        assertTrue(shareMsg.contains("UPI"))
        assertTrue(shareMsg.contains("2,500.00"))
        assertTrue(shareMsg.contains("11,000.00"))
        assertTrue(shareMsg.contains("5,000.00"))
        assertTrue(shareMsg.contains("6,000.00"))

        val payment = Payment(
            id = 101,
            customerId = 42,
            customerName = "Arjun Kumar",
            amount = 2500.0,
            paymentDate = "2026-11-26",
            note = "UPI",
            receiptNumber = "CF-20261126-000101"
        )
        assertTrue(payment.toDisplayText().contains("CF-20261126-000101"))
    }

    @Test
    fun customerClosureBreakdownAndClosedStateInvariants() {
        val activeCustomer = Customer(
            id = 10,
            name = "Bala",
            phone = "9876500001",
            amount = 10000.0,
            interest = 10.0,
            totalDue = 11000.0,
            paid = 4000.0,
            due = 7000.0,
            totalInstallments = 4,
            installmentAmount = 2750.0,
            cycle = "Weekly",
            day = "Monday",
            loanStatus = "ACTIVE"
        )
        assertFalse(activeCustomer.isClosed)
        assertEquals(6000.0, activeCustomer.outstandingPrincipal, 0.001)
        assertEquals(1000.0, activeCustomer.outstandingInterest, 0.001)

        val closedCustomer = activeCustomer.copy(
            paid = 11000.0,
            due = 0.0,
            loanStatus = "CLOSED",
            settlementAmount = 7000.0,
            settlementType = "FULL",
            closedAt = "2026-11-26 10:00:00"
        )
        assertTrue(closedCustomer.isClosed)
        assertEquals(0.0, closedCustomer.outstandingPrincipal, 0.001)
        assertEquals(0.0, closedCustomer.outstandingInterest, 0.001)
        assertEquals(0, closedCustomer.remainingInstallments)

        val closureResponse = CloseLoanResponseDto(
            success = true,
            customerId = 10,
            loanStatus = "CLOSED",
            loanClosed = true,
            settlementType = "FULL",
            settlementAmount = 7000.0,
            paidAmount = 11000.0,
            remainingBalance = 0.0
        )
        assertTrue(closureResponse.loanClosed)
        assertEquals("CLOSED", closureResponse.loanStatus)
    }

    @Test
    fun uiStateUsesRemoteSearchResultsWhenAvailableAndFiltersLocallyOtherwise() {
        val c1 = Customer(id = 1, name = "Arjun", phone = "9876500001", amount = 1000.0, cycle = "Daily", day = "Daily")
        val c2 = Customer(id = 2, name = "Bala", phone = "9123400002", amount = 2000.0, cycle = "Weekly", day = "Monday")

        val localState = CredflowUiState(
            customers = listOf(c1, c2),
            searchQuery = "91234",
            hasRemoteSearchResults = false
        )
        assertEquals(listOf(c2), localState.filteredCustomers)

        val remoteState = localState.copy(
            searchResults = listOf(c1),
            hasRemoteSearchResults = true
        )
        assertEquals(listOf(c1), remoteState.filteredCustomers)
    }
}

