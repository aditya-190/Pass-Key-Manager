package com.bhardwaj.passkey.analytics

import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.testing.RecordingCrashReporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConsentApplierTest {
    private val analytics = RecordingAnalyticsTracker()
    private val crash = RecordingCrashReporter()
    private val consentModeCalls = mutableListOf<Boolean>()
    private val consentMode = ConsentModeSink { granted -> consentModeCalls += granted }
    private val performanceCalls = mutableListOf<Boolean>()
    private val performance = DiagnosticsSink { enabled -> performanceCalls += enabled }

    private fun applier(consent: MutableStateFlow<ConsentState>, scope: kotlinx.coroutines.CoroutineScope) =
        ConsentApplier(consent, analytics, crash, consentMode, performance, scope)

    @Test
    fun `undecided users collect nothing`() = runTest(UnconfinedTestDispatcher()) {
        applier(MutableStateFlow(ConsentState.Undecided), backgroundScope).start()
        assertEquals(false, analytics.collectionEnabled)
        assertEquals(false, crash.collectionEnabled)
        assertEquals(listOf(false), performanceCalls)
        assertEquals(listOf(false), consentModeCalls)
    }

    @Test
    fun `applies each choice to its own sink`() = runTest(UnconfinedTestDispatcher()) {
        val consent = MutableStateFlow(ConsentState.Undecided)
        applier(consent, backgroundScope).start()

        consent.value = ConsentState(decided = true, analytics = true, crash = false)
        assertEquals(true, analytics.collectionEnabled)
        assertEquals(false, crash.collectionEnabled)
        assertEquals(false, performanceCalls.last())
        assertEquals(true, consentModeCalls.last())
        assertEquals("true", analytics.userProperties[AppUserProps.CONSENT_ANALYTICS])

        consent.value = ConsentState(decided = true, analytics = false, crash = true)
        assertEquals(false, analytics.collectionEnabled)
        assertEquals(true, crash.collectionEnabled)
        assertEquals(true, performanceCalls.last())
        assertEquals(false, consentModeCalls.last())
    }

    @Test
    fun `reports captured before the user decides are kept, not deleted`() = runTest(UnconfinedTestDispatcher()) {
        applier(MutableStateFlow(ConsentState.Undecided), backgroundScope).start()
        assertEquals(0, crash.discardedUnsentReports)
    }

    @Test
    fun `an explicit no to crash reports deletes anything captured meanwhile`() = runTest(UnconfinedTestDispatcher()) {
        val consent = MutableStateFlow(ConsentState.Undecided)
        applier(consent, backgroundScope).start()
        consent.value = ConsentState(decided = true, analytics = true, crash = false)
        assertEquals(1, crash.discardedUnsentReports)
    }

    @Test
    fun `allowing crash reports keeps the pending ones so they are sent`() = runTest(UnconfinedTestDispatcher()) {
        val consent = MutableStateFlow(ConsentState.Undecided)
        applier(consent, backgroundScope).start()
        consent.value = ConsentState(decided = true, analytics = false, crash = true)
        assertEquals(0, crash.discardedUnsentReports)
        assertEquals(true, crash.collectionEnabled)
    }

    @Test
    fun `identical states are applied once`() = runTest(UnconfinedTestDispatcher()) {
        val state = ConsentState(decided = true, analytics = true, crash = true)
        val consent = MutableStateFlow(state)
        applier(consent, backgroundScope).start()
        consent.value = state.copy()
        assertEquals(1, consentModeCalls.size)
    }

    @Test
    fun `withdrawing consent turns everything off`() {
        val applier = applier(MutableStateFlow(ConsentState(true, true, true)), TestScope())
        applier.apply(ConsentState(decided = true, analytics = false, crash = false))
        assertEquals(false, analytics.collectionEnabled)
        assertEquals(false, crash.collectionEnabled)
        assertEquals(false, performanceCalls.last())
        assertEquals(false, consentModeCalls.last())
    }
}
