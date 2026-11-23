package com.bhardwaj.passkey

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.data.repository.passkeyDataStore
import com.bhardwaj.passkey.presentation.MainActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real app through its launch order: onboarding → consent → unlock.
 *
 * App data is never wiped (studio rule), so the test reads the stored state first and asserts the path
 * that state must take. On a fresh install it walks the whole flow; on later runs it proves an
 * onboarded, decided user goes straight to unlock and is never asked again.
 */
@RunWith(AndroidJUnit4::class)
class LaunchFlowE2ETest {
    // Opt-in since 5.7.0 (-e launchFlow true): the flow ends at real system authentication (PIN / biometric),
    // which only a host script can answer (scripts/e2e/unlock_once.sh); a plain run must not leave it up.
    @get:Rule
    val compose = gateComposeRule(MainActivity::class.java) {
        InstrumentationRegistry.getArguments().getString("launchFlow") == "true"
    }

    private val timeoutMs = 10_000L

    private fun string(id: Int) = compose.activity.getString(id)

    private fun prefs() = runBlocking { compose.activity.applicationContext.passkeyDataStore.data.first() }

    private fun <A : ComponentActivity> AndroidComposeTestRule<GateActivityRule<A>, A>.awaitText(text: String) =
        waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun launchFollowsOnboardingThenConsentThenUnlock() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("launchFlow") == "true")
        val before = prefs()
        val onboarded = before[booleanPreferencesKey("onboarding_complete")] ?: false
        val decided = before[booleanPreferencesKey("consent_decided")] ?: false

        if (!onboarded) {
            compose.awaitText(string(R.string.skip))
            compose.onNodeWithText(string(R.string.skip)).performClick()
        }

        if (!decided) {
            compose.awaitText(string(R.string.consent_decline_all))
            // Nothing is pre-ticked.
            val switches = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
            compose.onAllNodes(switches).fetchSemanticsNodes().indices.forEach { index ->
                compose.onAllNodes(switches)[index].assertIsOff()
            }
            compose.onNodeWithText(string(R.string.consent_decline_all)).performClick()
        }

        // Unlock, reached only after the consent decision. On a device with a screen lock the system
        // credential screen may already cover the app, which also means the unlock step was reached.
        // runCatching: "no compose hierarchy" means the credential screen covers the app, i.e. unlock was reached.
        // Past onboarding and consent: the vault gate is in front (recovery setup, unlock prompt, a spinner while
        // the system prompt is up, or the system PIN screen covering the app).
        compose.waitUntil(timeoutMs) {
            runCatching {
                listOf(R.string.skip, R.string.consent_decline_all)
                    .none { compose.onAllNodesWithText(string(it)).fetchSemanticsNodes().isNotEmpty() }
            }.getOrDefault(true)
        }
        val consentStillShown = runCatching {
            compose.onAllNodes(hasText(string(R.string.consent_decline_all))).fetchSemanticsNodes().isNotEmpty()
        }.getOrDefault(false)
        assertThat(consentStillShown).isFalse()

        val after = prefs()
        assertThat(after[booleanPreferencesKey("onboarding_complete")]).isTrue()
        assertThat(after[booleanPreferencesKey("consent_decided")]).isTrue()
        if (!decided) {
            assertThat(after[booleanPreferencesKey("consent_analytics")]).isFalse()
            assertThat(after[booleanPreferencesKey("consent_crash")]).isFalse()
        }
    }
}
