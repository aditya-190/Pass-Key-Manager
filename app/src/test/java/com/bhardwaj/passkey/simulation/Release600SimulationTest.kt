package com.bhardwaj.passkey.simulation

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEffect
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEvent
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnboardingPages
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Feature workflow step 3 for 6.0.0 (feature tour): 1,000 seeded users mash Skip and Next in random orders,
 * including double taps. Every assertion names its seed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Release600SimulationTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `a replayed tour always ends exactly once, with one clean analytics trail`() = runTest {
        val pages = OnboardingPages.tour.size
        repeat(SEEDS) { seed ->
            val random = Random(seed)
            val analytics = RecordingAnalyticsTracker()
            val vm = FeatureTourViewModel(analytics, SavedStateHandle())
            val effects = mutableListOf<FeatureTourEffect>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.effects.toList(effects)
            }
            repeat(random.nextInt(1, 12)) {
                if (random.nextBoolean()) {
                    vm.onEvent(FeatureTourEvent.Skip(random.nextInt(pages)))
                } else {
                    vm.onEvent(FeatureTourEvent.Complete)
                }
            }
            advanceUntilIdle()
            collector.cancel()

            assertEquals("seed $seed", listOf(FeatureTourEffect.Close), effects)
            assertEquals("seed $seed", AppEventNames.FEATURE_TOUR_STARTED, analytics.names.first())
            assertEquals("seed $seed", 2, analytics.events.size)
            val end = analytics.events.last()
            assertTrue(
                "seed $seed",
                end.name in setOf(AppEventNames.FEATURE_TOUR_COMPLETED, AppEventNames.FEATURE_TOUR_SKIPPED)
            )
            (end.params[AppParams.PAGE] as Int?)?.let { assertTrue("seed $seed", it in 1..pages) }
            analytics.events.forEach { assertEquals("seed $seed", emptyList<String>(), EventValidator.validate(it)) }
        }
    }

    private companion object {
        const val SEEDS = 1_000
    }
}
