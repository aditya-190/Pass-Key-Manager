package com.bhardwaj.passkey.presentation.screens.settings_screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.data.backup.BackupCheck
import com.bhardwaj.passkey.data.backup.BackupError
import com.bhardwaj.passkey.data.backup.BackupException
import com.bhardwaj.passkey.data.backup.BackupRepository
import com.bhardwaj.passkey.data.security.DatabaseKeyManager
import com.bhardwaj.passkey.di.WorkDispatcher
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.repository.BackupReminderStore
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.utils.PasswordAnalyzer
import com.bhardwaj.passkey.utils.SecretKeywords
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Arrays
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    private val preferences: PreferencesRepository,
    private val backupRepository: BackupRepository,
    private val secretKeywords: SecretKeywords,
    private val keyManager: DatabaseKeyManager,
    private val reminderStore: BackupReminderStore,
    private val clock: Clock,
    @param:WorkDispatcher private val workDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val local = MutableStateFlow(SettingsState())

    /** Held only between the two recovery-change steps, then zeroed. */
    private var pendingCurrentRecoveryPassword: CharArray? = null

    private val backupMutex = Mutex()

    val state: StateFlow<SettingsState> = combine(local, preferences.autoLockTimeout) { ui, timeout ->
        ui.copy(autoLockTimeout = timeout)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsState())

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            SettingsIntent.LanguageClicked -> showDialog(SettingsDialog.Language)
            is SettingsIntent.LanguageChosen -> chooseLanguage(intent)
            SettingsIntent.AutoLockClicked -> showDialog(SettingsDialog.AutoLock)
            is SettingsIntent.AutoLockChosen -> chooseAutoLock(intent.timeout)
            SettingsIntent.AnalyzeClicked -> analyze()
            is SettingsIntent.AnalysisItemClicked -> {
                showDialog(null)
                send(SettingsEffect.Navigate(NavRoute.Details(intent.detail.previewId)))
            }

            SettingsIntent.RateClicked -> send(SettingsEffect.OpenUrl(PLAY_STORE_URL + BuildConfig.APPLICATION_ID))
            SettingsIntent.AutofillClicked -> showDialog(SettingsDialog.AutofillHelp)
            SettingsIntent.AutofillHelpContinue -> {
                showDialog(null)
                send(SettingsEffect.OpenAutofillSettings)
            }
            SettingsIntent.ChangeRecoveryClicked -> showDialog(SettingsDialog.RecoveryCurrent)
            is SettingsIntent.CurrentRecoveryEntered -> {
                wipePendingRecovery()
                pendingCurrentRecoveryPassword = intent.password
                showDialog(SettingsDialog.RecoveryNew)
            }

            is SettingsIntent.NewRecoveryEntered -> changeRecovery(intent.password)
            SettingsIntent.DialogDismissed -> {
                wipePendingRecovery()
                showDialog(null)
            }

            is SettingsIntent.ExportChosen,
            is SettingsIntent.VerifyChosen,
            is SettingsIntent.ImportChosen -> onBackupIntent(intent)
        }
    }

    /** Export, verify and import: one at a time, passwords always wiped. */
    private fun onBackupIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ExportChosen -> runBackup {
                val result = backupRepository.export(intent.uri, intent.password)
                Arrays.fill(intent.password, Char(0))
                if (result.isSuccess) reminderStore.recordBackup(clock.nowMs())
                snackbar(if (result.isSuccess) R.string.export_success else R.string.export_failed)
            }

            is SettingsIntent.VerifyChosen -> runBackup {
                val result = backupRepository.verify(intent.uri, intent.password)
                intent.password?.let { Arrays.fill(it, Char(0)) }
                result.fold(
                    onSuccess = { check -> send(SettingsEffect.ShowSnackbar(verifyMessage(check))) },
                    onFailure = { error -> snackbar(importFailureMessage(error)) }
                )
            }

            is SettingsIntent.ImportChosen -> runBackup {
                val result = backupRepository.import(intent.uri, intent.password, intent.mode)
                intent.password?.let { Arrays.fill(it, Char(0)) }
                result.fold(
                    onSuccess = { summary ->
                        send(
                            SettingsEffect.ShowSnackbar(
                                UiText.StringResource(
                                    R.string.import_summary,
                                    listOf(summary.previewsAdded, summary.detailsAdded)
                                )
                            )
                        )
                    },
                    onFailure = { error -> snackbar(importFailureMessage(error)) }
                )
            }
            else -> Unit
        }
    }

    private fun showDialog(dialog: SettingsDialog?) = local.update { it.copy(dialog = dialog) }

    private fun chooseLanguage(intent: SettingsIntent.LanguageChosen) {
        showDialog(null)
        viewModelScope.launch {
            preferences.setSelectedLanguageTag(intent.language.tag)
            send(SettingsEffect.ApplyLanguage(intent.language.tag))
        }
    }

    private fun chooseAutoLock(timeout: AutoLockTimeout) {
        showDialog(null)
        viewModelScope.launch { preferences.setAutoLockTimeout(timeout) }
    }

    private fun analyze() = runWork {
        val details = repository.getDetails().first()
        val keywords = secretKeywords.all()
        // Off the main thread: a large vault takes noticeable time to digest and compare.
        val result = withContext(workDispatcher) { PasswordAnalyzer.analyze(details, keywords) }
        showDialog(SettingsDialog.Analysis(result))
    }

    private fun changeRecovery(newPassword: CharArray) {
        val current = pendingCurrentRecoveryPassword
        pendingCurrentRecoveryPassword = null
        showDialog(null)
        viewModelScope.launch {
            val changed = current != null && keyManager.changeRecoveryPassword(current, newPassword)
            current?.let { Arrays.fill(it, Char(0)) }
            Arrays.fill(newPassword, Char(0))
            snackbar(if (changed) R.string.recovery_change_done else R.string.recovery_change_failed)
        }
    }

    private fun wipePendingRecovery() {
        pendingCurrentRecoveryPassword?.let { Arrays.fill(it, Char(0)) }
        pendingCurrentRecoveryPassword = null
    }

    /**
     * Backups run one after another, never dropped: by the time the intent arrives the user has already picked a
     * file (the picker created it) and typed a password, so ignoring it left an empty file and an unwiped password.
     */
    private fun runBackup(block: suspend () -> Unit) {
        viewModelScope.launch { backupMutex.withLock { block() } }
    }

    /** One safety check at a time: rapid taps used to start several. */
    private fun runWork(block: suspend () -> Unit) {
        if (local.value.isWorking) return
        local.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                local.update { it.copy(isWorking = false) }
            }
        }
    }

    private fun importFailureMessage(error: Throwable): Int = when ((error as? BackupException)?.error) {
        BackupError.WrongPasswordOrCorrupt -> R.string.import_wrong_password
        BackupError.FileTooLarge -> R.string.import_file_too_large
        is BackupError.UnsupportedVersion -> R.string.import_unsupported_version
        else -> R.string.import_failed
    }

    private fun verifyMessage(check: BackupCheck): UiText = if (check.legacyFormat) {
        UiText.StringResource(R.string.verify_backup_ok_legacy, listOf(check.entries))
    } else {
        val date = check.exportedAt?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: "?"
        UiText.StringResource(R.string.verify_backup_ok, listOf(check.entries, check.authenticators, date))
    }

    private fun snackbar(message: Int) = send(SettingsEffect.ShowSnackbar(UiText.StringResource(message)))

    private fun send(effect: SettingsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id="
    }
}
