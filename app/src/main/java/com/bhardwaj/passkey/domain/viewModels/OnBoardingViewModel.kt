package com.bhardwaj.passkey.domain.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.consent.ConsentStore
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.domain.events.OnBoardingEvents
import com.bhardwaj.passkey.launch.LaunchRouter
import com.bhardwaj.passkey.presentation.navigation.route
import com.bhardwaj.passkey.utils.UiEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val repository: DataStoreRepository,
    private val consentStore: ConsentStore,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _uiEvents = Channel<UiEvents>(Channel.BUFFERED)
    val uiEvents = _uiEvents.receiveAsFlow()

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
            repository.savePreference(key = DataStoreRepository.onBoardingKey, value = true)
            val step = LaunchRouter.afterOnboarding(consentDecided = consentStore.consent.first().decided)
            _uiEvents.send(UiEvents.Navigate(step.route))
        }
    }
}
