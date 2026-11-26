package com.example.credflow

import com.example.credflow.data.remote.ApiConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInConfigTest {

    @Test
    fun webClientId_isValidWebOAuthClientAndMatchesBackend() {
        val clientId = ApiConfig.GOOGLE_WEB_CLIENT_ID
        assertTrue(clientId.isNotBlank())
        assertTrue(clientId.endsWith(".apps.googleusercontent.com"))
        assertFalse(clientId.contains("YOUR_"))
        assertFalse(clientId.contains("PASTE_"))
        assertEquals(
            "930439631272-nt9uc5fe4cvks0utc7off579ri6l8sj3.apps.googleusercontent.com",
            clientId
        )
    }

    @Test
    fun applicationId_matchesExpectedPackageName() {
        assertEquals("com.example.credflow", BuildConfig.APPLICATION_ID)
    }
}

