package com.bhardwaj.passkey.backupreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.analytics.AnalyticsEvent
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.domain.backup.BackupReminderPolicy
import com.bhardwaj.passkey.domain.repository.BackupReminderStore
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BackupReminderIntent {
    /** "Back up now": the screen opens Settings; the card rests for a day in case they don't finish. */
    data object BackUpNow : BackupReminderIntent

    /** Dismissed: back in a week. */
    data object Later : BackupReminderIntent
}

/** Home's backup reminder (6.0.0), shown in the announcement card's place when no announcement is live. */
@HiltViewModel
class BackupReminderViewModel @Inject constructor(
    private val store: BackupReminderStore,
    repository: PasskeyRepository,
    private val clock: Clock,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    val isDue: StateFlow<Boolean> = combine(
        store.state,
        repository.getPreviews().map { it.size }.distinctUntilChanged()
    ) { state, entries -> BackupReminderPolicy.isDue(clock.nowMs(), state, entries) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    init {
        viewModelScope.launch { store.markFirstSeen(clock.nowMs()) }
    }

    fun onIntent(intent: BackupReminderIntent) {
        val (snooze, event) = when (intent) {
            BackupReminderIntent.BackUpNow -> BackupReminderPolicy.CTA_SNOOZE_MS to AppEventNames.BACKUP_REMINDER_TAPPED
            BackupReminderIntent.Later -> BackupReminderPolicy.SNOOZE_MS to AppEventNames.BACKUP_REMINDER_SNOOZED
        }
        analytics.log(AnalyticsEvent(event))
        viewModelScope.launch { store.snooze(clock.nowMs() + snooze) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
