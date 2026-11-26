package com.example.credflow

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordChangeValidationTest {
    @Test
    fun requiresCurrentPassword() {
        assertEquals(
            "Enter your current password.",
            validatePasswordChange("", "newpass", "newpass")
        )
    }

    @Test
    fun requiresSixCharacterNewPassword() {
        assertEquals(
            "New password must be at least 6 characters.",
            validatePasswordChange("current", "short", "short")
        )
    }

    @Test
    fun requiresConfirmationToMatch() {
        assertEquals(
            "New password and confirmation do not match.",
            validatePasswordChange("current", "newpass", "different")
        )
    }

    @Test
    fun rejectsNewPasswordLongerThanBcryptByteLimit() {
        val password = "😀".repeat(19)

        assertEquals(
            "New password must be no more than 72 UTF-8 bytes.",
            validatePasswordChange("current", password, password)
        )
    }

    @Test
    fun acceptsValidPasswordChange() {
        assertNull(validatePasswordChange("current", "newpass", "newpass"))
    }
}
