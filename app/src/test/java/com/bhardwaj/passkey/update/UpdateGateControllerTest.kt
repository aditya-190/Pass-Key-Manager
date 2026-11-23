package com.bhardwaj.passkey.update

import androidx.datastore.preferences.core.longPreferencesKey
import app.cash.turbine.test
import com.bhardwaj.passkey.analytics.BaselineEventNames
import com.bhardwaj.passkey.testing.FakeClock
import com.bhardwaj.passkey.testing.FakeConfig
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.testing.testDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateGateControllerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val analytics = RecordingAnalyticsTracker()
    private val clock = FakeClock()

    @Test
    fun `gate follows config and logs hard and maintenance once`() = runTest(UnconfinedTestDispatcher()) {
        val config = FakeConfig()
        val controller =
            UpdateGateController(config.flow, 5, testDataStore(tmp.root), analytics, clock, backgroundScope)
        assertEquals(UpdateDecision.None, controller.gate.value)
        config.set(mapOf("force_update_enabled" to true, "min_supported_version_code" to 6L))
        assertEquals(UpdateDecision.Hard, controller.gate.value)
        config.set(mapOf("maintenance_mode" to true))
        assertEquals(UpdateDecision.Maintenance, controller.gate.value)
        assertEquals(
            listOf(BaselineEventNames.FORCE_UPDATE_SHOWN, BaselineEventNames.MAINTENANCE_SHOWN),
            analytics.names
        )
    }

    @Test
    fun `soft prompt is due once, then throttled after it was handled`() = runTest(UnconfinedTestDispatcher()) {
        val store = testDataStore(tmp.root)
        val config = FakeConfig(mapOf("latest_version_code" to 9L))
        val controller = UpdateGateController(config.flow, 5, store, analytics, clock, backgroundScope)
        controller.softPromptDue.test {
            assertFalse(awaitItem())
            controller.onMainScreenVisible()
            assertTrue(awaitItem())
            controller.onSoftPromptHandled(dismissed = true)
            assertFalse(awaitItem())
        }
        val saved = store.data.first { it[longPreferencesKey("soft_update_dismissed_version")] != null }
        assertEquals(9L, saved[longPreferencesKey("soft_update_dismissed_version")])
        assertTrue(BaselineEventNames.SOFT_UPDATE_DISMISSED in analytics.names)

        // Next launch, same version, within the interval: not due.
        val next = UpdateGateController(config.flow, 5, store, analytics, clock, backgroundScope)
        next.onMainScreenVisible()
        assertFalse(next.softPromptDue.value)

        // After soft_update_interval_days (default 3) it is due again.
        clock.now += 3 * ForceUpdateManager.DAY_MS
        val later = UpdateGateController(config.flow, 5, store, analytics, clock, backgroundScope)
        later.softPromptDue.test {
            assertFalse(awaitItem())
            later.onMainScreenVisible()
            assertTrue(awaitItem())
        }
    }
}
