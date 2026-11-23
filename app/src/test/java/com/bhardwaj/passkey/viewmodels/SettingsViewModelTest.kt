package com.bhardwaj.passkey.viewmodels

import android.net.Uri
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.data.backup.BackupCheck
import com.bhardwaj.passkey.data.backup.BackupError
import com.bhardwaj.passkey.data.backup.BackupException
import com.bhardwaj.passkey.data.backup.BackupRepository
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.data.backup.ImportSummary
import com.bhardwaj.passkey.data.security.DatabaseKeyManager
import com.bhardwaj.passkey.domain.model.AppLanguage
import com.bhardwaj.passkey.domain.model.AutoLockTimeout
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.ThemeMode
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsDialog
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsEffect
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsIntent
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsViewModel
import com.bhardwaj.passkey.review.ReviewPromptState
import com.bhardwaj.passkey.review.ReviewPrompter
import com.bhardwaj.passkey.testing.FakeBackupReminderStore
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.FakeReviewPromptStore
import com.bhardwaj.passkey.testing.FixedConfigProvider
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.utils.FormattedDate
import com.bhardwaj.passkey.utils.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Single-state MVI contract of Settings (5.8.0). Android-bound collaborators are mocked. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val vault = FakePasskeyRepository()
    private val prefs = FakePreferences()
    private val backups = mockk<BackupRepository>()
    private val keyManager = mockk<DatabaseKeyManager>()
    private val uri = mockk<Uri>()
    private val reminders = FakeBackupReminderStore()
    private val analytics = RecordingAnalyticsTracker()
    private val reviewStore = FakeReviewPromptStore(ReviewPromptState(firstSeenAt = NOW - 30 * DAY))

    private fun TestScope.vm() = SettingsViewModel(
        repository = vault,
        preferences = prefs,
        backupRepository = backups,
        secretKeywords = { setOf("pin") },
        keyManager = keyManager,
        reminderStore = reminders,
        clock = Clock { NOW },
        analytics = analytics,
        reviewPrompter = ReviewPrompter(reviewStore, Clock { NOW }, FixedConfigProvider(), analytics),
        workDispatcher = UnconfinedTestDispatcher(testScheduler)
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @Test
    fun `shows the saved auto-lock and the build's version`() = runTest {
        prefs.timeout.value = AutoLockTimeout.MINUTE_1
        val vm = vm()
        assertEquals(AutoLockTimeout.MINUTE_1, vm.state.value.autoLockTimeout)
        assertEquals(BuildConfig.VERSION_NAME, vm.state.value.appVersion)
    }

    @Test
    fun `choosing a language saves it and asks the screen to apply it`() = runTest {
        val vm = vm()
        vm.onIntent(SettingsIntent.LanguageClicked)
        assertEquals(SettingsDialog.Language, vm.state.value.dialog)

        vm.onIntent(SettingsIntent.LanguageChosen(AppLanguage.HINDI))
        assertNull(vm.state.value.dialog)
        assertEquals("hi", prefs.language.value)
        assertEquals(SettingsEffect.ApplyLanguage("hi"), vm.effects.first())
    }

    @Test
    fun `auto-lock choice is saved and closes the dialog`() = runTest {
        val vm = vm()
        vm.onIntent(SettingsIntent.AutoLockClicked)
        vm.onIntent(SettingsIntent.AutoLockChosen(AutoLockTimeout.MINUTES_5))
        assertNull(vm.state.value.dialog)
        assertEquals(AutoLockTimeout.MINUTES_5, vm.state.value.autoLockTimeout)
    }

    @Test
    fun `the safety check opens with results and a weak item leads to its entry`() = runTest {
        val bank = vault.createPreview("Bank", Category.BANKS)
        vault.createDetail(bank, "PIN", "1111", isSecret = true)
        val vm = vm()
        vm.onIntent(SettingsIntent.AnalyzeClicked)
        val dialog = vm.state.value.dialog as SettingsDialog.Analysis
        assertEquals(1, dialog.result.totalPasswords)

        vm.onIntent(SettingsIntent.AnalysisItemClicked(dialog.result.weakPasswords.single()))
        assertNull(vm.state.value.dialog)
        assertEquals(SettingsEffect.Navigate(NavRoute.Details(bank)), vm.effects.first())
    }

    @Test
    fun `changing the recovery password checks the current one and wipes both from memory`() = runTest {
        coEvery { keyManager.changeRecoveryPassword(any(), any()) } returns true
        val vm = vm()
        val current = "old-password".toCharArray()
        val new = "a new long phrase".toCharArray()

        vm.onIntent(SettingsIntent.ChangeRecoveryClicked)
        assertEquals(SettingsDialog.RecoveryCurrent, vm.state.value.dialog)
        vm.onIntent(SettingsIntent.CurrentRecoveryEntered(current))
        assertEquals(SettingsDialog.RecoveryNew, vm.state.value.dialog)
        vm.onIntent(SettingsIntent.NewRecoveryEntered(new))

        assertNull(vm.state.value.dialog)
        assertEquals(
            SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.recovery_change_done)),
            vm.effects.first()
        )
        coVerify { keyManager.changeRecoveryPassword(current, new) }
        assertTrue(current.all { it == Char(0) } && new.all { it == Char(0) })
    }

    @Test
    fun `a wrong current recovery password reports failure, and cancelling wipes it`() = runTest {
        coEvery { keyManager.changeRecoveryPassword(any(), any()) } returns false
        val vm = vm()
        vm.onIntent(SettingsIntent.ChangeRecoveryClicked)
        vm.onIntent(SettingsIntent.CurrentRecoveryEntered("wrong".toCharArray()))
        vm.onIntent(SettingsIntent.NewRecoveryEntered("a new long phrase".toCharArray()))
        assertEquals(
            SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.recovery_change_failed)),
            vm.effects.first()
        )

        val abandoned = "typed then cancelled".toCharArray()
        vm.onIntent(SettingsIntent.ChangeRecoveryClicked)
        vm.onIntent(SettingsIntent.CurrentRecoveryEntered(abandoned))
        vm.onIntent(SettingsIntent.DialogDismissed)
        assertNull(vm.state.value.dialog)
        assertTrue(abandoned.all { it == Char(0) })
    }

    @Test
    fun `export reports the result and never keeps the password`() = runTest {
        coEvery { backups.export(any(), any()) } returns Result.success(Unit)
        val vm = vm()
        val password = "backup pass".toCharArray()
        vm.onIntent(SettingsIntent.ExportChosen(uri, password))
        assertEquals(SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.export_success)), vm.effects.first())
        assertTrue(password.all { it == Char(0) })
    }

    @Test
    fun `the theme choice opens, saves, closes and shows the saved mode`() = runTest {
        val vm = vm()
        vm.onIntent(SettingsIntent.ThemeClicked)
        assertEquals(SettingsDialog.Theme, vm.state.value.dialog)
        vm.onIntent(SettingsIntent.ThemeChosen(ThemeMode.DARK))
        assertNull(vm.state.value.dialog)
        assertEquals(ThemeMode.DARK, prefs.theme.value)
        assertEquals(ThemeMode.DARK, vm.state.value.themeMode)
    }

    @Test
    fun `a successful export is remembered for the backup reminder, a failed one is not`() = runTest {
        coEvery { backups.export(any(), any()) } returns Result.failure(BackupException(BackupError.WriteFailed))
        val vm = vm()
        vm.onIntent(SettingsIntent.ExportChosen(uri, "p".toCharArray()))
        vm.effects.first()
        assertNull(reminders.flow.value.lastBackupAt)

        coEvery { backups.export(any(), any()) } returns Result.success(Unit)
        vm.onIntent(SettingsIntent.ExportChosen(uri, "p".toCharArray()))
        vm.effects.first()
        assertEquals(NOW, reminders.flow.value.lastBackupAt)
    }

    @Test
    fun `verifying a backup reports what it holds, imports nothing and wipes the password`() = runTest {
        coEvery { backups.verify(any(), any()) } returns
            Result.success(
                BackupCheck(entries = 12, fields = 30, authenticators = 2, exportedAt = NOW, legacyFormat = false)
            )
        val vm = vm()
        val password = "backup pass".toCharArray()
        vm.onIntent(SettingsIntent.VerifyChosen(uri, password))
        val message = (vm.effects.first() as SettingsEffect.ShowSnackbar).message as UiText.StringResource
        assertEquals(R.string.verify_backup_ok, message.resId)
        assertEquals(listOf(12, 2), message.args.take(2))
        assertTrue(password.all { it == Char(0) })
        coVerify(exactly = 0) { backups.import(any(), any(), any()) }

        coEvery { backups.verify(any(), any()) } returns
            Result.success(
                BackupCheck(entries = 4, fields = 4, authenticators = 0, exportedAt = null, legacyFormat = true)
            )
        vm.onIntent(SettingsIntent.VerifyChosen(uri, null))
        assertEquals(
            SettingsEffect.ShowSnackbar(
                UiText.StringResource(R.string.verify_backup_ok_legacy, listOf(4)),
                long = true
            ),
            vm.effects.first()
        )

        coEvery { backups.verify(any(), any()) } returns
            Result.failure(BackupException(BackupError.WrongPasswordOrCorrupt))
        vm.onIntent(SettingsIntent.VerifyChosen(uri, "x".toCharArray()))
        assertEquals(
            SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.import_wrong_password)),
            vm.effects.first()
        )
    }

    @Test
    fun `a file that is not a backup gets its own message, in import and in verify`() = runTest {
        coEvery { backups.import(any(), any(), any()) } returns Result.failure(BackupException(BackupError.NotABackup))
        coEvery { backups.verify(any(), any()) } returns Result.failure(BackupException(BackupError.NotABackup))
        val vm = vm()
        val expected = SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.import_not_a_backup))
        vm.onIntent(SettingsIntent.ImportChosen(uri, null, ImportMode.MERGE))
        assertEquals(expected, vm.effects.first())
        vm.onIntent(SettingsIntent.VerifyChosen(uri, null))
        assertEquals(expected, vm.effects.first())
    }

    @Test
    fun `a verified backup stays on screen longer and shows its date in the app's language`() = runTest {
        // 6.0.0b attack-pass #2 and #3.
        val made = NOW - 2 * DAY
        coEvery { backups.verify(any(), any()) } returns
            Result.success(
                BackupCheck(entries = 3, fields = 3, authenticators = 0, exportedAt = made, legacyFormat = false)
            )
        val vm = vm()
        vm.onIntent(SettingsIntent.VerifyChosen(uri, "p".toCharArray()))
        val effect = vm.effects.first() as SettingsEffect.ShowSnackbar
        assertTrue(effect.long)
        assertEquals(FormattedDate(made), (effect.message as UiText.StringResource).args[2])
    }

    @Test
    fun `verifying a recent backup counts as having one, a future or old CSV one does not`() = runTest {
        // 6.0.0b attack-pass #7: people who exported in 5.8.0 were reminded anyway.
        val vm = vm()
        fun check(exportedAt: Long?, legacy: Boolean = false) =
            Result.success(BackupCheck(1, 1, 0, exportedAt = exportedAt, legacyFormat = legacy))

        coEvery { backups.verify(any(), any()) } returns check(NOW + 3 * DAY)
        vm.onIntent(SettingsIntent.VerifyChosen(uri, "p".toCharArray()))
        vm.effects.first()
        assertNull("a date in the future is not trusted", reminders.flow.value.lastBackupAt)

        coEvery { backups.verify(any(), any()) } returns check(null, legacy = true)
        vm.onIntent(SettingsIntent.VerifyChosen(uri, null))
        vm.effects.first()
        assertNull("an unencrypted CSV is not a backup to rely on", reminders.flow.value.lastBackupAt)

        coEvery { backups.verify(any(), any()) } returns check(NOW - 5 * DAY)
        vm.onIntent(SettingsIntent.VerifyChosen(uri, "p".toCharArray()))
        vm.effects.first()
        assertEquals(NOW - 5 * DAY, reminders.flow.value.lastBackupAt)

        coEvery { backups.verify(any(), any()) } returns check(NOW - 9 * DAY)
        vm.onIntent(SettingsIntent.VerifyChosen(uri, "p".toCharArray()))
        vm.effects.first()
        assertEquals("an older file never replaces a newer backup", NOW - 5 * DAY, reminders.flow.value.lastBackupAt)
    }

    @Test
    fun `share sends the store link and is counted`() = runTest {
        val vm = vm()
        vm.onIntent(SettingsIntent.ShareClicked)
        val effect = vm.effects.first() as SettingsEffect.Share
        assertTrue(effect.storeUrl.endsWith("id=${BuildConfig.APPLICATION_ID}"))
        assertTrue(AppEventNames.APP_SHARED in analytics.names)
    }

    @Test
    fun `a saved backup is a good moment to ask for a review, once`() = runTest {
        coEvery { backups.export(any(), any()) } returns Result.success(Unit)
        val vm = vm()
        vm.onIntent(SettingsIntent.ExportChosen(uri, "p".toCharArray()))
        assertEquals(SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.export_success)), vm.effects.first())
        assertEquals(SettingsEffect.RequestReview, vm.effects.first())

        vm.onIntent(SettingsIntent.ExportChosen(uri, "p".toCharArray()))
        assertEquals(SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.export_success)), vm.effects.first())
        vm.onIntent(SettingsIntent.RateClicked)
        assertTrue("no second ask inside the cooldown", vm.effects.first() is SettingsEffect.OpenUrl)
    }

    @Test
    fun `import maps each failure to a message the user can act on`() = runTest {
        coEvery { backups.import(any(), any(), any()) } returns
            Result.failure(BackupException(BackupError.WrongPasswordOrCorrupt))
        val vm = vm()
        vm.onIntent(SettingsIntent.ImportChosen(uri, "x".toCharArray(), ImportMode.MERGE))
        assertEquals(
            SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.import_wrong_password)),
            vm.effects.first()
        )

        coEvery { backups.import(any(), any(), any()) } returns Result.success(ImportSummary(2, 5, 0, false))
        vm.onIntent(SettingsIntent.ImportChosen(uri, null, ImportMode.MERGE))
        assertEquals(
            SettingsEffect.ShowSnackbar(UiText.StringResource(R.string.import_summary, listOf(2, 5))),
            vm.effects.first()
        )
    }

    @Test
    fun `rate opens this app's store page`() = runTest {
        val vm = vm()
        vm.onIntent(SettingsIntent.RateClicked)
        val effect = vm.effects.first() as SettingsEffect.OpenUrl
        assertTrue(effect.url.endsWith("id=${BuildConfig.APPLICATION_ID}"))
    }

    @Test
    fun `a backup chosen while another job runs still happens, and every password is wiped`() = runTest {
        // Attack-pass #3: it was silently dropped, leaving an empty file and the password in memory.
        val gate = CompletableDeferred<Unit>()
        coEvery { backups.export(any(), any()) } coAnswers {
            gate.await()
            Result.success(Unit)
        }
        val vm = vm()
        val first = "first pass".toCharArray()
        val second = "second pass".toCharArray()
        vm.onIntent(SettingsIntent.ExportChosen(uri, first))
        vm.onIntent(SettingsIntent.AnalyzeClicked)
        vm.onIntent(SettingsIntent.ExportChosen(uri, second))
        gate.complete(Unit)

        coVerify(exactly = 2) { backups.export(any(), any()) }
        assertTrue(first.all { it == Char(0) } && second.all { it == Char(0) })
        assertTrue("the safety check still ran", vm.state.value.dialog is SettingsDialog.Analysis)
    }

    @Test
    fun `autofill setup explains Chrome's extra step before opening Android's picker`() = runTest {
        // Device finding C: Chrome ignores the Android setting until "Autofill using another service" is chosen.
        val vm = vm()
        vm.onIntent(SettingsIntent.AutofillClicked)
        assertEquals(SettingsDialog.AutofillHelp, vm.state.value.dialog)
        vm.onIntent(SettingsIntent.DialogDismissed)
        assertNull(vm.state.value.dialog)

        vm.onIntent(SettingsIntent.AutofillClicked)
        vm.onIntent(SettingsIntent.AutofillHelpContinue)
        assertNull(vm.state.value.dialog)
        assertEquals(SettingsEffect.OpenAutofillSettings, vm.effects.first())
    }

    private companion object {
        const val NOW = 1_800_000_000_000L
        const val DAY = 86_400_000L
    }

    private class FakePreferences : PreferencesRepository {
        val timeout = MutableStateFlow(AutoLockTimeout.DEFAULT)
        val language = MutableStateFlow<String?>(null)
        override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
        override val selectedLanguageTag: Flow<String?> = language
        override suspend fun setSelectedLanguageTag(tag: String) {
            language.value = tag
        }
        override val autoLockTimeout: Flow<AutoLockTimeout> = timeout
        override suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
            this.timeout.value = timeout
        }
        val theme = MutableStateFlow(ThemeMode.SYSTEM)
        override val themeMode: Flow<ThemeMode> = theme
        override suspend fun setThemeMode(mode: ThemeMode) {
            theme.value = mode
        }
    }
}
