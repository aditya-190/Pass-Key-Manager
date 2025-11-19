package com.bhardwaj.passkey.utils

import com.bhardwaj.passkey.data.local.entity.Details

data class PasswordAnalysisResult(
    val totalPasswords: Int = 0,
    val weakPasswords: List<Details> = emptyList(),
    val reusedPasswords: Map<String, List<Details>> = emptyMap(),
    val strengthScore: Int = 0
)

object PasswordAnalyzer {

    fun analyze(details: List<Details>): PasswordAnalysisResult {
        val passwordEntries = details.filter {
            val q = it.question.lowercase()
            q.contains("password") || q.contains("pin") || q.contains("code") || q.contains("secret")
        }

        val weakList = mutableListOf<Details>()
        val reuseMap = mutableMapOf<String, MutableList<Details>>()

        passwordEntries.forEach { detail ->
            val pass = detail.answer

            if (isWeak(pass)) {
                weakList.add(detail)
            }

            if (reuseMap.containsKey(pass)) {
                reuseMap[pass]?.add(detail)
            } else {
                reuseMap[pass] = mutableListOf(detail)
            }
        }

        val actualReused = reuseMap.filter { it.value.size > 1 }

        var score = 100
        val weakPenalty = (weakList.size * 10).coerceAtMost(50)
        val reusedPenalty = (actualReused.size * 15).coerceAtMost(50)

        score -= (weakPenalty + reusedPenalty)
        if (passwordEntries.isEmpty()) score = 100

        return PasswordAnalysisResult(
            totalPasswords = passwordEntries.size,
            weakPasswords = weakList,
            reusedPasswords = actualReused,
            strengthScore = score.coerceAtLeast(0)
        )
    }

    private fun isWeak(password: String): Boolean {
        if (password.length < 8) return true
        if (password.all { it.isDigit() }) return true
        if (password.all { it.isLetter() }) return true
        return false
    }
}