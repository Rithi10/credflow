package com.example.credflow.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class NotificationItemDto(
    val id: Int = 0,
    val userId: Int = 0,
    val customerId: Int? = null,
    val customerName: String = "Customer",
    val customerPhone: String = "",
    val customerCycle: String = "Daily",
    val type: String = "DUE_REMINDER", // "DUE_REMINDER", "OVERDUE_ALERT", "PAYMENT_RECEIVED"
    val title: String = "",
    val message: String = "",
    val dueDate: String? = null,
    val amount: Double = 0.0,
    val overdueDays: Int = 0,
    val remainingBalance: Double = 0.0,
    val isRead: Boolean = false,
    val createdAt: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): NotificationItemDto {
            return NotificationItemDto(
                id = json.optInt("id", 0),
                userId = json.optInt("user_id", 0),
                customerId = if (json.isNull("customer_id")) null else json.optInt("customer_id", 0).takeIf { it > 0 },
                customerName = json.optString("customer_name", "Customer"),
                customerPhone = json.optString("customer_phone", ""),
                customerCycle = json.optString("customer_cycle", "Daily"),
                type = json.optString("type", "DUE_REMINDER"),
                title = json.optString("title", ""),
                message = json.optString("message", ""),
                dueDate = json.optString("due_date").takeIf { it.isNotBlank() && it != "null" },
                amount = json.optDouble("amount", 0.0),
                overdueDays = json.optInt("overdue_days", 0),
                remainingBalance = json.optDouble("remaining_balance", 0.0),
                isRead = json.optBoolean("is_read", false),
                createdAt = json.optString("created_at", "")
            )
        }
    }
}

data class NotificationsResponseDto(
    val success: Boolean = false,
    val userId: Int = 0,
    val filter: String = "all",
    val totalCount: Int = 0,
    val unreadCount: Int = 0,
    val notifications: List<NotificationItemDto> = emptyList(),
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): NotificationsResponseDto {
            val list = mutableListOf<NotificationItemDto>()
            val arr = json.optJSONArray("notifications")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(NotificationItemDto.fromJson(it)) }
                }
            }
            return NotificationsResponseDto(
                success = json.optBoolean("success", true),
                userId = json.optInt("user_id", 0),
                filter = json.optString("filter", "all"),
                totalCount = json.optInt("total_count", list.size),
                unreadCount = json.optInt("unread_count", 0),
                notifications = list,
                error = json.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }
}

