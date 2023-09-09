package com.bhardwaj.passkey.presentation.viewModels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.presentation.views.events.SplashEvents
import com.bhardwaj.passkey.presentation.views.navigation.NavScreens
import com.bhardwaj.passkey.utils.UiEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    val repository: DataStoreRepository
) : ViewModel() {

    var startDestination by mutableStateOf(NavScreens.SplashPage.route)
        private set

    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onEvent(event: SplashEvents) {
        when (event) {
            SplashEvents.OnLoadingComplete -> {
                viewModelScope.launch {
                    repository.readPreference(
                        DataStoreRepository.onBoardingKey,
                        defaultValue = false
                    ).collect { completed ->
                        startDestination = if (completed) {
                            NavScreens.SecurityPage.route
                        } else {
                            NavScreens.OnboardingPage.route
                        }
                        _uiEvents.send(UiEvents.Navigate(startDestination))
                    }
                }
            }
        }
    }
}