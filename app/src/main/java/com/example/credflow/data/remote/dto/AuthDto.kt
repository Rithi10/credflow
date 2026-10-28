package com.example.credflow.data.remote.dto

data class AuthResponseDto(
    val success: Boolean,
    val userId: Int,
    val token: String,
    val name: String,
    val businessName: String,
    val message: String
)

