package com.example.credflow

import com.example.credflow.data.remote.dto.ChatResponseDto
import com.example.credflow.domain.model.Customer
import com.example.credflow.util.OfflineChatHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatbotCommandTest {

    private fun sampleCustomers(): List<Customer> = listOf(
        Customer(
            id = 1,
            name = "Arjun",
            phone = "9876501001",
            amount = 5000.0,
            installmentAmount = 500.0,
            cycle = "Weekly",
            day = "Monday",
            totalDue = 5500.0,
            paid = 500.0,
            due = 5000.0,
            loanStatus = "ACTIVE"
        ),
        Customer(
            id = 2,
            name = "Charan",
            phone = "9876501002",
            amount = 6000.0,
            installmentAmount = 600.0,
            cycle = "Weekly",
            day = "Tuesday",
            totalDue = 6600.0,
            paid = 600.0,
            due = 6000.0,
            loanStatus = "ACTIVE"
        ),
        Customer(
            id = 3,
            name = "BlankDayLegacy",
            phone = "9876501003",
            amount = 3000.0,
            installmentAmount = 300.0,
            cycle = "Weekly",
            day = "",
            totalDue = 3300.0,
            paid = 0.0,
            due = 3300.0,
            loanStatus = "ACTIVE"
        ),
        Customer(
            id = 4,
            name = "ClosedTuesday",
            phone = "9876501004",
            amount = 2000.0,
            installmentAmount = 200.0,
            cycle = "Weekly",
            day = "Tuesday",
            totalDue = 2000.0,
            paid = 1500.0,
            due = 500.0,
            loanStatus = "CLOSED"
        )
    )

    @Test
    fun offlineFallback_tuesdayListStrictlyExcludesMondayBlankAndClosedCustomers() {
        val customers = sampleCustomers()
        val tuesdayOnly = OfflineChatHelper.filterActiveWeekdayCustomers(customers, "Tuesday")

        assertEquals(1, tuesdayOnly.size)
        assertEquals("Charan", tuesdayOnly.first().name)

        val reply = OfflineChatHelper.computeFallbackReply(
            question = "Tuesday customers list kaattu",
            customers = customers,
            payments = emptyList()
        )
        assertTrue(reply.contains("Charan"))
        assertFalse(reply.contains("Arjun"))
        assertFalse(reply.contains("BlankDayLegacy"))
        assertFalse(reply.contains("ClosedTuesday"))
    }

    @Test
    fun offlineFallback_rejectsDatabaseMutationsSafely() {
        assertTrue(OfflineChatHelper.isOfflineMutationCommand("Monday customers-ah Tuesday list-la add pannu"))
        assertTrue(OfflineChatHelper.isOfflineMutationCommand("Record payment 500 for Arjun"))
        assertTrue(OfflineChatHelper.isOfflineMutationCommand("Add customer Ravi 9876543210 amount 5000"))
        assertFalse(OfflineChatHelper.isOfflineMutationCommand("Tuesday customers list kaattu"))

        val reply = OfflineChatHelper.computeFallbackReply(
            question = "Monday customers-ah Tuesday list-la add pannu",
            customers = sampleCustomers(),
            payments = emptyList()
        )
        assertTrue(reply.contains("offline", ignoreCase = true))
    }

    @Test
    fun offlineFallback_extractsTypoAndTanglishWeekdaysStrictly() {
        assertEquals("Tuesday", OfflineChatHelper.extractWeekdayStrict("tuesdy custmers list"))
        assertEquals("Monday", OfflineChatHelper.extractWeekdayStrict("Thingal customers yaaru?"))
        assertEquals("Sunday", OfflineChatHelper.extractWeekdayStrict("Nyiru list"))
    }

    @Test
    fun chatResponseDto_exposesDataChangedFlagCorrectly() {
        val unchanged = ChatResponseDto(status = "ok", intent = "weekday_customers")
        val changed = ChatResponseDto(status = "ok", intent = "move_weekday", dataChanged = true)

        assertFalse(unchanged.dataChanged)
        assertTrue(changed.dataChanged)
    }

    @Test
    fun apiConfig_keepsPhpAndAiUrlsSeparateWithSameReachableLanIp() {
        val originalUrl = com.example.credflow.data.remote.ApiConfig.baseUrl
        try {
            com.example.credflow.data.remote.ApiConfig.baseUrl =
                com.example.credflow.data.remote.ApiConfig.DEFAULT_BASE_URL

            val phpUrl = com.example.credflow.data.remote.ApiConfig.baseUrl
            val aiUrl = com.example.credflow.data.remote.ApiConfig.aiBaseUrl

            assertTrue(phpUrl.contains(com.example.credflow.data.remote.ApiConfig.PC_LAN_IP))
            assertTrue(aiUrl.contains("${com.example.credflow.data.remote.ApiConfig.PC_LAN_IP}:8000"))
            assertFalse(aiUrl.contains("127.0.0.1:8000"))
            assertFalse(aiUrl.contains("localhost:8000"))
            assertTrue(phpUrl.endsWith("/credflow_api/"))
            assertTrue(aiUrl.endsWith(":8000/"))

            // Updating server IP derives matching AI URL host automatically
            com.example.credflow.data.remote.ApiConfig.baseUrl = "http://192.168.1.50/credflow_api/"
            assertEquals("http://192.168.1.50:8000/", com.example.credflow.data.remote.ApiConfig.aiBaseUrl)
            assertEquals("http://192.168.1.50:8000/health", com.example.credflow.data.remote.ApiConfig.getAiEndpoint("health"))
            assertEquals("http://192.168.1.50/credflow_api/chat.php", com.example.credflow.data.remote.ApiConfig.getEndpoint("chat.php"))
            assertEquals("http://192.168.1.50/credflow_api/predict_repayment.php", com.example.credflow.data.remote.ApiConfig.getEndpoint("predict_repayment.php"))
        } finally {
            com.example.credflow.data.remote.ApiConfig.baseUrl = originalUrl
        }
    }
}

