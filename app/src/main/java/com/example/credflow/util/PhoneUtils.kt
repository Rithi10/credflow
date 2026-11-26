package com.example.credflow.util

object PhoneUtils {
    /**
     * Normalizes a phone number for duplicate comparison: strips all non-digit characters
     * and, for numbers longer than 10 digits (e.g., +91 prefix or leading 0), keeps the last 10 digits.
     */
    fun normalizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val digits = phone.filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    /**
     * Returns true when two phone strings represent the same customer phone number.
     */
    fun phonesMatch(a: String?, b: String?): Boolean {
        val normA = normalizePhone(a)
        val normB = normalizePhone(b)
        return normA.length >= 6 && normA == normB
    }
}

