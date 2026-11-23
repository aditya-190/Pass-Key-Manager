package com.bhardwaj.passkey.onboarding

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.data.security.LockPolicy
import com.bhardwaj.passkey.data.security.LockReason
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.navigation.routeName
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEffect
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEvent
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnboardingPage
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnboardingPages
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 6.0.0 feature tour: part of first-run onboarding, and replayable on its own from Settings. */
@OptIn(ExperimentalCoroutinesApi::class)
class FeatureTourTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val analytics = RecordingAnalyticsTracker()

    @Test
    fun `first run shows the three intro pages, then the six tour pages`() {
        assertEquals(
            listOf(OnboardingPage.GENERATE, OnboardingPage.ALL_HERE, OnboardingPage.ONLY_YOU) + OnboardingPages.tour,
            OnboardingPages.firstRun
        )
        assertEquals(
            listOf(
                OnboardingPage.TOUR_ENTRIES,
                OnboardingPage.TOUR_AUTOFILL,
                OnboardingPage.TOUR_CODES,
                OnboardingPage.TOUR_GENERATOR,
                OnboardingPage.TOUR_BACKUP,
                OnboardingPage.TOUR_PRIVACY
            ),
            OnboardingPages.tour
        )
    }

    @Test
    fun `every page has its own heading and description`() {
        val ids = OnboardingPage.entries.flatMap { listOf(it.heading, it.description) }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `opening the tour is counted once, finishing closes it once`() = runTest {
        val vm =
            FeatureTourViewModel(
                analytics,
                SavedStateHandle(),
                com.bhardwaj.passkey.review.ReviewPrompter(
                    com.bhardwaj.passkey.testing.FakeReviewPromptStore(),
                    com.bhardwaj.passkey.core.Clock {
                        0L
                    },
                    com.bhardwaj.passkey.testing.FixedConfigProvider(),
                    RecordingAnalyticsTracker()
                )
            )
        vm.onEvent(FeatureTourEvent.Complete)
        vm.onEvent(FeatureTourEvent.Complete)
        assertEquals(FeatureTourEffect.Close, vm.effects.first())
        assertEquals(
            listOf(AppEventNames.FEATURE_TOUR_STARTED, AppEventNames.FEATURE_TOUR_COMPLETED),
            analytics.names
        )
    }

    @Test
    fun `skipping closes the tour and logs the 1-based page`() = runTest {
        val vm =
            FeatureTourViewModel(
                analytics,
                SavedStateHandle(),
                com.bhardwaj.passkey.review.ReviewPrompter(
                    com.bhardwaj.passkey.testing.FakeReviewPromptStore(),
                    com.bhardwaj.passkey.core.Clock {
                        0L
                    },
                    com.bhardwaj.passkey.testing.FixedConfigProvider(),
                    RecordingAnalyticsTracker()
                )
            )
        vm.onEvent(FeatureTourEvent.Skip(page = 2))
        vm.onEvent(FeatureTourEvent.Complete)
        assertEquals(FeatureTourEffect.Close, vm.effects.first())
        assertEquals(AppEventNames.FEATURE_TOUR_SKIPPED, analytics.names.last())
        assertEquals(mapOf(AppParams.PAGE to 3), analytics.events.last().params)
    }

    @Test
    fun `a tour restored after process death is not counted as a new one`() {
        // Attack-pass #4: Android recreates the ViewModel from saved state; the opening was already counted.
        val handle = SavedStateHandle()
        FeatureTourViewModel(
            analytics,
            handle,
            com.bhardwaj.passkey.review.ReviewPrompter(
                com.bhardwaj.passkey.testing.FakeReviewPromptStore(),
                com.bhardwaj.passkey.core.Clock {
                    0L
                },
                com.bhardwaj.passkey.testing.FixedConfigProvider(),
                RecordingAnalyticsTracker()
            )
        )
        FeatureTourViewModel(
            analytics,
            SavedStateHandle(
                handle.keys().associateWith {
                    handle.get<Any>(it)
                }
            ),
            com.bhardwaj.passkey.review.ReviewPrompter(
                com.bhardwaj.passkey.testing.FakeReviewPromptStore(),
                com.bhardwaj.passkey.core.Clock {
                    0L
                },
                com.bhardwaj.passkey.testing.FixedConfigProvider(),
                RecordingAnalyticsTracker()
            )
        )
        assertEquals(listOf(AppEventNames.FEATURE_TOUR_STARTED), analytics.names)
    }

    @Test
    fun `tour events pass the Firebase limits and carry no free text`() {
        listOf(AppEvents.featureTourStarted(), AppEvents.featureTourCompleted(), AppEvents.featureTourSkipped(4))
            .forEach { assertEquals(it.name, emptyList<String>(), EventValidator.validate(it)) }
    }

    @Test
    fun `the replayed tour sits behind the lock like any vault screen`() {
        assertTrue(LockPolicy.mustShowLockScreen(false, LockReason.ColdStart, routeName<NavRoute.FeatureTour>()))
    }

    @Test
    fun `every language has the tour, and each heading highlights exactly one phrase`() {
        val res = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val locales = res.listFiles()!!.filter { it.name == "values" || it.name.matches(Regex("values-[a-z]{2}")) }
        assertEquals(17, locales.size)
        val names = OnboardingPage.entries.filter { it.isTour }.flatMap {
            listOf("tour_${it.key}_heading", "tour_${it.key}_description")
        } + "feature_tour"
        locales.forEach { dir ->
            val xml = File(dir, "strings.xml").readText()
            names.forEach { name ->
                val value = Regex("<string name=\"$name\">(.*?)</string>", RegexOption.DOT_MATCHES_ALL)
                    .find(xml)?.groupValues?.get(1)
                assertTrue("${dir.name} lacks $name", !value.isNullOrBlank())
                if (name.endsWith("_heading")) {
                    assertEquals("${dir.name}/$name", 1, Regex("\\[\\[[^\\]]+]]").findAll(value!!).count())
                }
            }
        }
    }

    private val OnboardingPage.key: String get() = name.removePrefix("TOUR_").lowercase()

    @Test
    fun `finishing the replayed tour may ask for a review, then closes`() = runTest {
        // 6.0.0: a good moment, under the same pacing as every other.
        val now = 1_800_000_000_000L
        val store = com.bhardwaj.passkey.testing.FakeReviewPromptStore(
            com.bhardwaj.passkey.review.ReviewPromptState(firstSeenAt = now - 10 * 86_400_000L)
        )
        val prompter = com.bhardwaj.passkey.review.ReviewPrompter(
            store,
            com.bhardwaj.passkey.core.Clock {
                now
            },
            com.bhardwaj.passkey.testing.FixedConfigProvider(),
            analytics
        )
        val vm = FeatureTourViewModel(analytics, SavedStateHandle(), prompter)
        vm.onIntent2Complete()
        assertEquals(FeatureTourEffect.RequestReview, vm.effects.first())
        assertEquals(FeatureTourEffect.Close, vm.effects.first())
    }

    private fun FeatureTourViewModel.onIntent2Complete() = onEvent(FeatureTourEvent.Complete)
}
