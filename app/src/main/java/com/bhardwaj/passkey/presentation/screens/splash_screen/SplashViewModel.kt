package com.bhardwaj.passkey.presentation.screens.splash_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.consent.ConsentStore
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.launch.LaunchRouter
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.navigation.route
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
    private val consentStore: ConsentStore
) : ViewModel() {

    // BUFFERED, not the RENDEZVOUS default: with lifecycle-aware collection a backgrounded
    // screen has no active collector, and a rendezvous channel would suspend the coroutine
    // that emitted the effect until the user came back.
    private val _effects = Channel<SplashEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var routed = false

    fun onEvent(event: SplashEvents) {
        when (event) {
            SplashEvents.OnLoadingComplete -> {
                if (routed) return
                routed = true
                viewModelScope.launch {
                    // Read once. Collecting kept the splash subscribed and re-navigated on every later
                    // preference write.
                    val onboardingDone = preferences.onboardingCompleted.first()
                    val consentDecided = consentStore.consent.first().decided
                    val step = LaunchRouter.atStart(onboardingDone = onboardingDone, consentDecided = consentDecided)
                    _effects.send(SplashEffect.Navigate(step.route))
                }
            }
        }
    }
}

sealed interface SplashEffect {
    data class Navigate(val route: NavRoute) : SplashEffect
}
