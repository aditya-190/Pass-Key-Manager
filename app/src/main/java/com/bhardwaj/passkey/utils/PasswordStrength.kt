package com.bhardwaj.passkey.utils

import kotlin.math.ln

enum class Strength { WEAK, FAIR, STRONG }

/**
 * A deliberately simple, offline strength estimate: length times the variety of character classes, with obvious
 * patterns (one repeated character, runs like "abcdef" / "123456", common passwords) treated as weak at any
 * length. Counted in code points, so passphrases in Hindi, Arabic or Chinese are measured fairly.
 *
 * Used for the recovery password, which anyone holding the phone's files can try to guess offline: its strength
 * is the real protection of that key slot.
 */
object PasswordStrength {
    const val MIN_RECOVERY_LENGTH = 10

    private const val MIN_LENGTH_FOR_ANY_CREDIT = 8
    private const val FAIR_BITS = 40.0
    private const val STRONG_BITS = 60.0
    private const val SEQUENTIAL_SHARE = 0.8
    private const val COMMON_ONLY_BELOW = 16

    private val common = listOf(
        "password", "passwort", "qwerty", "letmein", "welcome", "iloveyou", "admin", "abc123", "passkey",
        "monkey", "dragon", "123123", "111111"
    )

    fun of(password: String): Strength {
        val codePoints = password.codePoints().toArray()
        val obviouslyWeak = codePoints.size < MIN_LENGTH_FOR_ANY_CREDIT ||
            codePoints.distinct().size <= 2 ||
            isMostlySequential(codePoints) ||
            (codePoints.size < COMMON_ONLY_BELOW && common.any { password.lowercase().contains(it) })
        if (obviouslyWeak) return Strength.WEAK
        val bits = codePoints.size * log2(poolSize(codePoints).toDouble())
        return when {
            bits >= STRONG_BITS -> Strength.STRONG
            bits >= FAIR_BITS -> Strength.FAIR
            else -> Strength.WEAK
        }
    }

    fun acceptableForRecovery(password: String): Boolean =
        password.codePointCount(0, password.length) >= MIN_RECOVERY_LENGTH && of(password) != Strength.WEAK

    /** "abcdef", "654321", "1234567890": most neighbouring characters differ by exactly one. */
    private fun isMostlySequential(codePoints: IntArray): Boolean {
        if (codePoints.size < 2) return false
        val steps = codePoints.toList().zipWithNext { a, b -> b - a }
        return steps.count { it == 1 || it == -1 } >= SEQUENTIAL_SHARE * steps.size
    }

    private fun poolSize(codePoints: IntArray): Int {
        var pool = 0
        if (codePoints.any { it in 'a'.code..'z'.code }) pool += 26
        if (codePoints.any { it in 'A'.code..'Z'.code }) pool += 26
        if (codePoints.any { it in '0'.code..'9'.code }) pool += 10
        if (codePoints.any { it < 128 && !Character.isLetterOrDigit(it) }) pool += 33
        if (codePoints.any { it >= 128 }) pool += 100
        return pool.coerceAtLeast(2)
    }

    private fun log2(x: Double) = ln(x) / ln(2.0)
}
