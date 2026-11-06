package com.example.credflow.data.remote.dto

data class AuthResponseDto(
    val success: Boolean,
    val userId: Int,
    val token: String,
    val name: String,
    val businessName: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val message: String = ""
)
