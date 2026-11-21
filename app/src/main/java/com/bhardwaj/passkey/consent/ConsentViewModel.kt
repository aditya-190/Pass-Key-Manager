package com.bhardwaj.passkey.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsentUiState(val analytics: Boolean = false, val crash: Boolean = false, val isLoading: Boolean = true)

sealed interface ConsentIntent {
    data class SetAnalytics(val enabled: Boolean) : ConsentIntent

    data class SetCrash(val enabled: Boolean) : ConsentIntent

    data object Save : ConsentIntent

    data object AllowAll : ConsentIntent

    data object DeclineAll : ConsentIntent
}

sealed interface ConsentEffect {
    data object Finished : ConsentEffect
}

/**
 * Shared by the first-run consent step and Settings > Privacy options. Nothing is pre-ticked: a user who
 * has never chosen starts with both switches off, and "Decline all" is as prominent as "Allow all".
 */
@HiltViewModel
class ConsentViewModel @Inject constructor(private val store: ConsentStore, private val analytics: AnalyticsTracker) :
    ViewModel() {
    private val _state = MutableStateFlow(ConsentUiState())
    val state: StateFlow<ConsentUiState> = _state.asStateFlow()

    private val _effects = Channel<ConsentEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var saved = false

    init {
        viewModelScope.launch {
            val stored = store.consent.first()
            _state.value = ConsentUiState(analytics = stored.analytics, crash = stored.crash, isLoading = false)
        }
    }

    fun onIntent(intent: ConsentIntent) {
        when (intent) {
            is ConsentIntent.SetAnalytics -> _state.update { it.copy(analytics = intent.enabled) }
            is ConsentIntent.SetCrash -> _state.update { it.copy(crash = intent.enabled) }
            ConsentIntent.Save -> save(_state.value.analytics, _state.value.crash)
            ConsentIntent.AllowAll -> save(analytics = true, crash = true)
            ConsentIntent.DeclineAll -> save(analytics = false, crash = false)
        }
    }

    private fun save(analytics: Boolean, crash: Boolean) {
        // Rapid double taps must not write twice or navigate twice.
        if (saved) return
        saved = true
        _state.update { it.copy(analytics = analytics, crash = crash) }
        viewModelScope.launch {
            store.save(analytics = analytics, crash = crash)
            // Logged after saving: if analytics is now off, Firebase drops it, which is correct.
            this@ConsentViewModel.analytics.log(AppEvents.consentUpdated(analytics = analytics, crash = crash))
            _effects.send(ConsentEffect.Finished)
        }
    }
}
