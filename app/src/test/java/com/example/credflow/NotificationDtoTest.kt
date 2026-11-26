package com.example.credflow

import com.example.credflow.data.remote.dto.NotificationItemDto
import com.example.credflow.data.remote.dto.NotificationsResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDtoTest {

    @Test
    fun dueReminderNotificationProperties() {
        val item = NotificationItemDto(
            id = 1,
            userId = 31,
            customerId = 14,
            customerName = "rithi",
            customerPhone = "8072773657",
            customerCycle = "Daily",
            type = "DUE_REMINDER",
            title = "Installment Due Today: rithi",
            message = "Installment of ₹142.67 for rithi (Daily cycle) is due Today (2026-11-25).",
            dueDate = "2026-11-25",
            amount = 142.67,
            overdueDays = 0,
            remainingBalance = 4280.0,
            isRead = false,
            createdAt = "2026-11-25 13:50:00"
        )

        assertEquals(1, item.id)
        assertEquals(31, item.userId)
        assertEquals(14, item.customerId)
        assertEquals("rithi", item.customerName)
        assertEquals("Daily", item.customerCycle)
        assertEquals("DUE_REMINDER", item.type)
        assertEquals(142.67, item.amount, 0.001)
        assertEquals(4280.0, item.remainingBalance, 0.001)
        assertEquals(0, item.overdueDays)
        assertFalse(item.isRead)
    }

    @Test
    fun overdueAlertNotificationProperties() {
        val alert = NotificationItemDto(
            id = 2,
            userId = 31,
            customerId = 14,
            customerName = "rithi",
            customerPhone = "8072773657",
            customerCycle = "Daily",
            type = "OVERDUE_ALERT",
            title = "Overdue Alert: rithi",
            message = "rithi's installment of ₹142.67 is overdue by 1 day(s) (Due: 2026-11-24).",
            dueDate = "2026-11-24",
            amount = 142.67,
            overdueDays = 1,
            remainingBalance = 4280.0,
            isRead = false,
            createdAt = "2026-11-25 13:50:00"
        )

        assertEquals("OVERDUE_ALERT", alert.type)
        assertEquals(1, alert.overdueDays)
        assertEquals("2026-11-24", alert.dueDate)
        assertFalse(alert.isRead)
    }

    @Test
    fun paymentReceivedNotificationProperties() {
        val payNotification = NotificationItemDto(
            id = 3,
            userId = 31,
            customerId = 24,
            customerName = "karthi",
            customerPhone = "9632587136",
            customerCycle = "Monthly",
            type = "PAYMENT_RECEIVED",
            title = "Payment Received: karthi",
            message = "karthi paid ₹3,533.00 on 2026-11-25. Remaining balance: ₹38,867.00.",
            dueDate = "2026-11-25",
            amount = 3533.0,
            overdueDays = 0,
            remainingBalance = 38867.0,
            isRead = true,
            createdAt = "2026-11-25 13:50:00"
        )

        assertEquals("PAYMENT_RECEIVED", payNotification.type)
        assertEquals(3533.0, payNotification.amount, 0.001)
        assertEquals(38867.0, payNotification.remainingBalance, 0.001)
        assertTrue(payNotification.isRead)
    }

    @Test
    fun notificationsResponseDtoInvariants() {
        val items = listOf(
            NotificationItemDto(id = 1, type = "DUE_REMINDER", isRead = false),
            NotificationItemDto(id = 2, type = "OVERDUE_ALERT", isRead = false),
            NotificationItemDto(id = 3, type = "PAYMENT_RECEIVED", isRead = true)
        )

        val response = NotificationsResponseDto(
            success = true,
            userId = 31,
            filter = "all",
            totalCount = 3,
            unreadCount = 2,
            notifications = items
        )

        assertTrue(response.success)
        assertEquals(31, response.userId)
        assertEquals(3, response.totalCount)
        assertEquals(2, response.unreadCount)
        assertEquals(3, response.notifications.size)
        assertEquals("DUE_REMINDER", response.notifications[0].type)
        assertEquals("OVERDUE_ALERT", response.notifications[1].type)
        assertEquals("PAYMENT_RECEIVED", response.notifications[2].type)
    }
}

