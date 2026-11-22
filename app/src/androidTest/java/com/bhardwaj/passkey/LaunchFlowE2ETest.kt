package com.bhardwaj.passkey

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhardwaj.passkey.data.repository.passkeyDataStore
import com.bhardwaj.passkey.presentation.MainActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val timeoutMs = 10_000L

    private fun string(id: Int) = compose.activity.getString(id)

    private fun prefs() = runBlocking { compose.activity.applicationContext.passkeyDataStore.data.first() }

    private fun <A : ComponentActivity> AndroidComposeTestRule<ActivityScenarioRule<A>, A>.awaitText(text: String) =
        waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun launchFollowsOnboardingThenConsentThenUnlock() {
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
        compose.waitUntil(timeoutMs) {
            runCatching {
                compose.onAllNodesWithText(string(R.string.welcome_to_pass_key)).fetchSemanticsNodes().isNotEmpty()
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
