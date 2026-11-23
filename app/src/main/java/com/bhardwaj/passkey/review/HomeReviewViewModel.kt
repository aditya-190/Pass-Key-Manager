package com.bhardwaj.passkey.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * On Home (6.0.0): records the install's first look, and asks for a review once enough autofills have worked.
 * Autofill itself happens inside other apps, where a review dialog would interrupt; Home is where it fits. No UI.
 */
@HiltViewModel
class HomeReviewViewModel @Inject constructor(private val prompter: ReviewPrompter) : ViewModel() {
    private val _requests = Channel<Unit>(Channel.BUFFERED)
    val requests = _requests.receiveAsFlow()

    init {
        viewModelScope.launch {
            prompter.markFirstSeen()
            if (prompter.onMoment(ReviewMoment.AUTOFILL_WORKED)) _requests.send(Unit)
        }
    }
}
