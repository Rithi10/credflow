package com.example.credflow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

suspend fun testApiConnection(): String =
    withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConfig.BASE_URL}test.php")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                connection.inputStream.bufferedReader().use {
                    it.readText()
                }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            "Connection failed: ${e.message}"
        }
    }