package com.example.credflow.data.remote.dto

data class AuthResponseDto(
    val success: Boolean,
    val userId: Int,
    val token: String,
    val name: String,
    val businessName: String,
    val message: String,
    val email: String = "",
    val phone: String = "",
    val gstinNumber: String = "",
    val businessAddress: String = ""
)
