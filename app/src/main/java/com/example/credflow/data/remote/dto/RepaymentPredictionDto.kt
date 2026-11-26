package com.example.credflow.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class CustomerRiskDto(
    val customerId: Int,
    val name: String,
    val phone: String,
    val cycle: String,
    val totalDue: Double,
    val paidAmount: Double,
    val remainingDue: Double,
    val installmentAmount: Double,
    val missedPayments: Int,
    val overdueDays: Int,
    val paymentRatio: Double,
    val repaymentProbability: Double,
    val riskTier: String,
    val riskReason: String,
    val confidence: String
) {
    companion object {
        fun fromJson(json: JSONObject): CustomerRiskDto {
            return CustomerRiskDto(
                customerId = json.optInt("customer_id", 0),
                name = json.optString("name", "Customer"),
                phone = json.optString("phone", ""),
                cycle = json.optString("cycle", "Daily"),
                totalDue = json.optDouble("total_due", 0.0),
                paidAmount = json.optDouble("paid_amount", 0.0),
                remainingDue = json.optDouble("remaining_due", 0.0),
                installmentAmount = json.optDouble("installment_amount", 0.0),
                missedPayments = json.optInt("missed_payments", 0),
                overdueDays = json.optInt("overdue_days", 0),
                paymentRatio = json.optDouble("payment_ratio", 0.0),
                repaymentProbability = json.optDouble("repayment_probability", 0.85),
                riskTier = json.optString("risk_tier", "Moderate Risk"),
                riskReason = json.optString("risk_reason", ""),
                confidence = json.optString("confidence", "Rule-Based")
            )
        }
    }
}

data class RepaymentPredictionDto(
    val status: String,
    val predictionType: String,
    val predictionTypeDisplay: String,
    val repaymentProbability: Double?,
    val overallRiskTier: String,
    val overallReason: String,
    val confidence: String,
    val predictedInstallments: Int,
    val totalCustomers: Int,
    val historicalRows: Int,
    val requiredTrainingRows: Int,
    val modelVersion: String,
    val message: String,
    val disclaimer: String,
    val customerPredictions: List<CustomerRiskDto> = emptyList(),
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = (status == "ok" || status == "insufficient_data") && repaymentProbability != null

    val isMlModel: Boolean
        get() = predictionType == "ml_model"

    val isEarlyPrediction: Boolean
        get() = predictionType != "ml_model" && repaymentProbability != null

    val isNoActiveInstallments: Boolean
        get() = status == "no_active_installments"

    companion object {
        fun fromJson(json: JSONObject): RepaymentPredictionDto {
            val status = json.optString("status", "")
            val prob = if (json.has("repayment_probability") && !json.isNull("repayment_probability")) {
                val p = json.optDouble("repayment_probability")
                if (p.isNaN() || p < 0.0 || p > 1.0) null else p
            } else if (json.has("rule_based_estimate") && !json.isNull("rule_based_estimate")) {
                val p = json.optDouble("rule_based_estimate")
                if (p.isNaN() || p < 0.0 || p > 1.0) null else p
            } else {
                null
            }
            val predictionType = json.optString("prediction_type", if (status == "ok") "ml_model" else "early_rule_based")
            val predictionTypeDisplay = json.optString(
                "prediction_type_display",
                if (predictionType == "ml_model") "Trained PyTorch Machine Learning Model" else "Early Dynamic Prediction (Transparent Rules)"
            )
            val overallRiskTier = json.optString("overall_risk_tier", if ((prob ?: 0.0) >= 0.75) "Low Risk (High Likelihood)" else if ((prob ?: 0.0) >= 0.50) "Moderate Risk" else "High Risk of Delay")
            val overallReason = json.optString("overall_reason", "")
            val confidence = json.optString("confidence", if (predictionType == "ml_model") "High (PyTorch ML)" else "Rule-Based (Live collection history)")
            val predictedInstallments = json.optInt("predicted_installments", 0)
            val totalCustomers = json.optInt("total_customers", 0)
            val historicalRows = json.optInt("historical_rows", 0)
            val requiredTrainingRows = json.optInt("required_training_rows", 30)
            val modelVersion = json.optString("model_version", "")
            val message = json.optString("message", "")
            val disclaimer = json.optString("disclaimer", "")
            val error = json.optString("error").takeIf { it.isNotBlank() }

            val customersList = mutableListOf<CustomerRiskDto>()
            val custArray = json.optJSONArray("customer_predictions")
            if (custArray != null) {
                for (i in 0 until custArray.length()) {
                    val cObj = custArray.optJSONObject(i)
                    if (cObj != null) {
                        customersList.add(CustomerRiskDto.fromJson(cObj))
                    }
                }
            }

            return RepaymentPredictionDto(
                status = status,
                predictionType = predictionType,
                predictionTypeDisplay = predictionTypeDisplay,
                repaymentProbability = prob,
                overallRiskTier = overallRiskTier,
                overallReason = overallReason,
                confidence = confidence,
                predictedInstallments = predictedInstallments,
                totalCustomers = totalCustomers,
                historicalRows = historicalRows,
                requiredTrainingRows = requiredTrainingRows,
                modelVersion = modelVersion,
                message = message,
                disclaimer = disclaimer,
                customerPredictions = customersList,
                error = error
            )
        }
    }
}
