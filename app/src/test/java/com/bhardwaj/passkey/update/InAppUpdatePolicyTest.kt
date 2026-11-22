package com.bhardwaj.passkey.update

import org.junit.Assert.assertEquals
import org.junit.Test

class InAppUpdatePolicyTest {
    private val both = PlayUpdateInfo(updateAvailable = true, immediateAllowed = true, flexibleAllowed = true)
    private val soft = UpdateDecision.Soft(10)

    private fun choose(decision: UpdateDecision, play: PlayUpdateInfo?, enabled: Boolean = true) =
        InAppUpdatePolicy.choose(decision, play, enabled)

    @Test
    fun `hard update uses IMMEDIATE when Play allows it`() {
        assertEquals(InAppUpdateAction.START_IMMEDIATE, choose(UpdateDecision.Hard, both))
    }

    @Test
    fun `hard update falls back to the gate`() {
        assertEquals(InAppUpdateAction.SHOW_GATE, choose(UpdateDecision.Hard, null))
        assertEquals(InAppUpdateAction.SHOW_GATE, choose(UpdateDecision.Hard, both, enabled = false))
        assertEquals(InAppUpdateAction.SHOW_GATE, choose(UpdateDecision.Hard, both.copy(immediateAllowed = false)))
        assertEquals(InAppUpdateAction.SHOW_GATE, choose(UpdateDecision.Hard, both.copy(updateAvailable = false)))
    }

    @Test
    fun `hard update resumes an IMMEDIATE update in progress`() {
        val inProgress =
            PlayUpdateInfo(false, immediateAllowed = false, flexibleAllowed = false, immediateInProgress = true)
        assertEquals(InAppUpdateAction.START_IMMEDIATE, choose(UpdateDecision.Hard, inProgress))
    }

    @Test
    fun `soft update uses FLEXIBLE, falling back to our prompt`() {
        assertEquals(InAppUpdateAction.START_FLEXIBLE, choose(soft, both))
        assertEquals(InAppUpdateAction.SHOW_SOFT_PROMPT, choose(soft, null))
        assertEquals(InAppUpdateAction.SHOW_SOFT_PROMPT, choose(soft, both, enabled = false))
        assertEquals(InAppUpdateAction.SHOW_SOFT_PROMPT, choose(soft, both.copy(flexibleAllowed = false)))
    }

    @Test
    fun `downloaded flexible update asks for a restart`() {
        val downloaded = both.copy(flexibleDownloaded = true)
        assertEquals(InAppUpdateAction.COMPLETE_FLEXIBLE, choose(soft, downloaded))
        assertEquals(InAppUpdateAction.COMPLETE_FLEXIBLE, choose(UpdateDecision.None, downloaded))
    }

    @Test
    fun `no update and maintenance do nothing`() {
        assertEquals(InAppUpdateAction.NONE, choose(UpdateDecision.None, both))
        assertEquals(InAppUpdateAction.NONE, choose(UpdateDecision.Maintenance, both))
    }

    @Test
    fun `result names`() {
        assertEquals("ok", inAppUpdateResultName(-1))
        assertEquals("cancelled", inAppUpdateResultName(0))
        assertEquals("failed", inAppUpdateResultName(1))
    }
}
