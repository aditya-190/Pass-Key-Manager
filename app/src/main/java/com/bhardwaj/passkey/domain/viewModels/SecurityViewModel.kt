package com.bhardwaj.passkey.domain.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.data.security.VaultSession
import com.bhardwaj.passkey.domain.events.SecurityEvents
import com.bhardwaj.passkey.presentation.navigation.Routes
import com.bhardwaj.passkey.utils.UiEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val vaultSession: VaultSession,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var isAuthenticated by mutableStateOf(false)

    var needToOpenAlertDialog by mutableStateOf(false)
        private set

    fun onEvent(event: SecurityEvents) {
        when (event) {
            SecurityEvents.OnPromptStarted -> {
                promptInFlight = true
                attempts++
                leftAppSinceAttempt = false
            }

            // A stop while the prompt is up is the system PIN screen covering us, not the user leaving.
            SecurityEvents.OnStopped -> if (!promptInFlight) leftAppSinceAttempt = true

            SecurityEvents.AddPasswordToTheDeviceFirst -> {
                promptInFlight = false
                isAuthenticated = false
                needToOpenAlertDialog = true
            }

            SecurityEvents.OnAuthenticationError -> {
                promptInFlight = false
                analytics.log(AppEvents.unlockFailed())
                isAuthenticated = false
                needToOpenAlertDialog = true
            }

            SecurityEvents.OnAuthenticationSucceeded -> {
                promptInFlight = false
                analytics.log(AppEvents.vaultUnlocked())
                // Previously isAuthenticated was written and never read, so navigation was the
                // only gate. The session is now the thing the rest of the app observes.
                vaultSession.onUnlocked()
                isAuthenticated = true
                needToOpenAlertDialog = false
                sendUiEvents(UiEvents.Navigate(Routes.PREVIEW_PAGE))
            }
        }
    }

    private var promptInFlight = false
    private var attempts = 0
    private var leftAppSinceAttempt = false

    /**
     * Prompt automatically only when the lock screen is actually on screen (called from ON_RESUME), and
     * only on its first showing or after the user really left the app since the last attempt. Otherwise
     * cancelling the system PIN screen, which resumes the app, would re-prompt forever.
     */
    fun shouldPromptOnResume(): Boolean =
        !vaultSession.isUnlocked.value && !promptInFlight && (attempts == 0 || leftAppSinceAttempt)

    /** The on-screen Unlock button: always available unless a prompt is already showing. */
    fun canPromptManually(): Boolean = !vaultSession.isUnlocked.value && !promptInFlight

    private fun sendUiEvents(events: UiEvents) {
        viewModelScope.launch {
            _uiEvents.send(events)
        }
    }
}
