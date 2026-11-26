package com.example.credflow.data.remote.dto

import org.json.JSONObject

data class ChatResponseDto(
    val status: String = "",
    val intent: String = "",
    val question: String = "",
    val answer: String = "",
    val source: String = "",
    val language: String = "ENGLISH",
    val customerId: Int? = null,
    val dataChanged: Boolean = false,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = status.equals("ok", ignoreCase = true) && error.isNullOrBlank()

    companion object {
        fun fromJson(json: JSONObject): ChatResponseDto {
            val status = json.optString("status", "")
            val intent = json.optString("intent", "")
            val question = json.optString("question", "")
            val answer = json.optString("answer", "")
            val source = json.optString("source", "")
            val language = json.optString("language", "ENGLISH")
            val custObj = json.optJSONObject("customer")
            val contextUpdate = json.optJSONObject("context_update")
            val custId = custObj?.optInt("id", 0)?.takeIf { it > 0 }
                ?: contextUpdate?.optInt("last_customer_id", 0)?.takeIf { it > 0 }
            val dataChanged = json.optBoolean("data_changed", false)
            val error = json.optString("error").takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() && status == "error" }
                ?: json.optString("detail").takeIf { it.isNotBlank() && status == "error" }

            return ChatResponseDto(
                status = status,
                intent = intent,
                question = question,
                answer = answer,
                source = source,
                language = language,
                customerId = custId,
                dataChanged = dataChanged,
                error = error
            )
        }
    }
}

