package com.example.credflow

import com.example.credflow.data.remote.dto.BackupCreateResponseDto
import com.example.credflow.data.remote.dto.BackupItemDto
import com.example.credflow.data.remote.dto.BackupListResponseDto
import com.example.credflow.data.remote.dto.ChangePasswordResponseDto
import com.example.credflow.data.remote.dto.RestoreResponseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDtoTest {

    @Test
    fun testBackupItemDtoProperties() {
        val item = BackupItemDto(
            filename = "credflow_backup_user_31_20261125_100000.json",
            createdAt = "2026-11-25 10:00:00",
            sizeBytes = 124500L,
            customerCount = 21,
            paymentCount = 43,
            installmentCount = 194,
            notificationCount = 13
        )

        assertEquals("credflow_backup_user_31_20261125_100000.json", item.filename)
        assertEquals("2026-11-25 10:00:00", item.createdAt)
        assertEquals(124500L, item.sizeBytes)
        assertEquals(21, item.customerCount)
        assertEquals(43, item.paymentCount)
        assertEquals(194, item.installmentCount)
        assertEquals(13, item.notificationCount)
    }

    @Test
    fun testBackupListResponseDtoProperties() {
        val item = BackupItemDto(
            filename = "credflow_backup_user_31_20261125_100000.json",
            createdAt = "2026-11-25 10:00:00",
            sizeBytes = 124500L,
            customerCount = 21,
            paymentCount = 43
        )
        val listDto = BackupListResponseDto(
            success = true,
            userId = 31,
            totalBackups = 1,
            backups = listOf(item),
            error = null
        )

        assertTrue(listDto.success)
        assertEquals(31, listDto.userId)
        assertEquals(1, listDto.totalBackups)
        assertEquals(1, listDto.backups.size)
        assertEquals("credflow_backup_user_31_20261125_100000.json", listDto.backups[0].filename)
        assertNull(listDto.error)
    }

    @Test
    fun testBackupCreateResponseDtoProperties() {
        val item = BackupItemDto(
            filename = "credflow_backup_user_31_20261125_100000.json",
            createdAt = "2026-11-25 10:00:00",
            sizeBytes = 124500L
        )
        val createDto = BackupCreateResponseDto(
            success = true,
            message = "Backup created successfully.",
            filename = item.filename,
            backup = item
        )

        assertTrue(createDto.success)
        assertEquals("Backup created successfully.", createDto.message)
        assertNotNull(createDto.backup)
        assertEquals("credflow_backup_user_31_20261125_100000.json", createDto.backup?.filename)
    }

    @Test
    fun testRestoreResponseDtoProperties() {
        val restoreDto = RestoreResponseDto(
            success = true,
            message = "Database successfully restored from backup.",
            restoredCustomers = 21,
            restoredPayments = 43,
            restoredInstallments = 194,
            restoredNotifications = 13
        )

        assertTrue(restoreDto.success)
        assertEquals(21, restoreDto.restoredCustomers)
        assertEquals(43, restoreDto.restoredPayments)
        assertEquals(194, restoreDto.restoredInstallments)
        assertEquals(13, restoreDto.restoredNotifications)
    }

    @Test
    fun testChangePasswordResponseDtoProperties() {
        val changeDto = ChangePasswordResponseDto(
            success = true,
            message = "Password changed successfully.",
            newAuthToken = "new_rotated_secure_token"
        )

        assertTrue(changeDto.success)
        assertEquals("Password changed successfully.", changeDto.message)
        assertEquals("new_rotated_secure_token", changeDto.newAuthToken)
    }
}
