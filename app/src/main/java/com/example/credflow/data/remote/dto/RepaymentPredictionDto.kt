package com.example.credflow.data.remote.dto

data class RepaymentPredictionDto(
    val status: String,
    val repaymentProbability: Double? = null,
    val predictedInstallments: Int = 0,
    val historicalRows: Int = 0,
    val message: String? = null
)
