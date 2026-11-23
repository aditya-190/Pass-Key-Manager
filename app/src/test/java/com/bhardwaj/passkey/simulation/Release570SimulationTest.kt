package com.bhardwaj.passkey.simulation

import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.data.security.VaultMigration
import com.bhardwaj.passkey.utils.PasswordStrength
import com.bhardwaj.passkey.utils.Strength
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Feature workflow step 3 for the 5.7.0 recovery-password rules: seeded random passwords in every supported
 * script (emoji, RTL, CJK, Indic), empty and 10k-character values. Every assertion carries its seed.
 */
class Release570SimulationTest {
    private val alphabet = "abcxyzABCXYZ0189 !@#\$%^&*()-_=+[]{};:'\",.<>/?\\|`~€ñçहिন্দীعربيעברית中文日本語한국어🔐😀👨‍👩‍👧"
    private val symbols = alphabet.codePoints().toArray().map { String(Character.toChars(it)) }

    private fun Random.password(): String {
        val length = when (nextInt(10)) {
            0 -> 0
            1 -> 10_000
            else -> nextInt(40)
        }
        return buildString { repeat(length) { append(symbols[nextInt(symbols.size)]) } }
    }

    @Test
    fun `random passwords never break the strength rules`() {
        var runs = 0
        for (seed in 1L..SEEDS) {
            val rnd = Random(seed)
            val password = rnd.password()
            val strength = PasswordStrength.of(password)
            assertEquals("seed $seed deterministic", strength, PasswordStrength.of(password))
            val codePoints = password.codePointCount(0, password.length)
            if (PasswordStrength.acceptableForRecovery(password)) {
                assertTrue("seed $seed too short", codePoints >= PasswordStrength.MIN_RECOVERY_LENGTH)
                assertTrue("seed $seed weak accepted", strength != Strength.WEAK)
            }
            if (codePoints < PasswordStrength.MIN_RECOVERY_LENGTH) {
                assertTrue("seed $seed short accepted", !PasswordStrength.acceptableForRecovery(password))
            }
            runs++
        }
        assertEquals(SEEDS.toInt(), runs)
    }

    @Test
    fun `repeats and runs are weak at any length`() {
        for (seed in 1L..SEEDS) {
            val rnd = Random(seed)
            val length = PasswordStrength.MIN_RECOVERY_LENGTH + rnd.nextInt(200)
            val symbol = symbols[rnd.nextInt(symbols.size)]
            assertEquals("seed $seed repeat", Strength.WEAK, PasswordStrength.of(symbol.repeat(length)))
            val start = 'a'.code + rnd.nextInt(10)
            val run = (0 until length).joinToString("") { String(Character.toChars(start + it)) }
            val ordered = if (rnd.nextBoolean()) run else run.reversed()
            assertEquals("seed $seed run", Strength.WEAK, PasswordStrength.of(ordered))
        }
    }

    @Test
    fun `huge values are measured quickly`() {
        val rnd = Random(SEEDS)
        val started = System.nanoTime()
        repeat(HUGE_RUNS) {
            PasswordStrength.of(buildString { repeat(10_000) { append(symbols[rnd.nextInt(symbols.size)]) } })
        }
        val perCallMs = (System.nanoTime() - started) / 1_000_000.0 / HUGE_RUNS
        // The meter recomputes on every keystroke; a pasted 10k-character value must not freeze typing.
        assertTrue("$perCallMs ms per 10k-character value", perCallMs < MAX_MS_PER_HUGE_VALUE)
    }

    @Test
    fun `every migration failure reason makes a valid event`() {
        VaultMigration.FailureReason.entries.forEach { reason ->
            val event = AppEvents.vaultMigrationFailed(reason.name)
            assertTrue(reason.name, EventValidator.validate(event).isEmpty())
        }
    }

    private companion object {
        const val SEEDS = 5_000L
        const val HUGE_RUNS = 50
        const val MAX_MS_PER_HUGE_VALUE = 50.0
    }
}
