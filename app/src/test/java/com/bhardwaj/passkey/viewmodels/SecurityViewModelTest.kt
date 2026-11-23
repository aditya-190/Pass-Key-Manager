package com.bhardwaj.passkey.viewmodels

import com.bhardwaj.passkey.data.security.VaultSession
import com.bhardwaj.passkey.domain.events.SecurityEvents
import com.bhardwaj.passkey.domain.viewModels.SecurityViewModel
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * When the unlock prompt appears on its own. Starting it while the app is in the background left users on
 * a screen with nothing to tap (5.6.0 device finding); prompting on every resume would trap a user who
 * cancels the system PIN screen, which itself resumes the app on API 28.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecurityViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val session = VaultSession { 0L }
    private val vm = SecurityViewModel(session, RecordingAnalyticsTracker())

    @Test
    fun `the first resume of the lock screen prompts`() {
        assertTrue(vm.shouldPromptOnResume())
    }

    @Test
    fun `a lock screen created in the background prompts when it is finally shown`() {
        vm.onEvent(SecurityEvents.OnStopped)
        assertTrue(vm.shouldPromptOnResume())
    }

    @Test
    fun `no second prompt while one is already showing`() {
        vm.onEvent(SecurityEvents.OnPromptStarted)
        assertFalse(vm.shouldPromptOnResume())
    }

    @Test
    fun `cancelling the system PIN screen does not immediately prompt again`() {
        vm.onEvent(SecurityEvents.OnPromptStarted)
        vm.onEvent(SecurityEvents.OnStopped) // the PIN screen covers the app; this is not leaving it
        vm.onEvent(SecurityEvents.OnAuthenticationError)
        assertFalse(vm.shouldPromptOnResume())
    }

    @Test
    fun `leaving the app and coming back prompts again after a cancel`() {
        vm.onEvent(SecurityEvents.OnPromptStarted)
        vm.onEvent(SecurityEvents.OnAuthenticationError)
        vm.onEvent(SecurityEvents.OnStopped)
        assertTrue(vm.shouldPromptOnResume())
    }

    @Test
    fun `the unlock button always works unless a prompt is already up`() {
        vm.onEvent(SecurityEvents.OnPromptStarted)
        vm.onEvent(SecurityEvents.OnAuthenticationError)
        assertTrue(vm.canPromptManually())
        vm.onEvent(SecurityEvents.OnPromptStarted)
        assertFalse(vm.canPromptManually())
    }

    @Test
    fun `once unlocked nothing prompts`() {
        vm.onEvent(SecurityEvents.OnPromptStarted)
        vm.onEvent(SecurityEvents.OnAuthenticationSucceeded)
        vm.onEvent(SecurityEvents.OnStopped)
        assertTrue(session.isUnlocked.value)
        assertFalse(vm.shouldPromptOnResume())
    }
}
