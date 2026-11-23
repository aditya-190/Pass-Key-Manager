package com.bhardwaj.passkey.presentation.screens.settings_screen

import android.net.Uri
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.Language
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.utils.PasswordAnalysisResult
import com.bhardwaj.passkey.utils.UiText

data class SettingsState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.DEFAULT,
    /** At most one dialog or sheet at a time; five separate flags allowed impossible combinations. */
    val dialog: SettingsDialog? = null,
    /** A safety check is running; repeat taps are ignored until it ends. */
    val isWorking: Boolean = false
)

sealed interface SettingsDialog {
    data object Language : SettingsDialog
    data object AutoLock : SettingsDialog
    data class Analysis(val result: PasswordAnalysisResult) : SettingsDialog

    /** Recovery change, step 1: prove the current password. */
    data object RecoveryCurrent : SettingsDialog

    /** Step 2: choose the new one. */
    data object RecoveryNew : SettingsDialog
}

/** Password-carrying intents are plain classes: a CharArray has no value equality, and they are never compared. */
sealed interface SettingsIntent {
    data object LanguageClicked : SettingsIntent
    data class LanguageChosen(val language: Language) : SettingsIntent
    data object AutoLockClicked : SettingsIntent
    data class AutoLockChosen(val timeout: AutoLockTimeout) : SettingsIntent
    data object AnalyzeClicked : SettingsIntent
    data class AnalysisItemClicked(val detail: Detail) : SettingsIntent
    data object RateClicked : SettingsIntent
    data object ChangeRecoveryClicked : SettingsIntent
    class CurrentRecoveryEntered(val password: CharArray) : SettingsIntent
    class NewRecoveryEntered(val password: CharArray) : SettingsIntent
    data object DialogDismissed : SettingsIntent

    /** The user picked a destination and a password for the encrypted backup. */
    class ExportChosen(val uri: Uri, val password: CharArray) : SettingsIntent

    /** [password] is null for a legacy plaintext .passkey file, which needs none. */
    class ImportChosen(val uri: Uri, val password: CharArray?, val mode: ImportMode) : SettingsIntent
}

sealed interface SettingsEffect {
    data class ShowSnackbar(val message: UiText) : SettingsEffect
    data class Navigate(val route: NavRoute) : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect

    /** Applying a locale needs a Context, so the screen does it. */
    data class ApplyLanguage(val languageTag: String) : SettingsEffect
}
