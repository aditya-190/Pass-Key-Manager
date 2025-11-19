package com.bhardwaj.passkey.utils

object PasswordGenerator {
    fun generate(
        length: Int,
        includeUpper: Boolean,
        includeLower: Boolean,
        includeNumbers: Boolean,
        includeSpecial: Boolean
    ): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val numbers = "0123456789"
        val special = "!@#$%^&*()_+-=[]{}|;:,.<>?"

        var charPool = ""
        if (includeUpper) charPool += upper
        if (includeLower) charPool += lower
        if (includeNumbers) charPool += numbers
        if (includeSpecial) charPool += special

        if (charPool.isEmpty()) charPool = lower

        return (1..length)
            .map { charPool.random() }
            .joinToString("")
    }
}