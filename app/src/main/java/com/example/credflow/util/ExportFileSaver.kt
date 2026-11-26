package com.example.credflow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ExportFileSaver {
    fun saveAndCreateResult(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        rowCount: Int,
        shareSubject: String,
        shareText: String,
        chooserTitle: String
    ): Result<ExportResult> {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            FileOutputStream(cacheFile).use { it.write(bytes) }

            val fileProviderUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            var finalUri: Uri = fileProviderUri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val insertedUri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )
                if (insertedUri != null) {
                    context.contentResolver.openOutputStream(insertedUri)?.use { it.write(bytes) }
                    finalUri = insertedUri
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileProviderUri)
                putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileProviderUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Result.success(
                ExportResult(
                    uri = finalUri,
                    fileName = fileName,
                    rowCount = rowCount,
                    shareIntent = Intent.createChooser(shareIntent, chooserTitle),
                    viewIntent = viewIntent
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

