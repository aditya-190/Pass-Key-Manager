package com.bhardwaj.passkey.consent

import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.ConsentState
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeConsentStore(initial: ConsentState = ConsentState.Undecided) : ConsentStore {
    override val consent = MutableStateFlow(initial)
    var saves = 0

    override suspend fun save(analytics: Boolean, crash: Boolean) {
        saves++
        consent.value = ConsentState(decided = true, analytics = analytics, crash = crash)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = RecordingAnalyticsTracker()

    @Test
    fun `first run starts with both switches off - nothing is pre-ticked`() {
        val vm = ConsentViewModel(FakeConsentStore(), analytics)
        assertEquals(ConsentUiState(analytics = false, crash = false, isLoading = false), vm.state.value)
    }

    @Test
    fun `privacy options show the stored choice`() {
        val vm = ConsentViewModel(FakeConsentStore(ConsentState(true, analytics = true, crash = false)), analytics)
        assertTrue(vm.state.value.analytics)
        assertFalse(vm.state.value.crash)
    }

    @Test
    fun `toggles change state without saving`() {
        val store = FakeConsentStore()
        val vm = ConsentViewModel(store, analytics)
        vm.onIntent(ConsentIntent.SetAnalytics(true))
        vm.onIntent(ConsentIntent.SetCrash(true))
        assertTrue(vm.state.value.analytics && vm.state.value.crash)
        assertEquals(0, store.saves)
    }

    @Test
    fun `save stores the switches and finishes`() = runTest {
        val store = FakeConsentStore()
        val vm = ConsentViewModel(store, analytics)
        vm.onIntent(ConsentIntent.SetCrash(true))
        vm.onIntent(ConsentIntent.Save)
        assertEquals(ConsentState(decided = true, analytics = false, crash = true), store.consent.value)
        assertEquals(ConsentEffect.Finished, vm.effects.first())
    }

    @Test
    fun `allow all and decline all are one tap each`() = runTest {
        val allowStore = FakeConsentStore()
        ConsentViewModel(allowStore, analytics).onIntent(ConsentIntent.AllowAll)
        assertEquals(ConsentState(true, analytics = true, crash = true), allowStore.consent.value)

        val declineStore = FakeConsentStore(ConsentState(true, analytics = true, crash = true))
        ConsentViewModel(declineStore, analytics).onIntent(ConsentIntent.DeclineAll)
        assertEquals(ConsentState(true, analytics = false, crash = false), declineStore.consent.value)
    }

    @Test
    fun `rapid double taps save once`() {
        val store = FakeConsentStore()
        val vm = ConsentViewModel(store, analytics)
        vm.onIntent(ConsentIntent.AllowAll)
        vm.onIntent(ConsentIntent.AllowAll)
        vm.onIntent(ConsentIntent.Save)
        assertEquals(1, store.saves)
    }

    @Test
    fun `the decision is logged without any personal data`() {
        val vm = ConsentViewModel(FakeConsentStore(), analytics)
        vm.onIntent(ConsentIntent.AllowAll)
        val event = analytics.events.single()
        assertEquals(AppEventNames.CONSENT_UPDATED, event.name)
        assertTrue(event.params.values.all { it is Boolean })
    }
}
