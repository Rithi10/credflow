package com.example.credflow.data.remote

import com.example.credflow.data.remote.dto.AddCustomerRequestDto
import com.example.credflow.data.remote.dto.AuthResponseDto
import com.example.credflow.data.remote.dto.ChatResponseDto
import com.example.credflow.data.remote.dto.CollectionForecastDto
import com.example.credflow.data.remote.dto.CustomerDto
import com.example.credflow.data.remote.dto.PaymentDto
import com.example.credflow.data.remote.dto.RecordPaymentRequestDto
import com.example.credflow.data.remote.dto.RecordPaymentResponseDto
import com.example.credflow.data.remote.dto.RepaymentPredictionDto
import com.example.credflow.data.remote.dto.ReportsDto
import com.example.credflow.data.remote.dto.NotificationsResponseDto
import com.example.credflow.data.remote.dto.ChangePasswordResponseDto
import com.example.credflow.data.remote.dto.CloseLoanRequestDto
import com.example.credflow.data.remote.dto.CloseLoanResponseDto
import com.example.credflow.data.remote.dto.CustomerLedgerDto
import com.example.credflow.data.remote.dto.ExportReportDataDto
import com.example.credflow.data.remote.dto.PaymentReceiptDto
import com.example.credflow.data.remote.dto.PaymentReceiptResponseDto
import com.example.credflow.data.remote.dto.SearchCustomersResponseDto
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

class SessionExpiredException(message: String, val statusCode: Int = 401) : IOException(message)
class DuplicateCustomerException(message: String, val existingCustomerId: Int = 0) : IOException(message)
class ValidationException(message: String, val statusCode: Int = 400) : IOException(message)

class CredflowApiService {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
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
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
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
                    val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                    val err = json.optString("error").takeIf { it.isNotBlank() }
                        ?: parseErrorMessage(responseBody)
                        ?: "HTTP ${response.code}"
                    return@withContext when (response.code) {
                        401, 403 -> Result.failure(SessionExpiredException(err, response.code))
                        409 -> Result.failure(DuplicateCustomerException(err, json.optInt("existing_customer_id", 0)))
                        400, 422 -> Result.failure(ValidationException(err, response.code))
                        else -> Result.failure(IOException(err))
                    }
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

    suspend fun checkAiHealth(): Result<String> = withContext(Dispatchers.IO) {
        val aiHealthUrl = ApiConfig.getAiEndpoint("health")
        try {
            val request = Request.Builder()
                .url(aiHealthUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()?.trim() ?: ""
                if (response.isSuccessful) {
                    Result.success(body)
                } else {
                    Result.failure(IOException("AI service at $aiHealthUrl returned HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e, aiHealthUrl), e))
        }
    }

    fun getFriendlyErrorMessage(e: Exception, targetUrl: String = ApiConfig.baseUrl): String {
        val aiUrl = ApiConfig.aiBaseUrl
        return when (e) {
            is java.net.ConnectException ->
                "Cannot connect to server at $targetUrl (AI service: $aiUrl). Ensure Apache and FastAPI (port ${ApiConfig.AI_SERVICE_PORT}) are running and your device is on the same Wi-Fi."
            is java.net.SocketTimeoutException ->
                "Connection timed out reaching $targetUrl (AI service: $aiUrl). Check your Wi-Fi network, server load, and firewall."
            is java.net.UnknownHostException ->
                "Server host ($targetUrl) could not be resolved. Verify your network connection and PC LAN IP (${ApiConfig.PC_LAN_IP})."
            is org.json.JSONException -> "Unexpected response format from server ($targetUrl)."
            else -> e.message ?: "Unable to reach server ($targetUrl)."
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
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", name),
                            businessName = json.optString("business_name", businessName),
                            message = json.optString("message", "Registered successfully"),
                            email = json.optString("email", ""),
                            phone = json.optString("phone", ""),
                            gstinNumber = json.optString("gstin_number", ""),
                            businessAddress = json.optString("business_address", "")
                        )
                    )
                } else {
                    val errMsg = json.optString("error").takeIf { it.isNotBlank() }
                        ?: "Registration failed (HTTP ${response.code})"
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
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
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", ""),
                            businessName = json.optString("business_name", ""),
                            message = json.optString("message", "Login successful"),
                            email = json.optString("email", ""),
                            phone = json.optString("phone", ""),
                            gstinNumber = json.optString("gstin_number", ""),
                            businessAddress = json.optString("business_address", "")
                        )
                    )
                } else {
                    val errMsg = json.optString("error").takeIf { it.isNotBlank() }
                        ?: "Login failed (HTTP ${response.code})"
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
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
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = json.optString("token", ""),
                            name = json.optString("name", "Google User"),
                            businessName = json.optString("business_name", "Credflow Finance"),
                            message = json.optString("message", "Google authentication successful"),
                            email = json.optString("email", ""),
                            phone = json.optString("phone", ""),
                            gstinNumber = json.optString("gstin_number", ""),
                            businessAddress = json.optString("business_address", "")
                        )
                    )
                } else {
                    val errMsg = json.optString("error").takeIf { it.isNotBlank() }
                        ?: "Google authentication failed (HTTP ${response.code})"
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
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
                            name = json.optString("name", "User"),
                            businessName = json.optString("business_name", "Credflow Finance"),
                            message = json.optString("message", "Session valid"),
                            email = json.optString("email", ""),
                            phone = json.optString("phone", ""),
                            gstinNumber = json.optString("gstin_number", ""),
                            businessAddress = json.optString("business_address", "")
                        )
                    )
                } else {
                    val errMsg = json.optString("error").takeIf { it.isNotBlank() } ?: "Session expired or invalid"
                    if (response.code == 401 || response.code == 403) {
                        Result.failure(SessionExpiredException(errMsg, response.code))
                    } else {
                        Result.failure(IOException("Server error (HTTP ${response.code})"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun updateProfile(
        businessName: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String,
        businessAddress: String,
        gstinNumber: String,
        authToken: String?
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("business_name", businessName.trim())
                put("owner_name", ownerName.trim())
                put("name", ownerName.trim())
                put("phone", ownerPhone.trim())
                put("email", ownerEmail.trim())
                put("business_address", businessAddress.trim())
                put("address", businessAddress.trim())
                put("gstin_number", gstinNumber.trim())
                put("gstin", gstinNumber.trim())
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("update_profile.php"))
                .post(body)
            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }
            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val json = try { JSONObject(responseBody) } catch (_: Exception) { JSONObject() }
                if (response.code == 401 || response.code == 403) {
                    val err = json.optString("error").takeIf { it.isNotBlank() } ?: "Session expired."
                    return@withContext Result.failure(SessionExpiredException(err, response.code))
                }
                if (response.isSuccessful && json.optBoolean("success", false)) {
                    Result.success(
                        AuthResponseDto(
                            success = true,
                            userId = json.optInt("user_id", 0),
                            token = authToken ?: "",
                            name = json.optString("owner_name", json.optString("name", ownerName.trim())),
                            businessName = json.optString("business_name", businessName.trim()),
                            message = json.optString("message", "Profile saved successfully"),
                            email = json.optString("email", ownerEmail.trim()),
                            phone = json.optString("phone", ownerPhone.trim()),
                            gstinNumber = json.optString("gstin_number", gstinNumber.trim()),
                            businessAddress = json.optString("business_address", businessAddress.trim())
                        )
                    )
                } else {
                    val errMsg = json.optString("error").takeIf { it.isNotBlank() }
                        ?: parseErrorMessage(responseBody)
                        ?: "Failed to update profile (HTTP ${response.code})"
                    Result.failure(IOException(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
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

    private val logoutClient = client.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun logout(authToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (authToken.isNullOrBlank()) return@withContext Result.success(Unit)
            val request = Request.Builder()
                .url(ApiConfig.getEndpoint("logout.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            logoutClient.newCall(request).execute().use {
                Result.success(Unit)
            }
        } catch (_: Exception) {
            Result.success(Unit) // Local logout can proceed regardless of network state
        }
    }

    suspend fun predictRepayment(authToken: String? = null): Result<RepaymentPredictionDto> = withContext(Dispatchers.IO) {
        val endpointUrl = ApiConfig.getEndpoint("predict_repayment.php")
        val aiUrl = ApiConfig.aiBaseUrl
        try {
            val body = "{}".toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(endpointUrl)
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                if (response.code == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return@withContext Result.failure(SessionExpiredException(err, response.code))
                }
                if (!response.isSuccessful) {
                    val rawErr = parseErrorMessage(responseBody) ?: "Server error HTTP ${response.code}"
                    val friendlyErr = if (response.code >= 500) {
                        "Repayment prediction AI service is unreachable via $endpointUrl (AI service: $aiUrl). Details: $rawErr"
                    } else {
                        rawErr
                    }
                    return@withContext Result.failure(IOException(friendlyErr))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid response format from repayment prediction service at $endpointUrl."))
                }

                Result.success(RepaymentPredictionDto.fromJson(json))
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e, endpointUrl), e))
        }
    }

    suspend fun askChatbot(
        question: String,
        authToken: String? = null,
        lastCustomerId: Int? = null
    ): Result<ChatResponseDto> = withContext(Dispatchers.IO) {
        val cleanQ = question.trim()
        val endpointUrl = ApiConfig.getEndpoint("chat.php")
        val aiUrl = ApiConfig.aiBaseUrl
        android.util.Log.d("CredflowChat", "[ApiService] Outgoing Query: '$cleanQ' | PHP Target: $endpointUrl | AI Service: $aiUrl")
        try {
            val jsonPayload = JSONObject().apply {
                put("question", cleanQ)
                if (lastCustomerId != null && lastCustomerId > 0) {
                    put("context", JSONObject().apply {
                        put("last_customer_id", lastCustomerId)
                    })
                }
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(endpointUrl)
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""
                android.util.Log.d("CredflowChat", "[ApiService] Response HTTP Status: $statusCode")

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    android.util.Log.e("CredflowChat", "[ApiService] 401 Unauthorized: $err")
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val rawErr = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    val friendlyErr = if (statusCode >= 500) {
                        "Chatbot AI service is unreachable via $endpointUrl (AI service: $aiUrl). Details: $rawErr"
                    } else {
                        rawErr
                    }
                    android.util.Log.e("CredflowChat", "[ApiService] HTTP $statusCode Error: $friendlyErr")
                    return@withContext Result.failure(IOException(friendlyErr))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    android.util.Log.e("CredflowChat", "[ApiService] Invalid JSON payload from chat.php: $responseBody", e)
                    return@withContext Result.failure(IOException("Invalid response format from chatbot service at $endpointUrl."))
                }

                val dto = ChatResponseDto.fromJson(json)
                if (dto.error != null) {
                    android.util.Log.e("CredflowChat", "[ApiService] Chatbot Error Payload: ${dto.error}")
                    Result.failure(IOException(dto.error))
                } else {
                    android.util.Log.d(
                        "CredflowChat",
                        "[ApiService] Live Success | Intent=${dto.intent} | Lang=${dto.language} | Source=${dto.source} | Live=true"
                    )
                    Result.success(dto)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CredflowChat", "[ApiService] Network/IO Exception for query '$cleanQ': ${e.message}", e)
            Result.failure(IOException(getFriendlyErrorMessage(e, endpointUrl), e))
        }
    }

    suspend fun getCollectionForecast(
        period: String = "all",
        authToken: String? = null
    ): Result<CollectionForecastDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("period", period)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("forecast_collections.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid response format from forecast service."))
                }

                val dto = CollectionForecastDto.fromJson(json)
                if (dto.error != null && !dto.isSuccess) {
                    Result.failure(IOException(dto.error))
                } else {
                    Result.success(dto)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun getReports(
        rangePreset: String = "all",
        startDate: String? = null,
        endDate: String? = null,
        authToken: String? = null
    ): Result<ReportsDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("range_preset", rangePreset)
                if (!startDate.isNullOrBlank()) put("start_date", startDate)
                if (!endDate.isNullOrBlank()) put("end_date", endDate)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("reports.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid response format from reports service."))
                }

                val dto = ReportsDto.fromJson(json)
                if (dto.error != null && !dto.isSuccess) {
                    Result.failure(IOException(dto.error))
                } else {
                    Result.success(dto)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun getNotifications(
        filter: String = "all",
        limit: Int = 100,
        authToken: String? = null
    ): Result<NotificationsResponseDto> = withContext(Dispatchers.IO) {
        try {
            val url = ApiConfig.getEndpoint("get_notifications.php?filter=$filter&limit=$limit")
            val reqBuilder = Request.Builder().url(url).get()
            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid response format from notifications service."))
                }

                val dto = NotificationsResponseDto.fromJson(json)
                if (dto.error != null && !dto.success) {
                    Result.failure(IOException(dto.error))
                } else {
                    Result.success(dto)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun markNotificationAsRead(
        notificationId: Int? = null,
        markAll: Boolean = false,
        authToken: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                if (markAll) {
                    put("mark_all", true)
                } else if (notificationId != null) {
                    put("notification_id", notificationId)
                }
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("mark_notification_read.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired."
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = JSONObject(responseBody)
                val updatedCount = json.optInt("updated_count", 0)
                Result.success(updatedCount)
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        authToken: String? = null
    ): Result<ChangePasswordResponseDto> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("current_password", currentPassword)
                put("new_password", newPassword)
                put("confirm_password", confirmPassword)
            }
            val body = jsonPayload.toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("change_password.php"))
                .post(body)

            if (!authToken.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = JSONObject(responseBody)
                val dto = ChangePasswordResponseDto.fromJson(json)
                if (dto.error != null && !dto.success) {
                    Result.failure(IOException(dto.error))
                } else {
                    Result.success(dto)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun getCustomerLedger(
        customerId: Int,
        authToken: String? = null
    ): Result<CustomerLedgerDto> = withContext(Dispatchers.IO) {
        if (customerId <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Valid customer ID is required."))
        }
        val jsonResult = executeAuthenticatedGetJson(
            endpoint = "get_customer_ledger.php?customer_id=$customerId",
            authToken = authToken,
            serviceLabel = "customer ledger"
        )
        jsonResult.mapCatching { json ->
            val dto = CustomerLedgerDto.fromJson(json)
            if (dto.error != null && !dto.success) {
                throw IOException(dto.error)
            }
            dto
        }
    }

    suspend fun getExportData(
        cycle: String = "all",
        startDate: String? = null,
        endDate: String? = null,
        authToken: String? = null
    ): Result<ExportReportDataDto> = withContext(Dispatchers.IO) {
        val params = buildList {
            if (cycle.isNotBlank()) add("cycle=${java.net.URLEncoder.encode(cycle, "UTF-8")}")
            if (!startDate.isNullOrBlank()) add("start_date=${java.net.URLEncoder.encode(startDate, "UTF-8")}")
            if (!endDate.isNullOrBlank()) add("end_date=${java.net.URLEncoder.encode(endDate, "UTF-8")}")
        }
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val jsonResult = executeAuthenticatedGetJson(
            endpoint = "get_export_data.php$query",
            authToken = authToken,
            serviceLabel = "export report"
        )
        jsonResult.mapCatching { json ->
            val dto = ExportReportDataDto.fromJson(json)
            if (dto.error != null && !dto.success) {
                throw IOException(dto.error)
            }
            dto
        }
    }

    suspend fun getPaymentReceipt(
        paymentId: Int,
        authToken: String? = null
    ): Result<PaymentReceiptDto> = withContext(Dispatchers.IO) {
        if (paymentId <= 0) {
            return@withContext Result.failure(IllegalArgumentException("Valid payment ID is required."))
        }
        val jsonResult = executeAuthenticatedGetJson(
            endpoint = "get_payment_receipt.php?payment_id=$paymentId",
            authToken = authToken,
            serviceLabel = "payment receipt"
        )
        jsonResult.mapCatching { json ->
            val dto = PaymentReceiptResponseDto.fromJson(json)
            if (dto.error != null && !dto.success) {
                throw IOException(dto.error)
            }
            dto.receipt
        }
    }

    suspend fun closeLoan(
        dto: CloseLoanRequestDto,
        authToken: String? = null
    ): Result<CloseLoanResponseDto> = withContext(Dispatchers.IO) {
        try {
            if (authToken.isNullOrBlank()) {
                return@withContext Result.failure(SessionExpiredException("Authentication required. Please log in again.", 401))
            }
            val body = dto.toJson().toString().toRequestBody(jsonMediaType)
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint("close_loan.php"))
                .addHeader("Authorization", "Bearer $authToken")
                .post(body)

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return@withContext Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return@withContext Result.failure(IOException(err))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return@withContext Result.failure(IOException("Invalid response format from loan closure service."))
                }
                val respDto = CloseLoanResponseDto.fromJson(json)
                if (respDto.error != null && !respDto.success) {
                    Result.failure(IOException(respDto.error))
                } else {
                    Result.success(respDto)
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    suspend fun searchCustomers(
        query: String = "",
        cycle: String = "all",
        status: String = "all",
        sort: String = "name_asc",
        authToken: String? = null
    ): Result<SearchCustomersResponseDto> = withContext(Dispatchers.IO) {
        val params = listOf(
            "q=${java.net.URLEncoder.encode(query.trim(), "UTF-8")}",
            "cycle=${java.net.URLEncoder.encode(cycle.trim(), "UTF-8")}",
            "status=${java.net.URLEncoder.encode(status.trim(), "UTF-8")}",
            "sort=${java.net.URLEncoder.encode(sort.trim(), "UTF-8")}"
        ).joinToString("&")
        val jsonResult = executeAuthenticatedGetJson(
            endpoint = "search_customers.php?$params",
            authToken = authToken,
            serviceLabel = "customer search"
        )
        jsonResult.mapCatching { json ->
            val dto = SearchCustomersResponseDto.fromJson(json)
            if (dto.error != null && !dto.success) {
                throw IOException(dto.error)
            }
            dto
        }
    }

    private fun executeAuthenticatedGetJson(
        endpoint: String,
        authToken: String?,
        serviceLabel: String
    ): Result<JSONObject> {
        return try {
            if (authToken.isNullOrBlank()) {
                return Result.failure(SessionExpiredException("Authentication required. Please log in again.", 401))
            }
            val reqBuilder = Request.Builder()
                .url(ApiConfig.getEndpoint(endpoint))
                .addHeader("Authorization", "Bearer $authToken")
                .get()

            client.newCall(reqBuilder.build()).execute().use { response ->
                val statusCode = response.code
                val responseBody = response.body?.string() ?: ""

                if (statusCode == 401) {
                    val err = parseErrorMessage(responseBody) ?: "Authentication expired. Please log in again."
                    return Result.failure(SessionExpiredException(err, statusCode))
                }
                if (!response.isSuccessful) {
                    val err = parseErrorMessage(responseBody) ?: "Server error HTTP $statusCode"
                    return Result.failure(IOException(err))
                }

                val json = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    return Result.failure(IOException("Invalid response format from $serviceLabel service."))
                }
                Result.success(json)
            }
        } catch (e: Exception) {
            Result.failure(IOException(getFriendlyErrorMessage(e), e))
        }
    }

    private fun parseErrorMessage(responseBody: String): String? {
        return try {
            val obj = JSONObject(responseBody)
            obj.optString("error").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("detail").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}

