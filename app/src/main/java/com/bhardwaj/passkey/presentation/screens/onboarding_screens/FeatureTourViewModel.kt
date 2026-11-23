package com.bhardwaj.passkey.presentation.screens.onboarding_screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The feature tour replayed on its own. It only reports how far people get and closes; it stores nothing. */
@HiltViewModel
class FeatureTourViewModel @Inject constructor(private val analytics: AnalyticsTracker) : ViewModel() {
    // BUFFERED for the same reason as onboarding: a backgrounded screen must not suspend the sender.
    private val _effects = Channel<FeatureTourEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var closed = false

    init {
        // Once per opening: the ViewModel outlives rotation.
        analytics.log(AppEvents.featureTourStarted())
    }

    fun onEvent(event: FeatureTourEvent) {
        if (closed) return
        closed = true
        when (event) {
            // Pages arrive 0-based from the pager and are logged 1-based, like onboarding.
            is FeatureTourEvent.Skip -> analytics.log(AppEvents.featureTourSkipped(event.page + 1))
            FeatureTourEvent.Complete -> analytics.log(AppEvents.featureTourCompleted())
        }
        viewModelScope.launch { _effects.send(FeatureTourEffect.Close) }
    }
}

sealed interface FeatureTourEvent {
    /** [page] is the 0-based pager index the user skipped from. */
    data class Skip(val page: Int) : FeatureTourEvent

    data object Complete : FeatureTourEvent
}

sealed interface FeatureTourEffect {
    data object Close : FeatureTourEffect
}
