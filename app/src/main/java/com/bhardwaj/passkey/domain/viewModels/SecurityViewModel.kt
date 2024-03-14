package com.bhardwaj.passkey.domain.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.domain.events.SecurityEvents
import com.bhardwaj.passkey.presentation.navigation.Routes
import com.bhardwaj.passkey.utils.UiEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor() : ViewModel() {
    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    private var isAuthenticated by mutableStateOf(false)

    var needToOpenAlertDialog by mutableStateOf(false)
        private set

    fun onEvent(event: SecurityEvents) {
        when (event) {
            SecurityEvents.AddPasswordToTheDeviceFirst -> {
                isAuthenticated = false
                needToOpenAlertDialog = true
            }

            SecurityEvents.OnAuthenticationError -> {
                isAuthenticated = false
                needToOpenAlertDialog = true
            }

            SecurityEvents.OnAuthenticationSucceeded -> {
                isAuthenticated = true
                needToOpenAlertDialog = false
                sendUiEvents(UiEvents.Navigate(Routes.PREVIEW_PAGE))
            }
        }
    }

    private fun sendUiEvents(events: UiEvents) {
        viewModelScope.launch {
            _uiEvents.send(events)
        }
    }
}