package com.bhardwaj.passkey.simulation

import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.ConsentState
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.consent.ConsentEffect
import com.bhardwaj.passkey.consent.ConsentIntent
import com.bhardwaj.passkey.consent.ConsentViewModel
import com.bhardwaj.passkey.consent.FakeConsentStore
import com.bhardwaj.passkey.launch.LaunchRouter
import com.bhardwaj.passkey.launch.LaunchStep
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.parseHighlightMarkup
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.Categories
import com.bhardwaj.passkey.utils.PasswordGenerator
import java.util.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Feature workflow step 3 for 5.5.2: seeded randomised runs through the pure logic this release touches.
 * Every failure message carries its seed so it can be replayed exactly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Release552SimulationTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val runs = 2_000

    @Test
    fun `generator honours length and every selected class across seeded configs`() {
        repeat(runs) { seed ->
            val rnd = Random(seed.toLong())
            val upper = rnd.nextBoolean()
            val lower = rnd.nextBoolean()
            val digits = rnd.nextBoolean()
            val special = rnd.nextBoolean()
            val length = rnd.nextInt(70) - 5 // includes zero and negative lengths
            val pw = PasswordGenerator.generate(length, upper, lower, digits, special, random = Random(seed * 31L))
            val classes = listOf(upper, lower, digits, special).count { it }.coerceAtLeast(1)
            assertEquals("seed $seed", maxOf(length, classes), pw.length)
            if (upper) assertTrue("seed $seed upper", pw.any { it.isUpperCase() })
            val fallsBackToLower = !(upper || lower || digits || special)
            if (lower || fallsBackToLower) {
                assertTrue("seed $seed lower", pw.any { it.isLowerCase() })
            }
            if (digits) assertTrue("seed $seed digit", pw.any { it.isDigit() })
            if (special) assertTrue("seed $seed special", pw.any { !it.isLetterOrDigit() })
            if (!upper) assertTrue("seed $seed no upper", pw.none { it.isUpperCase() })
            if (!digits) assertTrue("seed $seed no digit", pw.none { it.isDigit() })
            if (!special) assertTrue("seed $seed no special", pw.all { it.isLetterOrDigit() })
        }
    }

    @Test
    fun `highlight parser never throws and spans always fit the text`() {
        val alphabet = "[]ab ç中ع\n🔐"
        repeat(runs * 5) { seed ->
            val rnd = Random(seed.toLong())
            val raw = buildString { repeat(rnd.nextInt(40)) { append(alphabet[rnd.nextInt(alphabet.length)]) } }
            val parsed = parseHighlightMarkup(raw)
            assertTrue("seed $seed", parsed.text.length <= raw.length)
            parsed.spans.forEach { span ->
                assertTrue("seed $seed span $span in '${parsed.text}'", span.start in 0 until span.endExclusive)
                assertTrue("seed $seed span $span", span.endExclusive <= parsed.text.length)
            }
        }
    }

    @Test
    fun `every analytics event built from random inputs is valid and leaks no argument`() {
        repeat(runs) { seed ->
            val rnd = Random(seed.toLong())
            val secretId = 1_000_000 + rnd.nextInt(9_000_000)
            val events = listOf(
                AppEvents.passwordGenerated(rnd.nextInt()),
                AppEvents.backupExported(rnd.nextInt()),
                AppEvents.backupImported(rnd.nextBoolean(), rnd.nextInt()),
                AppEvents.safetyCheckViewed(rnd.nextInt()),
                AppEvents.onboardingPageView(rnd.nextInt(3) + 1),
                AppEvents.entryCreated(Categories.entries[rnd.nextInt(Categories.entries.size)]),
                AppEvents.screenView("details_page?previewId=$secretId"),
                AppEvents.languageChanged("x".repeat(rnd.nextInt(300)))
            )
            events.forEach { event ->
                assertEquals("seed $seed ${event.name}", emptyList<String>(), EventValidator.validate(event))
                assertTrue("seed $seed ${event.name}", event.params.values.none { "$secretId" in it.toString() })
            }
        }
    }

    @Test
    fun `launch routing is total and consent always precedes unlock`() {
        repeat(runs) { seed ->
            val rnd = Random(seed.toLong())
            val onboarded = rnd.nextBoolean()
            val decided = rnd.nextBoolean()
            val step = LaunchRouter.atStart(onboarded, decided)
            val expected = when {
                !onboarded -> LaunchStep.Onboarding
                !decided -> LaunchStep.Consent
                else -> LaunchStep.Unlock
            }
            assertEquals("seed $seed", expected, step)
            if (step == LaunchStep.Unlock) assertTrue("seed $seed", decided)
        }
    }

    @Test
    fun `consent screen under random tapping saves once and stores the first decision`() = runTest {
        repeat(runs) { seed ->
            val rnd = Random(seed.toLong())
            val initial = if (rnd.nextBoolean()) {
                ConsentState.Undecided
            } else {
                ConsentState(
                    true,
                    rnd.nextBoolean(),
                    rnd.nextBoolean()
                )
            }
            val store = FakeConsentStore(initial)
            val vm = ConsentViewModel(store, RecordingAnalyticsTracker())
            val effects = mutableListOf<ConsentEffect>()
            val job = backgroundScope.launch { vm.effects.toList(effects) }

            val session = ConsentSession(initial)
            repeat(rnd.nextInt(30)) { vm.onIntent(session.next(rnd)) }

            runCurrent() // let the effect collector drain the channel
            assertTrue("seed $seed saved ${store.saves} times", store.saves <= 1)
            assertEquals("seed $seed", session.firstDecision ?: initial, store.consent.value)
            assertEquals("seed $seed effects", if (session.firstDecision == null) 0 else 1, effects.size)
            job.cancel()
        }
    }

    /** Models a user tapping at random, and what the first save should have stored. */
    private class ConsentSession(initial: ConsentState) {
        private var analytics = initial.analytics
        private var crash = initial.crash
        var firstDecision: ConsentState? = null
            private set

        fun next(rnd: Random): ConsentIntent = when (rnd.nextInt(5)) {
            0 -> ConsentIntent.SetAnalytics(rnd.nextBoolean()).also { analytics = it.enabled }
            1 -> ConsentIntent.SetCrash(rnd.nextBoolean()).also { crash = it.enabled }
            2 -> ConsentIntent.Save.also { decide(ConsentState(true, analytics, crash)) }
            3 -> ConsentIntent.AllowAll.also { decide(ConsentState(true, analytics = true, crash = true)) }
            else -> ConsentIntent.DeclineAll.also { decide(ConsentState(true, analytics = false, crash = false)) }
        }

        private fun decide(state: ConsentState) {
            if (firstDecision == null) firstDecision = state
        }
    }
}
