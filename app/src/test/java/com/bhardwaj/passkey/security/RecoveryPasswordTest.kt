package com.bhardwaj.passkey.security

import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.utils.PasswordStrength
import com.bhardwaj.passkey.utils.Strength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 5.7.0 attack-pass findings 1 and 2. The recovery slot is guessable offline by anyone holding the phone's
 * files, so its password strength is the real protection; and the one-time upgrade step needs a funnel.
 */
class RecoveryPasswordTest {
    @Test
    fun `recovery passwords need at least ten characters`() {
        assertEquals(10, PasswordStrength.MIN_RECOVERY_LENGTH)
        assertFalse(PasswordStrength.acceptableForRecovery("Tr0ub4dor"))
        assertTrue(PasswordStrength.acceptableForRecovery("Tr0ub4dor&3x"))
    }

    @Test
    fun `obviously guessable passwords are weak whatever their length`() {
        listOf("aaaaaaaaaaaa", "1234567890", "abcdefghijkl", "password1234", "qwertyuiop12", "1111111111111111")
            .forEach { assertEquals(it, Strength.WEAK, PasswordStrength.of(it)) }
        listOf("aaaaaaaaaaaa", "1234567890", "password1234").forEach {
            assertFalse("$it accepted", PasswordStrength.acceptableForRecovery(it))
        }
    }

    @Test
    fun `strength grows with length and variety`() {
        assertEquals(Strength.WEAK, PasswordStrength.of("short"))
        assertEquals(Strength.FAIR, PasswordStrength.of("mangoriver7"))
        assertEquals(Strength.STRONG, PasswordStrength.of("Mango-River-Tiger-42"))
        assertEquals(Strength.STRONG, PasswordStrength.of("correct horse battery staple"))
    }

    @Test
    fun `non-latin passphrases are measured fairly`() {
        assertEquals(Strength.STRONG, PasswordStrength.of("मेरा पासवर्ड बहुत सुरक्षित है"))
    }

    @Test
    fun `the upgrade funnel events are valid and carry no content`() {
        val events = listOf(
            AppEvents.recoverySetupShown(existingVault = true),
            AppEvents.recoverySetupCompleted(existingVault = false),
            AppEvents.vaultMigrationFailed(reason = "EXPORT_FAILED")
        )
        events.forEach { assertTrue(it.name, EventValidator.validate(it).isEmpty()) }
        assertEquals(
            listOf(
                AppEventNames.RECOVERY_SETUP_SHOWN,
                AppEventNames.RECOVERY_SETUP_COMPLETED,
                AppEventNames.VAULT_MIGRATION_FAILED
            ),
            events.map { it.name }
        )
        assertEquals("export_failed", events[2].params.values.single())
    }
}
