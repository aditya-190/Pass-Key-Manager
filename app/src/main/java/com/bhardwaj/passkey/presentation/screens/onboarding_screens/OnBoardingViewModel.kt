package com.bhardwaj.passkey.presentation.screens.onboarding_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.consent.ConsentStore
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.launch.LaunchRouter
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.navigation.route
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val consentStore: ConsentStore,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    // BUFFERED, not the RENDEZVOUS default: with lifecycle-aware collection a backgrounded
    // screen has no active collector, and a rendezvous channel would suspend the coroutine
    // that emitted the effect until the user came back.
    private val _effects = Channel<OnBoardingEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var finished = false

    fun onEvent(event: OnBoardingEvents) {
        when (event) {
            // Pages arrive 0-based from the pager and are logged 1-based.
            is OnBoardingEvents.OnPageViewed -> analytics.log(AppEvents.onboardingPageView(event.page + 1))
            is OnBoardingEvents.OnSkip -> {
                if (finished) return
                analytics.log(AppEvents.onboardingSkipped(event.page + 1))
                finish()
            }
            OnBoardingEvents.OnBoardingComplete -> {
                if (finished) return
                analytics.log(AppEvents.onboardingComplete())
                finish()
            }
        }
    }

    private fun finish() {
        finished = true
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            val step = LaunchRouter.afterOnboarding(consentDecided = consentStore.consent.first().decided)
            _effects.send(OnBoardingEffect.Navigate(step.route))
        }
    }
}

sealed interface OnBoardingEffect {
    data class Navigate(val route: NavRoute) : OnBoardingEffect
}
