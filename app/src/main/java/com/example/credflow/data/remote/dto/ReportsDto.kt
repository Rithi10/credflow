package com.example.credflow.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class FilterInfoDto(
    val rangePreset: String = "all",
    val startDate: String? = null,
    val endDate: String? = null
) {
    companion object {
        fun fromJson(json: JSONObject?): FilterInfoDto {
            if (json == null) return FilterInfoDto()
            return FilterInfoDto(
                rangePreset = json.optString("range_preset", "all"),
                startDate = json.optString("start_date").takeIf { it.isNotBlank() && it != "null" },
                endDate = json.optString("end_date").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }
}

data class ReportsSummaryDto(
    val todayCollection: Double = 0.0,
    val weeklyCollection: Double = 0.0,
    val monthlyCollection: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalOverdue: Double = 0.0,
    val filteredCollection: Double = 0.0,
    val filteredPaymentsCount: Int = 0,
    val activeCustomersCount: Int = 0,
    val overdueCustomersCount: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject?): ReportsSummaryDto {
            if (json == null) return ReportsSummaryDto()
            return ReportsSummaryDto(
                todayCollection = json.optDouble("today_collection", 0.0),
                weeklyCollection = json.optDouble("weekly_collection", 0.0),
                monthlyCollection = json.optDouble("monthly_collection", 0.0),
                totalCollected = json.optDouble("total_collected", 0.0),
                totalPending = json.optDouble("total_pending", 0.0),
                totalOverdue = json.optDouble("total_overdue", 0.0),
                filteredCollection = json.optDouble("filtered_collection", 0.0),
                filteredPaymentsCount = json.optInt("filtered_payments_count", 0),
                activeCustomersCount = json.optInt("active_customers_count", 0),
                overdueCustomersCount = json.optInt("overdue_customers_count", 0)
            )
        }
    }
}

data class DailyChartPointDto(
    val date: String = "",
    val displayName: String = "",
    val dayName: String = "",
    val amount: Double = 0.0,
    val count: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): DailyChartPointDto {
            return DailyChartPointDto(
                date = json.optString("date", ""),
                displayName = json.optString("display_name", ""),
                dayName = json.optString("day_name", ""),
                amount = json.optDouble("amount", 0.0),
                count = json.optInt("count", 0)
            )
        }
    }
}

data class WeeklyChartPointDto(
    val key: String = "",
    val displayName: String = "",
    val amount: Double = 0.0,
    val count: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): WeeklyChartPointDto {
            return WeeklyChartPointDto(
                key = json.optString("key", ""),
                displayName = json.optString("display_name", ""),
                amount = json.optDouble("amount", 0.0),
                count = json.optInt("count", 0)
            )
        }
    }
}

data class MonthlyChartPointDto(
    val key: String = "",
    val displayName: String = "",
    val amount: Double = 0.0,
    val count: Int = 0
) {
    companion object {
        fun fromJson(json: JSONObject): MonthlyChartPointDto {
            return MonthlyChartPointDto(
                key = json.optString("key", ""),
                displayName = json.optString("display_name", ""),
                amount = json.optDouble("amount", 0.0),
                count = json.optInt("count", 0)
            )
        }
    }
}

data class CycleReportDto(
    val cycle: String = "",
    val customerCount: Int = 0,
    val collectedAmount: Double = 0.0,
    val pendingAmount: Double = 0.0,
    val overdueAmount: Double = 0.0
) {
    companion object {
        fun fromJson(json: JSONObject): CycleReportDto {
            return CycleReportDto(
                cycle = json.optString("cycle", ""),
                customerCount = json.optInt("customer_count", 0),
                collectedAmount = json.optDouble("collected_amount", 0.0),
                pendingAmount = json.optDouble("pending_amount", 0.0),
                overdueAmount = json.optDouble("overdue_amount", 0.0)
            )
        }
    }
}

data class RecentCollectionDto(
    val paymentId: Int = 0,
    val customerId: Int = 0,
    val customerName: String = "",
    val customerPhone: String = "",
    val cycle: String = "",
    val amount: Double = 0.0,
    val paymentDate: String = "",
    val method: String = "Cash",
    val note: String = ""
) {
    companion object {
        fun fromJson(json: JSONObject): RecentCollectionDto {
            return RecentCollectionDto(
                paymentId = json.optInt("payment_id", 0),
                customerId = json.optInt("customer_id", 0),
                customerName = json.optString("customer_name", "Customer"),
                customerPhone = json.optString("customer_phone", ""),
                cycle = json.optString("cycle", "Daily"),
                amount = json.optDouble("amount", 0.0),
                paymentDate = json.optString("payment_date", ""),
                method = json.optString("method", "Cash"),
                note = json.optString("note", "")
            )
        }
    }
}

data class ReportsDto(
    val status: String = "ok",
    val userId: Int = 0,
    val dataTimestamp: String = "",
    val filter: FilterInfoDto = FilterInfoDto(),
    val summary: ReportsSummaryDto = ReportsSummaryDto(),
    val dailyChart: List<DailyChartPointDto> = emptyList(),
    val weeklyChart: List<WeeklyChartPointDto> = emptyList(),
    val monthlyChart: List<MonthlyChartPointDto> = emptyList(),
    val cycleBreakdown: Map<String, CycleReportDto> = emptyMap(),
    val paymentMethods: Map<String, Double> = emptyMap(),
    val recentCollections: List<RecentCollectionDto> = emptyList(),
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = status == "ok" && error == null

    companion object {
        fun fromJson(json: JSONObject): ReportsDto {
            val status = json.optString("status", "error")
            val error = json.optString("error").takeIf { it.isNotBlank() }
                ?: json.optString("detail").takeIf { it.isNotBlank() }

            val filter = FilterInfoDto.fromJson(json.optJSONObject("filter"))
            val summary = ReportsSummaryDto.fromJson(json.optJSONObject("summary"))

            val chartsObj = json.optJSONObject("charts")
            val dailyList = mutableListOf<DailyChartPointDto>()
            val weeklyList = mutableListOf<WeeklyChartPointDto>()
            val monthlyList = mutableListOf<MonthlyChartPointDto>()

            if (chartsObj != null) {
                val dailyArr = chartsObj.optJSONArray("daily")
                if (dailyArr != null) {
                    for (i in 0 until dailyArr.length()) {
                        dailyArr.optJSONObject(i)?.let { dailyList.add(DailyChartPointDto.fromJson(it)) }
                    }
                }
                val weeklyArr = chartsObj.optJSONArray("weekly")
                if (weeklyArr != null) {
                    for (i in 0 until weeklyArr.length()) {
                        weeklyArr.optJSONObject(i)?.let { weeklyList.add(WeeklyChartPointDto.fromJson(it)) }
                    }
                }
                val monthlyArr = chartsObj.optJSONArray("monthly")
                if (monthlyArr != null) {
                    for (i in 0 until monthlyArr.length()) {
                        monthlyArr.optJSONObject(i)?.let { monthlyList.add(MonthlyChartPointDto.fromJson(it)) }
                    }
                }
            }

            val cycleMap = mutableMapOf<String, CycleReportDto>()
            val cycleObj = json.optJSONObject("cycle_breakdown")
            if (cycleObj != null) {
                val keys = cycleObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    cycleObj.optJSONObject(k)?.let {
                        cycleMap[k] = CycleReportDto.fromJson(it)
                    }
                }
            }

            val methodMap = mutableMapOf<String, Double>()
            val methodsObj = json.optJSONObject("payment_methods")
            if (methodsObj != null) {
                val keys = methodsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    methodMap[k] = methodsObj.optDouble(k, 0.0)
                }
            }

            val recentList = mutableListOf<RecentCollectionDto>()
            val recentArr = json.optJSONArray("recent_collections")
            if (recentArr != null) {
                for (i in 0 until recentArr.length()) {
                    recentArr.optJSONObject(i)?.let { recentList.add(RecentCollectionDto.fromJson(it)) }
                }
            }

            return ReportsDto(
                status = status,
                userId = json.optInt("user_id", 0),
                dataTimestamp = json.optString("data_timestamp", ""),
                filter = filter,
                summary = summary,
                dailyChart = dailyList,
                weeklyChart = weeklyList,
                monthlyChart = monthlyList,
                cycleBreakdown = cycleMap,
                paymentMethods = methodMap,
                recentCollections = recentList,
                error = error
            )
        }
    }
}

