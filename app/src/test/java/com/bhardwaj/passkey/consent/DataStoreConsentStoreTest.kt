package com.bhardwaj.passkey.consent

import com.bhardwaj.passkey.analytics.ConsentState
import com.bhardwaj.passkey.testing.testDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreConsentStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `a fresh install is undecided with everything off`() = runTest {
        val store = DataStoreConsentStore(testDataStore(tmp.root))
        assertEquals(ConsentState.Undecided, store.consent.first())
    }

    @Test
    fun `saving records the choice and marks it decided`() = runTest {
        val store = DataStoreConsentStore(testDataStore(tmp.root))
        store.save(analytics = false, crash = true)
        assertEquals(ConsentState(decided = true, analytics = false, crash = true), store.consent.first())
    }

    @Test
    fun `the choice survives a new store over the same file`() = runTest {
        val firstProcess = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
        DataStoreConsentStore(testDataStore(tmp.root, firstProcess)).save(analytics = true, crash = true)
        // Closing the first DataStore and opening a new one over the same file is how a process
        // restart looks to this code. (DataStore forbids two live instances on one file.)
        firstProcess.cancel()
        val reopened = DataStoreConsentStore(testDataStore(tmp.root))
        assertEquals(ConsentState(decided = true, analytics = true, crash = true), reopened.consent.first())
    }
}
