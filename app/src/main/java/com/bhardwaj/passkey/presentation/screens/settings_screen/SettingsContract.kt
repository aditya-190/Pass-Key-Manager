package com.bhardwaj.passkey.presentation.screens.settings_screen

import android.net.Uri
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.domain.model.AppLanguage
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.ThemeMode
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.utils.PasswordAnalysisResult
import com.bhardwaj.passkey.utils.UiText

data class SettingsState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** At most one dialog or sheet at a time; five separate flags allowed impossible combinations. */
    val dialog: SettingsDialog? = null,
    /** A safety check is running; repeat taps are ignored until it ends. */
    val isWorking: Boolean = false
)

sealed interface SettingsDialog {
    data object Language : SettingsDialog
    data object AutoLock : SettingsDialog
    data object Theme : SettingsDialog

    /** Explains Chrome's own "Autofill using another service" step before Android's picker opens. */
    data object AutofillHelp : SettingsDialog
    data class Analysis(val result: PasswordAnalysisResult) : SettingsDialog

    /** Recovery change, step 1: prove the current password. */
    data object RecoveryCurrent : SettingsDialog

    /** Step 2: choose the new one. */
    data object RecoveryNew : SettingsDialog
}

/** Password-carrying intents are plain classes: a CharArray has no value equality, and they are never compared. */
sealed interface SettingsIntent {
    data object LanguageClicked : SettingsIntent
    data class LanguageChosen(val language: AppLanguage) : SettingsIntent
    data object AutoLockClicked : SettingsIntent
    data class AutoLockChosen(val timeout: AutoLockTimeout) : SettingsIntent
    data object ThemeClicked : SettingsIntent
    data class ThemeChosen(val mode: ThemeMode) : SettingsIntent
    data object AnalyzeClicked : SettingsIntent
    data class AnalysisItemClicked(val detail: Detail) : SettingsIntent
    data object RateClicked : SettingsIntent
    data object ShareClicked : SettingsIntent
    data object AutofillClicked : SettingsIntent
    data object AutofillHelpContinue : SettingsIntent
    data object ChangeRecoveryClicked : SettingsIntent
    class CurrentRecoveryEntered(val password: CharArray) : SettingsIntent
    class NewRecoveryEntered(val password: CharArray) : SettingsIntent
    data object DialogDismissed : SettingsIntent

    /** The user picked a destination and a password for the encrypted backup. */
    class ExportChosen(val uri: Uri, val password: CharArray) : SettingsIntent

    /** [password] is null for a legacy plaintext .passkey file, which needs none. */
    class ImportChosen(val uri: Uri, val password: CharArray?, val mode: ImportMode) : SettingsIntent

    /** Read a backup and report what it holds, without importing (6.0.0). [password] is null for old CSV files. */
    class VerifyChosen(val uri: Uri, val password: CharArray?) : SettingsIntent
}

sealed interface SettingsEffect {
    /** [long] keeps a message that has to be read (a backup check) on screen for longer. */
    data class ShowSnackbar(val message: UiText, val long: Boolean = false) : SettingsEffect
    data class Navigate(val route: NavRoute) : SettingsEffect
    data class OpenUrl(val url: String) : SettingsEffect

    /** Share PassKey's store link through the system share sheet (6.0.0). */
    data class Share(val storeUrl: String) : SettingsEffect

    /** A good moment: start Play's in-app review (it may show nothing; Play decides). */
    data object RequestReview : SettingsEffect

    /** Android owns the autofill-service picker; the app can only ask for it to be shown. */
    data object OpenAutofillSettings : SettingsEffect

    /** Applying a locale needs a Context, so the screen does it. */
    data class ApplyLanguage(val languageTag: String) : SettingsEffect
}
