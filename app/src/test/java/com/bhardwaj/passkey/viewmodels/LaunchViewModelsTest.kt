package com.bhardwaj.passkey.viewmodels

import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.ConsentState
import com.bhardwaj.passkey.consent.FakeConsentStore
import com.bhardwaj.passkey.data.datastore.PreferencesRepositoryImpl
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingEffect
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingEvents
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingViewModel
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashEffect
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashEvents
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashViewModel
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.testing.testDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val analytics = RecordingAnalyticsTracker()

    private fun prefs() = PreferencesRepositoryImpl(testDataStore(tmp.root))

    @Test
    fun `splash sends a new user to onboarding`() = runTest {
        val vm = SplashViewModel(prefs(), FakeConsentStore())
        vm.onEvent(SplashEvents.OnLoadingComplete)
        assertEquals(SplashEffect.Navigate(NavRoute.Onboarding), vm.effects.first())
    }

    @Test
    fun `splash asks an upgrading user for consent once`() = runTest {
        val prefs = prefs().apply { setOnboardingCompleted(true) }
        val vm = SplashViewModel(prefs, FakeConsentStore())
        vm.onEvent(SplashEvents.OnLoadingComplete)
        assertEquals(SplashEffect.Navigate(NavRoute.Consent()), vm.effects.first())
    }

    @Test
    fun `splash navigates exactly once even if preferences change later`() = runTest {
        val prefs = prefs().apply { setOnboardingCompleted(true) }
        val consent = FakeConsentStore(ConsentState(true, analytics = false, crash = false))
        val vm = SplashViewModel(prefs, consent)
        val received = mutableListOf<SplashEffect>()
        val job = backgroundScope.launch { vm.effects.toList(received) }
        vm.onEvent(SplashEvents.OnLoadingComplete)
        // Before 5.5.2 the splash kept collecting DataStore and re-navigated on every write.
        prefs.setOnboardingCompleted(false)
        consent.save(analytics = true, crash = true)
        assertEquals(listOf<SplashEffect>(SplashEffect.Navigate(NavRoute.Security)), received)
        job.cancel()
    }

    @Test
    fun `finishing onboarding records it and goes to consent`() = runTest {
        val prefs = prefs()
        val vm = OnBoardingViewModel(prefs, FakeConsentStore(), analytics)
        vm.onEvent(OnBoardingEvents.OnBoardingComplete)
        assertEquals(OnBoardingEffect.Navigate(NavRoute.Consent()), vm.effects.first())
        assertTrue(prefs.onboardingCompleted.first())
        assertTrue(AppEventNames.ONBOARDING_COMPLETE in analytics.names)
    }

    @Test
    fun `skipping logs the page it was skipped from`() = runTest {
        val vm = OnBoardingViewModel(prefs(), FakeConsentStore(ConsentState(true, true, true)), analytics)
        vm.onEvent(OnBoardingEvents.OnPageViewed(page = 1))
        vm.onEvent(OnBoardingEvents.OnSkip(page = 1))
        assertEquals(OnBoardingEffect.Navigate(NavRoute.Security), vm.effects.first())
        assertEquals(
            listOf(AppEventNames.ONBOARDING_PAGE_VIEW, AppEventNames.ONBOARDING_SKIPPED),
            analytics.names
        )
        // Pages are 1-based in analytics.
        assertEquals(2, analytics.events.last().params.values.single())
    }
}
