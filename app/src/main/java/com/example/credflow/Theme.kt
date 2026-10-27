package com.example.credflow

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.util.Locale

val Navy = Color(0xFF173865)
val Blue = Color(0xFF235DC5)
val PageBg = Color(0xFFF3F6FA)
val Muted = Color(0xFF617187)
val Border = Color(0xFFDDE5EF)
val Green = Color(0xFF14856B)
val Orange = Color(0xFFB76B12)

fun rupees(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value).replace("INR", "₹")
