package com.bhardwaj.passkey.legal

import app.cash.turbine.test
import com.bhardwaj.passkey.analytics.BaselineEventNames
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.MapConfigReader
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LegalViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val analytics = RecordingAnalyticsTracker()
    private val texts = LegalTexts()

    @Test
    fun `shows the in-app text with its date and logs the view`() {
        val vm = LegalViewModel(texts.privacy, "https://example.com/privacy", analytics)
        val state = vm.state.value
        assertEquals(LegalDocument.PRIVACY, state.document)
        assertEquals(texts.privacy.lastUpdated, state.lastUpdated)
        assertTrue(state.sections.isNotEmpty())
        assertEquals("https://example.com/privacy", state.hostedUrl)
        assertEquals(listOf(BaselineEventNames.LEGAL_VIEWED), analytics.names)
        assertEquals("privacy", analytics.events.single().params["document"])
    }

    @Test
    fun `read online opens the hosted page`() = runTest {
        val vm = LegalViewModel(texts.terms, " https://example.com/terms ", analytics)
        vm.effects.test {
            vm.onIntent(LegalIntent.OpenOnline)
            assertEquals(LegalEffect.OpenUrl("https://example.com/terms"), awaitItem())
        }
        assertTrue(BaselineEventNames.LEGAL_OPENED_ONLINE in analytics.names)
    }

    @Test
    fun `blank or unsafe hosted urls hide the button`() = runTest {
        listOf("", "http://example.com", "javascript:alert(1)").forEach { url ->
            val vm = LegalViewModel(texts.terms, url, analytics)
            assertNull(vm.state.value.hostedUrl)
            vm.effects.test {
                vm.onIntent(LegalIntent.OpenOnline)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `optional sections are composed in only when passed`() {
        val extra = LegalSection(heading = 1, body = 2)
        val base = LegalTexts()
        val withExtras = LegalTexts(privacyExtras = listOf(extra), termsExtras = listOf(extra))
        assertFalse(extra in base.privacy.sections || extra in base.terms.sections)
        assertEquals(base.privacy.sections.size + 1, withExtras.privacy.sections.size)
        assertEquals(4, withExtras.privacy.sections.indexOf(extra)) // after intro, device, autofill, analytics
        assertEquals(1, withExtras.terms.sections.indexOf(extra)) // after "Using the app"
        assertEquals(base.privacy.sections.last(), withExtras.privacy.sections.last()) // Contact stays last
    }

    @Test
    fun `nav argument and remote config resolve the document and its hosted url`() {
        assertEquals(LegalDocument.TERMS, LegalViewModel.documentOf("TERMS"))
        assertEquals(LegalDocument.TERMS, LegalViewModel.documentOf("terms"))
        assertEquals(LegalDocument.PRIVACY, LegalViewModel.documentOf(null))
        assertEquals(LegalDocument.PRIVACY, LegalViewModel.documentOf("bogus"))
        val config = AppConfig(
            MapConfigReader(
                mapOf(
                    "terms_url" to "https://x.test/t",
                    "privacy_url" to "https://x.test/p"
                )
            )
        )
        assertEquals("https://x.test/t", LegalViewModel.hostedUrlFor(LegalDocument.TERMS, config))
        assertEquals("https://x.test/p", LegalViewModel.hostedUrlFor(LegalDocument.PRIVACY, config))
    }
}
