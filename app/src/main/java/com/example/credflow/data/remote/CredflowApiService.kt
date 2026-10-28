package com.example.credflow.data.remote

import com.example.credflow.data.remote.dto.AddCustomerRequestDto
import com.example.credflow.data.remote.dto.AuthResponseDto
import com.example.credflow.data.remote.dto.CustomerDto
import com.example.credflow.data.remote.dto.PaymentDto
import com.example.credflow.data.remote.dto.RecordPaymentRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CredflowApiService {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("test.php"))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(IOException("Server error HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomers(authToken: String? = null): Result<List<CustomerDto>> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("get_customers.php"))
                .get()

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $responseBody"))
                }

                val customers = mutableListOf<CustomerDto>()
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    customers.add(CustomerDto.fromJson(obj))
                }
                Result.success(customers)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addCustomer(dto: AddCustomerRequestDto, authToken: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val body = dto.toJson().toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("add_customer.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(err))
                }

                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    val newId = json.optInt("customer_id", 0)
                    Result.success(newId)
                } else {
                    val errMsg = json.optString("error", "Failed to add customer")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(dto: RecordPaymentRequestDto, authToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = dto.toJson().toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("record_payment.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "HTTP ${response.code}"
                    return@withContext Result.failure(IOException(err))
                }

                val json = JSONObject(responseBody)
                if (json.optBoolean("success", false)) {
                    Result.success(Unit)
                } else {
                    val errMsg = json.optString("error", "Failed to record payment")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPayments(customerId: Int = 0, authToken: String? = null): Result<List<PaymentDto>> = withContext(Dispatchers.IO) {
        try {
            val path = if (customerId > 0) "get_payments.php?customer_id=$customerId" else "get_payments.php"
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint(path))
                .get()

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $responseBody"))
                }

                val payments = mutableListOf<PaymentDto>()
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    payments.add(PaymentDto.fromJson(obj))
                }
                Result.success(payments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, businessName: String, identity: String, password: String): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("name", name)
                put("business_name", businessName)
                put("identity", identity)
                put("password", password)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("register.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", name),
                            businessName = json.optString("business_name", businessName),
                            message = json.optString("message", "Registered successfully")
                        )
                    )
                } else {
                    val errMsg = json.optString("error", "Registration failed")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identity: String, password: String): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("identity", identity)
                put("password", password)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("login.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", ""),
                            businessName = json.optString("business_name", ""),
                            message = json.optString("message", "Login successful")
                        )
                    )
                } else {
                    val errMsg = json.optString("error", "Login failed")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun googleAuth(idToken: String): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("id_token", idToken)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("google_auth.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", "Google User"),
                            businessName = json.optString("business_name", "Credflow Finance"),
                            message = json.optString("message", "Google authentication successful")
                        )
                    )
                } else {
                    val errMsg = json.optString("error", "Google authentication failed")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateSession(authToken: String): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("validate_session.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = authToken,
                            name = json.optString("name", "User"),
                            businessName = json.optString("business_name", "Credflow Finance"),
                            message = json.optString("message", "Session valid")
                        )
                    )
                } else {
                    Result.failure(IOException("Session expired or invalid"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(authToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (authToken.isNullOrBlank()) return@withContext Result.success(Unit)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("logout.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.success(Unit) // Local logout can proceed regardless of network state
        }
    }

    private fun parseErrorMessage(responseBody: String): String? {
        return try {
            val obj = JSONObject(responseBody)
            obj.optString("error").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

