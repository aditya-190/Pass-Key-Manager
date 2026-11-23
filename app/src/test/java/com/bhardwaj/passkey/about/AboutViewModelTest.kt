package com.bhardwaj.passkey.about

import app.cash.turbine.test
import com.bhardwaj.passkey.analytics.BaselineEventNames
import com.bhardwaj.passkey.testing.FakeConfig
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AboutViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val app = AppInfo("Sample", "1.2.0", 12, "com.example.sample")
    private val analytics = RecordingAnalyticsTracker()
    private val listing = "https://play.google.com/store/apps/details?id=com.example.sample"

    @Test
    fun `the shipped defaults offer PassKey's support email`() {
        assertEquals("yrkkh.cclub@gmail.com", vm().state.value.contactEmail)
    }

    private fun vm(overrides: Map<String, Any> = emptyMap()) = AboutViewModel(app, FakeConfig(overrides), analytics)

    @Test
    fun `state exposes version and hides unset links`() {
        val state = vm(mapOf("support_email" to "")).state.value
        assertEquals("1.2.0", state.app.versionName)
        assertEquals(12L, state.app.versionCode)
        assertEquals(listing, state.storeUrl)
        assertNull(state.contactEmail)
        assertNull(state.moreAppsUrl)
    }

    @Test
    fun `invalid remote values are ignored`() {
        val state = vm(mapOf("support_email" to "not-an-email", "more_apps_url" to "http://insecure.test")).state.value
        assertNull(state.contactEmail)
        assertNull(state.moreAppsUrl)
    }

    @Test
    fun `rate tries in-app review first, then the store listing`() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(AboutIntent.Clicked(AboutAction.RATE))
            assertEquals(AboutEffect.LaunchInAppReview, awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.RATE))
            assertEquals(AboutEffect.OpenUrl(listing), awaitItem())
            vm.onIntent(AboutIntent.ReviewUnavailable)
            assertEquals(AboutEffect.OpenUrl(listing), awaitItem())
        }
    }

    @Test
    fun `every row maps to its effect and is logged`() = runTest {
        val vm = vm(
            mapOf(
                "support_email" to "hi@example.com",
                "more_apps_url" to "https://play.google.com/store/apps/dev?id=1"
            )
        )
        vm.effects.test {
            vm.onIntent(AboutIntent.Clicked(AboutAction.SHARE))
            assertEquals(AboutEffect.Share(listing), awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.CONTACT))
            assertEquals(AboutEffect.Email("hi@example.com"), awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.MORE_APPS))
            assertEquals(AboutEffect.OpenUrl("https://play.google.com/store/apps/dev?id=1"), awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.LICENCES))
            assertEquals(AboutEffect.OpenLicences, awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.TERMS))
            assertEquals(AboutEffect.OpenTerms, awaitItem())
            vm.onIntent(AboutIntent.Clicked(AboutAction.PRIVACY))
            assertEquals(AboutEffect.OpenPrivacy, awaitItem())
        }
        assertEquals(6, analytics.names.count { it == BaselineEventNames.ABOUT_ACTION })
        val actions = analytics.events.map { it.params["action"] }
        assertTrue(actions.containsAll(listOf("share", "contact", "more_apps", "licences", "terms", "privacy")))
    }

    @Test
    fun `hidden rows do nothing`() = runTest {
        // PassKey ships a default support email; blank it to exercise the hidden-row path.
        val vm = vm(mapOf("support_email" to ""))
        vm.effects.test {
            vm.onIntent(AboutIntent.Clicked(AboutAction.CONTACT))
            vm.onIntent(AboutIntent.Clicked(AboutAction.MORE_APPS))
            expectNoEvents()
        }
        assertTrue(analytics.names.isEmpty())
    }
}
