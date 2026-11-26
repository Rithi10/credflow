package com.example.credflow

import com.example.credflow.data.remote.DuplicateCustomerException
import com.example.credflow.data.remote.ValidationException
import com.example.credflow.data.remote.dto.AddCustomerRequestDto
import com.example.credflow.data.remote.dto.AuthResponseDto
import com.example.credflow.ui.viewmodel.CredflowUiState
import com.example.credflow.util.PhoneUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BugFixesUnitTest {

    @Test
    fun normalizePhone_stripsFormattingAndCountryCode() {
        assertEquals("9876543210", PhoneUtils.normalizePhone("9876543210"))
        assertEquals("9876543210", PhoneUtils.normalizePhone("+91 98765-43210"))
        assertEquals("9876543210", PhoneUtils.normalizePhone("09876543210"))
        assertEquals("9876543210", PhoneUtils.normalizePhone("91 98765 43210"))
        assertEquals("", PhoneUtils.normalizePhone("   "))
        assertEquals("", PhoneUtils.normalizePhone(null))
    }

    @Test
    fun phonesMatch_detectsDuplicatesOnlyWhenNormalizedPhoneMatches() {
        assertTrue(PhoneUtils.phonesMatch("9876543210", "+91-98765-43210"))
        assertTrue(PhoneUtils.phonesMatch("08072773657", "80727 73657"))
        assertFalse(PhoneUtils.phonesMatch("9876543210", "9876543211"))
        assertFalse(PhoneUtils.phonesMatch("12345", "12345"))
    }

    @Test
    fun uiState_hasNoHardcodedProfileDefaultsAndExposesSaveAndRefreshFlags() {
        val state = CredflowUiState()
        assertEquals("", state.businessName)
        assertEquals("", state.ownerName)
        assertEquals("", state.ownerPhone)
        assertEquals("", state.ownerEmail)
        assertEquals("", state.businessAddress)
        assertEquals("", state.gstinNumber)
        assertFalse(state.isSavingCustomer)
        assertFalse(state.isRefreshingAi)
        assertNull(state.refreshError)
    }

    @Test
    fun authResponseDto_carriesBusinessAddressAndProfileFields() {
        val auth = AuthResponseDto(
            success = true,
            userId = 7,
            token = "tok_123",
            name = "Maha",
            businessName = "Maha Finance",
            message = "Updated",
            email = "maha@example.com",
            phone = "9876543210",
            gstinNumber = "33ABCDE1234F1Z5",
            businessAddress = "42 Anna Salai, Chennai"
        )
        assertEquals("42 Anna Salai, Chennai", auth.businessAddress)
        assertEquals("33ABCDE1234F1Z5", auth.gstinNumber)
        assertEquals("9876543210", auth.phone)
    }

    @Test
    fun addCustomerRequestDto_carriesIdempotencyKey() {
        val dto = AddCustomerRequestDto(
            name = "Rithi",
            phone = "9876500001",
            amount = 5000.0,
            interest = 10.0,
            interestType = "Flat Interest",
            period = 1,
            periodUnit = "Months",
            cycle = "Daily",
            collectionDay = "Daily",
            totalDue = 5500.0,
            installmentAmount = 183.33,
            totalInstallments = 30,
            firstDueDate = "2026-11-27",
            note = "",
            idempotencyKey = "idem-uuid-1234"
        )
        assertEquals("idem-uuid-1234", dto.idempotencyKey)
    }

    @Test
    fun duplicateAndValidationExceptions_preserveDetails() {
        val dup = DuplicateCustomerException("Already exists", existingCustomerId = 26)
        assertEquals("Already exists", dup.message)
        assertEquals(26, dup.existingCustomerId)

        val valErr = ValidationException("Invalid phone", statusCode = 400)
        assertEquals("Invalid phone", valErr.message)
        assertEquals(400, valErr.statusCode)
    }
}

