package com.bhardwaj.passkey.banner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AnnouncementIntent {
    data class CtaClicked(val announcement: Announcement) : AnnouncementIntent

    data class Dismissed(val announcement: Announcement) : AnnouncementIntent
}

sealed interface AnnouncementEffect {
    data class OpenUrl(val url: String) : AnnouncementEffect

    data class Navigate(val route: String) : AnnouncementEffect
}

/**
 * Drives [AnnouncementBanner] on the screen that hosts it (usually Home).
 * Hilt: annotate with `@HiltViewModel` and `@Inject constructor`. Manual DI: create it with a factory.
 */
@HiltViewModel
class AnnouncementViewModel @Inject constructor(
    private val repository: AnnouncementRepository,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val loggedIds = mutableSetOf<String>()

    val announcement: StateFlow<Announcement?> =
        repository.current
            .onEach { shown ->
                if (shown != null && loggedIds.add(shown.id)) {
                    analytics.log(BaselineEvents.announcementShown(shown.id, shown.style.analyticsName))
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _effects = Channel<AnnouncementEffect>(Channel.BUFFERED)
    val effects: Flow<AnnouncementEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: AnnouncementIntent) {
        when (intent) {
            is AnnouncementIntent.CtaClicked -> {
                val item = intent.announcement
                val effect =
                    item.route?.let { AnnouncementEffect.Navigate(it) }
                        ?: item.url?.let { AnnouncementEffect.OpenUrl(it) }
                        ?: return
                analytics.log(
                    BaselineEvents.announcementClicked(
                        item.id,
                        if (effect is AnnouncementEffect.Navigate) "route" else "url"
                    )
                )
                _effects.trySend(effect)
            }
            is AnnouncementIntent.Dismissed -> {
                if (!intent.announcement.dismissible) return
                analytics.log(BaselineEvents.announcementDismissed(intent.announcement.id))
                viewModelScope.launch { repository.dismiss(intent.announcement.id) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
