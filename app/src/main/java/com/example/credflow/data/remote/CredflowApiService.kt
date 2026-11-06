package com.example.credflow.data.remote

import com.example.credflow.data.remote.dto.AddCustomerRequestDto
import com.example.credflow.data.remote.dto.AuthResponseDto
import com.example.credflow.data.remote.dto.CustomerDto
import com.example.credflow.data.remote.dto.PaymentDto
import com.example.credflow.data.remote.dto.RecordPaymentRequestDto
import com.example.credflow.data.remote.dto.RecordPaymentResponseDto
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

    suspend fun recordPayment(dto: RecordPaymentRequestDto, authToken: String? = null): Result<RecordPaymentResponseDto> = withContext(Dispatchers.IO) {
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
                    Result.success(RecordPaymentResponseDto.fromJson(json))
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

    suspend fun getInstallments(customerId: Int, authToken: String? = null): Result<List<com.example.credflow.domain.model.InstallmentScheduleItem>> = withContext(Dispatchers.IO) {
        try {
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("get_installments.php?customer_id=$customerId"))
                .get()

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $responseBody"))
                }

                val list = mutableListOf<com.example.credflow.domain.model.InstallmentScheduleItem>()
                val jsonArray = JSONArray(responseBody)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        com.example.credflow.domain.model.InstallmentScheduleItem(
                            number = obj.optInt("installment_number", i + 1),
                            dueDate = obj.optString("due_date", ""),
                            amount = obj.optDouble("amount", 0.0),
                            paidAmount = obj.optDouble("paid_amount", 0.0),
                            status = obj.optString("status", "Pending")
                        )
                    )
                }
                Result.success(list)
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
                            phone = json.optString("phone", ""),
                            email = json.optString("email", ""),
                            address = json.optString("business_address", ""),
                            gstin = json.optString("gstin_number", ""),
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
                            name = json.optString("owner_name", json.optString("name", "")),
                            businessName = json.optString("business_name", ""),
                            phone = json.optString("phone", ""),
                            email = json.optString("email", ""),
                            address = json.optString("business_address", ""),
                            gstin = json.optString("gstin_number", ""),
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
                            name = json.optString("owner_name", json.optString("name", "")),
                            businessName = json.optString("business_name", ""),
                            phone = json.optString("phone", ""),
                            email = json.optString("email", ""),
                            address = json.optString("business_address", ""),
                            gstin = json.optString("gstin_number", ""),
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
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = authToken,
                            name = json.optString("owner_name", json.optString("name", "")),
                            businessName = json.optString("business_name", ""),
                            phone = json.optString("phone", ""),
                            email = json.optString("email", ""),
                            address = json.optString("business_address", json.optString("address", "")),
                            gstin = json.optString("gstin_number", json.optString("gstin", "")),
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

    suspend fun updateProfile(
        authToken: String,
        name: String,
        businessName: String,
        phone: String,
        email: String,
        address: String,
        gstin: String
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("owner_name", name.trim())
                put("business_name", businessName.trim())
                put("phone", phone.trim())
                put("email", email.trim())
                put("business_address", address.trim())
                put("gstin_number", gstin.trim())
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("update_profile.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = authToken,
                            name = json.optString("owner_name", json.optString("name", name)),
                            businessName = json.optString("business_name", businessName),
                            phone = json.optString("phone", phone),
                            email = json.optString("email", email),
                            address = json.optString("business_address", json.optString("address", address)),
                            gstin = json.optString("gstin_number", json.optString("gstin", gstin)),
                            message = json.optString("message", "Profile updated successfully")
                        )
                    )
                } else {
                    val errMsg = json.optString("error", "Failed to update profile (HTTP ${response.code})")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        authToken: String?,
        currentPassword: String,
        newPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (authToken.isNullOrBlank()) {
            return@withContext Result.failure(IOException("Please log in again to change your password."))
        }

        try {
            val payload = JSONObject().apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
            }
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("change_password.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try {
                    JSONObject(responseBody)
                } catch (_: Exception) {
                    JSONObject()
                }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        json.optString("message", "Password updated successfully.")
                    )
                } else {
                    val error = json.optString(
                        "error",
                        "Could not update the password (HTTP ${response.code})."
                    )
                    Result.failure(IOException(error))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetServerAccountData(authToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("reset_account_data.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "All server collection records have been reset."))
                } else {
                    val errMsg = json.optString("error", "Failed to reset server data (HTTP ${response.code})")
                    Result.failure(IOException(errMsg))
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

    suspend fun requestPasswordResetOtp(identity: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("identity", identity.trim())
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("send_reset_otp.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Verification code sent."))
                } else {
                    val errMsg = json.optString("error", "Failed to send verification code (HTTP ${response.code})")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPasswordResetOtp(identity: String, otp: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("identity", identity.trim())
                put("otp", otp.trim())
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("verify_reset_otp.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    val resetToken = json.optString("reset_token", "")
                    if (resetToken.isNotBlank()) {
                        Result.success(resetToken)
                    } else {
                        Result.failure(IOException("Server did not return a valid reset token."))
                    }
                } else {
                    val errMsg = json.optString("error", "Invalid verification code.")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(resetToken: String, newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("reset_token", resetToken.trim())
                put("new_password", newPassword)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("reset_password.php"))
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(json.optString("message", "Password updated successfully."))
                } else {
                    val errMsg = json.optString("error", "Failed to reset password.")
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun predictRepaymentProbability(
        amount: Double,
        installment: Double,
        missedPayments: Int,
        paymentRatio: Double,
        overdueDays: Int
    ): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("amount", amount)
                put("installment", installment)
                put("missed_payments", missedPayments)
                put("payment_ratio", paymentRatio)
                put("overdue_days", overdueDays)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val url = ApiConfig.getAiEndpoint("predict")
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.has("repayment_probability")) {
                    val probability = json.optDouble("repayment_probability", 0.0)
                    Result.success(probability)
                } else {
                    val errMsg = json.optString("detail", json.optString("error", "FastAPI error (HTTP ${response.code})"))
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
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
