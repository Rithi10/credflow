package com.example.credflow.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class ForecastCustomerDto(
    val customerId: Int,
    val name: String,
    val phone: String,
    val cycle: String,
    val amountDue: Double,
    val expectedAmount: Double
) {
    companion object {
        fun fromJson(json: JSONObject): ForecastCustomerDto {
            return ForecastCustomerDto(
                customerId = json.optInt("customer_id", 0),
                name = json.optString("name", "Customer"),
                phone = json.optString("phone", ""),
                cycle = json.optString("cycle", "Daily"),
                amountDue = json.optDouble("amount_due", 0.0),
                expectedAmount = json.optDouble("expected_amount", 0.0)
            )
        }
    }
}

data class ForecastHorizonDto(
    val periodKey: String = "",
    val targetDate: String = "",
    val daysCount: Int = 1,
    val scheduledAmount: Double = 0.0,
    val expectedAmount: Double = 0.0,
    val lowEstimate: Double = 0.0,
    val highEstimate: Double = 0.0,
    val expectedCustomerCount: Int = 0,
    val customers: List<ForecastCustomerDto> = emptyList()
) {
    companion object {
        fun fromJson(key: String, json: JSONObject): ForecastHorizonDto {
            val custList = mutableListOf<ForecastCustomerDto>()
            val arr = json.optJSONArray("customers")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) {
                        custList.add(ForecastCustomerDto.fromJson(obj))
                    }
                }
            }
            return ForecastHorizonDto(
                periodKey = key,
                targetDate = json.optString("target_date", ""),
                daysCount = json.optInt("days_count", if (key == "tomorrow") 1 else if (key == "next_7_days") 7 else 30),
                scheduledAmount = json.optDouble("scheduled_amount", 0.0),
                expectedAmount = json.optDouble("expected_amount", 0.0),
                lowEstimate = json.optDouble("low_estimate", 0.0),
                highEstimate = json.optDouble("high_estimate", 0.0),
                expectedCustomerCount = json.optInt("expected_customer_count", custList.size),
                customers = custList
            )
        }
    }
}

data class CycleBreakdownItemDto(
    val scheduledAmount: Double = 0.0,
    val expectedAmount: Double = 0.0,
    val customerCount: Int = 0,
    val weekdayDistribution: Map<String, Double> = emptyMap()
) {
    companion object {
        fun fromJson(json: JSONObject): CycleBreakdownItemDto {
            val dist = mutableMapOf<String, Double>()
            val distObj = json.optJSONObject("weekday_distribution")
            if (distObj != null) {
                val keys = distObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    dist[k] = distObj.optDouble(k, 0.0)
                }
            }
            return CycleBreakdownItemDto(
                scheduledAmount = json.optDouble("scheduled_amount", 0.0),
                expectedAmount = json.optDouble("expected_amount", 0.0),
                customerCount = json.optInt("customer_count", 0),
                weekdayDistribution = dist
            )
        }
    }
}

data class TimelineDayDto(
    val date: String,
    val dayName: String,
    val scheduledAmount: Double,
    val expectedAmount: Double,
    val lowEstimate: Double,
    val highEstimate: Double,
    val expectedCustomerCount: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): TimelineDayDto {
            return TimelineDayDto(
                date = json.optString("date", ""),
                dayName = json.optString("day_name", ""),
                scheduledAmount = json.optDouble("scheduled_amount", 0.0),
                expectedAmount = json.optDouble("expected_amount", 0.0),
                lowEstimate = json.optDouble("low_estimate", 0.0),
                highEstimate = json.optDouble("high_estimate", 0.0),
                expectedCustomerCount = json.optInt("expected_customer_count", 0)
            )
        }
    }
}

data class OverduePoolDto(
    val overdueCount: Int = 0,
    val overdueAmount: Double = 0.0,
    val expectedOverdueRecovery: Double = 0.0
) {
    companion object {
        fun fromJson(json: JSONObject?): OverduePoolDto {
            if (json == null) return OverduePoolDto()
            return OverduePoolDto(
                overdueCount = json.optInt("overdue_count", 0),
                overdueAmount = json.optDouble("overdue_amount", 0.0),
                expectedOverdueRecovery = json.optDouble("expected_overdue_recovery", 0.0)
            )
        }
    }
}

data class CollectionForecastDto(
    val status: String = "",
    val userId: Int = 0,
    val forecastPeriod: String = "all",
    val predictionMethod: String = "RULE_BASED_CASHFLOW",
    val predictionMethodLabel: String = "Rule-Based Cash-Flow Model (Baseline Recovery)",
    val isSufficientData: Boolean = false,
    val insufficientDataReasons: List<String> = emptyList(),
    val dataTimestamp: String = "",
    val scheduledAmount: Double = 0.0,
    val expectedAmount: Double = 0.0,
    val lowEstimate: Double = 0.0,
    val highEstimate: Double = 0.0,
    val expectedCustomerCount: Int = 0,
    val overduePool: OverduePoolDto = OverduePoolDto(),
    val horizons: Map<String, ForecastHorizonDto> = emptyMap(),
    val cycleBreakdown: Map<String, CycleBreakdownItemDto> = emptyMap(),
    val timeline: List<TimelineDayDto> = emptyList(),
    val message: String? = null,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = (status.equals("ok", ignoreCase = true) || status.equals("insufficient_data", ignoreCase = true)) && error.isNullOrBlank()

    val isEmpirical: Boolean
        get() = predictionMethod.equals("EMPIRICAL_HISTORICAL", ignoreCase = true)

    val isRuleBased: Boolean
        get() = !isEmpirical

    val isNoActiveInstallments: Boolean
        get() = status.equals("no_active_installments", ignoreCase = true) || (scheduledAmount == 0.0 && expectedCustomerCount == 0 && (horizons["next_30_days"]?.customers?.isEmpty() ?: true))

    fun getHorizon(key: String): ForecastHorizonDto {
        return horizons[key] ?: ForecastHorizonDto(
            periodKey = key,
            daysCount = if (key == "tomorrow") 1 else if (key == "next_7_days") 7 else 30,
            scheduledAmount = scheduledAmount,
            expectedAmount = expectedAmount,
            lowEstimate = lowEstimate,
            highEstimate = highEstimate,
            expectedCustomerCount = expectedCustomerCount
        )
    }

    companion object {
        fun fromJson(json: JSONObject): CollectionForecastDto {
            val status = json.optString("status", "")
            val userId = json.optInt("user_id", 0)
            val forecastPeriod = json.optString("forecast_period", "all")
            val predictionMethod = json.optString("prediction_method", "RULE_BASED_CASHFLOW")
            val predictionMethodLabel = json.optString(
                "prediction_method_label",
                if (predictionMethod == "EMPIRICAL_HISTORICAL") "Empirical Realization Model (Historical Payment Rate)"
                else "Rule-Based Cash-Flow Model (Baseline Recovery)"
            )
            val isSufficientData = json.optBoolean("is_sufficient_data", false)

            val reasons = mutableListOf<String>()
            val rArr = json.optJSONArray("insufficient_data_reasons")
            if (rArr != null) {
                for (i in 0 until rArr.length()) {
                    reasons.add(rArr.optString(i))
                }
            }

            val timestamp = json.optString("data_timestamp", "")
            val schedAmt = json.optDouble("scheduled_amount", 0.0)
            val expAmt = json.optDouble("expected_amount", 0.0)
            val lowEst = json.optDouble("low_estimate", 0.0)
            val highEst = json.optDouble("high_estimate", 0.0)
            val custCount = json.optInt("expected_customer_count", 0)

            val overdue = OverduePoolDto.fromJson(json.optJSONObject("overdue_pool"))

            val horizonMap = mutableMapOf<String, ForecastHorizonDto>()
            val hObj = json.optJSONObject("horizons")
            if (hObj != null) {
                val keys = hObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val horizonJson = hObj.optJSONObject(k)
                    if (horizonJson != null) {
                        horizonMap[k] = ForecastHorizonDto.fromJson(k, horizonJson)
                    }
                }
            }

            val cycleMap = mutableMapOf<String, CycleBreakdownItemDto>()
            val cObj = json.optJSONObject("cycle_breakdown")
            if (cObj != null) {
                val keys = cObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val cycleJson = cObj.optJSONObject(k)
                    if (cycleJson != null) {
                        cycleMap[k] = CycleBreakdownItemDto.fromJson(cycleJson)
                    }
                }
            }

            val timelineList = mutableListOf<TimelineDayDto>()
            val tArr = json.optJSONArray("timeline_next_30_days")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    val tObj = tArr.optJSONObject(i)
                    if (tObj != null) {
                        timelineList.add(TimelineDayDto.fromJson(tObj))
                    }
                }
            }

            val msg = json.optString("message").takeIf { it.isNotBlank() }
            val err = json.optString("error").takeIf { it.isNotBlank() }
                ?: json.optString("detail").takeIf { it.isNotBlank() && status == "error" }

            return CollectionForecastDto(
                status = status,
                userId = userId,
                forecastPeriod = forecastPeriod,
                predictionMethod = predictionMethod,
                predictionMethodLabel = predictionMethodLabel,
                isSufficientData = isSufficientData,
                insufficientDataReasons = reasons,
                dataTimestamp = timestamp,
                scheduledAmount = schedAmt,
                expectedAmount = expAmt,
                lowEstimate = lowEst,
                highEstimate = highEst,
                expectedCustomerCount = custCount,
                overduePool = overdue,
                horizons = horizonMap,
                cycleBreakdown = cycleMap,
                timeline = timelineList,
                message = msg,
                error = err
            )
        }
    }
}

