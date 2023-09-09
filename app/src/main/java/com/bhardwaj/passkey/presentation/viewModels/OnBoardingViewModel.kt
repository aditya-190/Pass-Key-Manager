package com.bhardwaj.passkey.presentation.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.presentation.views.events.OnBoardingEvents
import com.bhardwaj.passkey.utils.Routes
import com.bhardwaj.passkey.utils.UiEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val repository: DataStoreRepository
) : ViewModel() {

    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    fun onEvent(event: OnBoardingEvents) {
        when (event) {
            is OnBoardingEvents.OnBoardingComplete -> {
                viewModelScope.launch {
                    repository.savePreference(key = DataStoreRepository.onBoardingKey, value = true)
                    _uiEvents.send(UiEvents.Navigate(Routes.SECURITY_PAGE))
                }
            }
        }
    }
}