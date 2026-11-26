package com.example.credflow.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class BackupItemDto(
    val filename: String = "",
    val createdAt: String = "",
    val sizeBytes: Long = 0L,
    val customerCount: Int = 0,
    val paymentCount: Int = 0,
    val installmentCount: Int = 0,
    val notificationCount: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): BackupItemDto {
            val counts = json.optJSONObject("counts")
            val custCount = counts?.optInt("customers", 0) ?: json.optInt("customer_count", 0)
            val payCount = counts?.optInt("payments", 0) ?: json.optInt("payment_count", 0)
            val instCount = counts?.optInt("installments", 0) ?: json.optInt("installment_count", 0)
            val notifCount = counts?.optInt("notifications", 0) ?: json.optInt("notification_count", 0)

            return BackupItemDto(
                filename = json.optString("filename", ""),
                createdAt = json.optString("created_at", ""),
                sizeBytes = json.optLong("size_bytes", 0L),
                customerCount = custCount,
                paymentCount = payCount,
                installmentCount = instCount,
                notificationCount = notifCount
            )
        }
    }
}

data class BackupListResponseDto(
    val success: Boolean = false,
    val userId: Int = 0,
    val totalBackups: Int = 0,
    val backups: List<BackupItemDto> = emptyList(),
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): BackupListResponseDto {
            val list = mutableListOf<BackupItemDto>()
            val arr = json.optJSONArray("backups")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(BackupItemDto.fromJson(it)) }
                }
            }
            return BackupListResponseDto(
                success = json.optBoolean("success", true),
                userId = json.optInt("user_id", 0),
                totalBackups = json.optInt("total_backups", list.size),
                backups = list,
                error = json.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class BackupCreateResponseDto(
    val success: Boolean = false,
    val message: String = "",
    val filename: String = "",
    val backup: BackupItemDto? = null,
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): BackupCreateResponseDto {
            val backupObj = json.optJSONObject("backup")
            val item = if (backupObj != null) {
                BackupItemDto.fromJson(backupObj)
            } else if (json.has("filename")) {
                BackupItemDto(
                    filename = json.optString("filename", ""),
                    createdAt = json.optString("created_at", ""),
                    sizeBytes = json.optLong("size_bytes", 0L)
                )
            } else null

            return BackupCreateResponseDto(
                success = json.optBoolean("success", true),
                message = json.optString("message", "Backup created successfully."),
                filename = json.optString("filename", item?.filename ?: ""),
                backup = item,
                error = json.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class RestoreResponseDto(
    val success: Boolean = false,
    val message: String = "",
    val restoredCustomers: Int = 0,
    val restoredPayments: Int = 0,
    val restoredInstallments: Int = 0,
    val restoredNotifications: Int = 0,
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): RestoreResponseDto {
            val counts = json.optJSONObject("restored_counts")
            return RestoreResponseDto(
                success = json.optBoolean("success", true),
                message = json.optString("message", "Database successfully restored from backup."),
                restoredCustomers = counts?.optInt("customers", 0) ?: 0,
                restoredPayments = counts?.optInt("payments", 0) ?: 0,
                restoredInstallments = counts?.optInt("installments", 0) ?: 0,
                restoredNotifications = counts?.optInt("notifications", 0) ?: 0,
                error = json.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class ChangePasswordResponseDto(
    val success: Boolean = false,
    val message: String = "",
    val newAuthToken: String? = null,
    val error: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject): ChangePasswordResponseDto {
            val user = json.optJSONObject("user")
            return ChangePasswordResponseDto(
                success = json.optBoolean("success", true),
                message = json.optString("message", ""),
                newAuthToken = user?.optString("auth_token")?.takeIf { it.isNotBlank() },
                error = json.optString("error").takeIf { it.isNotBlank() }
            )
        }
    }
}
