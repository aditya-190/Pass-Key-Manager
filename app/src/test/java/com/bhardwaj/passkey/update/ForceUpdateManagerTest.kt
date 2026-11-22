package com.bhardwaj.passkey.update

import com.bhardwaj.passkey.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceUpdateManagerTest {
    private data class Case(
        val current: Long,
        val maintenance: Boolean,
        val forceEnabled: Boolean,
        val min: Long,
        val latest: Long,
        val expected: UpdateDecision
    )

    private val table =
        listOf(
            // up to date
            Case(10, false, true, 5, 10, UpdateDecision.None),
            Case(10, false, false, 5, 10, UpdateDecision.None),
            // newer than latest (internal builds)
            Case(11, false, true, 5, 10, UpdateDecision.None),
            // below latest, at or above min -> soft
            Case(7, false, true, 5, 10, UpdateDecision.Soft(10)),
            Case(5, false, true, 5, 10, UpdateDecision.Soft(10)),
            // below min with force enabled -> hard
            Case(4, false, true, 5, 10, UpdateDecision.Hard),
            // below min with force disabled -> only soft
            Case(4, false, false, 5, 10, UpdateDecision.Soft(10)),
            // misconfigured min > latest still hard-gates when enabled
            Case(4, false, true, 8, 6, UpdateDecision.Hard),
            // maintenance wins over everything
            Case(4, true, true, 5, 10, UpdateDecision.Maintenance),
            Case(10, true, false, 1, 10, UpdateDecision.Maintenance)
        )

    @Test
    fun `decision table`() {
        table.forEach { c ->
            assertEquals(
                c.toString(),
                c.expected,
                ForceUpdateManager.decide(c.current, c.maintenance, c.forceEnabled, c.min, c.latest)
            )
        }
    }

    @Test
    fun `defaults produce no gate`() {
        assertEquals(UpdateDecision.None, ForceUpdateManager.decide(1, AppConfig()))
    }

    @Test
    fun `soft prompt throttled per version for the interval`() {
        val day = ForceUpdateManager.DAY_MS
        assertTrue(ForceUpdateManager.shouldShowSoftPrompt(5, null, null, 1_000))
        assertFalse(ForceUpdateManager.shouldShowSoftPrompt(5, 5, 1_000, 1_000 + day - 1))
        assertTrue(ForceUpdateManager.shouldShowSoftPrompt(5, 5, 1_000, 1_000 + day))
        assertFalse(ForceUpdateManager.shouldShowSoftPrompt(5, 5, 1_000, 1_000 + 2 * day, intervalDays = 3))
        assertTrue(ForceUpdateManager.shouldShowSoftPrompt(5, 5, 1_000, 1_000 + 3 * day, intervalDays = 3))
        // A newer latest version resets the throttle; a zero interval is treated as one day.
        assertTrue(ForceUpdateManager.shouldShowSoftPrompt(6, 5, 1_000, 1_001))
        assertFalse(ForceUpdateManager.shouldShowSoftPrompt(5, 5, 1_000, 1_001, intervalDays = 0))
    }
}
