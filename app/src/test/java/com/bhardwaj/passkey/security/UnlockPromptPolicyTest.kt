package com.bhardwaj.passkey.security

import com.bhardwaj.passkey.data.security.UnlockPromptPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When the unlock prompt appears on its own (5.6.0 rules, carried into the 5.7.0 vault gate). Starting it from
 * a screen composed in the background left users with nothing to tap; prompting on every return to Locked
 * re-prompted forever after a cancel, because the system PIN screen itself resumes the app on API 28.
 */
class UnlockPromptPolicyTest {
    private val policy = UnlockPromptPolicy()

    @Test
    fun `the first showing of the lock screen prompts`() {
        assertTrue(policy.shouldPromptOnResume(isLocked = true))
    }

    @Test
    fun `a lock screen created in the background prompts when it is finally shown`() {
        policy.onStopped()
        assertTrue(policy.shouldPromptOnResume(isLocked = true))
    }

    @Test
    fun `no second prompt while one is already showing`() {
        policy.onPromptStarted()
        assertFalse(policy.shouldPromptOnResume(isLocked = true))
        assertFalse(policy.canPromptManually(isLocked = true))
    }

    @Test
    fun `cancelling the system PIN screen does not immediately prompt again`() {
        policy.onPromptStarted()
        policy.onStopped() // the PIN screen covers the app; this is not leaving it
        policy.onPromptFinished()
        assertFalse(policy.shouldPromptOnResume(isLocked = true))
        assertTrue("the Unlock button still works", policy.canPromptManually(isLocked = true))
    }

    @Test
    fun `leaving the app and coming back prompts again after a cancel`() {
        policy.onPromptStarted()
        policy.onPromptFinished()
        policy.onStopped()
        assertTrue(policy.shouldPromptOnResume(isLocked = true))
    }

    @Test
    fun `nothing prompts when not on the locked step`() {
        assertFalse(policy.shouldPromptOnResume(isLocked = false))
        assertFalse(policy.canPromptManually(isLocked = false))
    }
}
