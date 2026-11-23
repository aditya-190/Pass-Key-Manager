package com.bhardwaj.passkey.backup

import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.backupreminder.BackupReminderIntent
import com.bhardwaj.passkey.backupreminder.BackupReminderViewModel
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.domain.backup.BackupReminderPolicy
import com.bhardwaj.passkey.domain.backup.BackupReminderPolicy.DAY_MS
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.repository.BackupReminderState
import com.bhardwaj.passkey.testing.FakeBackupReminderStore
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 6.0.0 backup reminder: shown on Home when a vault has no recent backup; never for an empty vault. */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupReminderTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val t0 = 1_800_000_000_000L
    private fun due(now: Long, state: BackupReminderState, entries: Int = 5) =
        BackupReminderPolicy.isDue(now, state, entries)

    @Test
    fun `an empty vault is never nagged`() {
        assertFalse(due(t0 + 400 * DAY_MS, BackupReminderState(firstSeenAt = t0), entries = 0))
    }

    @Test
    fun `without any backup, the reminder comes 3 days after the first launch`() {
        val state = BackupReminderState(firstSeenAt = t0)
        assertFalse(due(t0 + 3 * DAY_MS - 1, state))
        assertTrue(due(t0 + 3 * DAY_MS, state))
        assertFalse("unknown start: wait until it is recorded", due(t0, BackupReminderState()))
    }

    @Test
    fun `after a backup, the reminder waits 30 days`() {
        val state = BackupReminderState(firstSeenAt = t0, lastBackupAt = t0 + DAY_MS)
        assertFalse(due(t0 + 31 * DAY_MS - 1, state))
        assertTrue(due(t0 + 31 * DAY_MS, state))
    }

    @Test
    fun `a snooze hides it until it ends`() {
        val state = BackupReminderState(firstSeenAt = t0, snoozedUntil = t0 + 10 * DAY_MS)
        assertFalse(due(t0 + 10 * DAY_MS - 1, state))
        assertTrue(due(t0 + 10 * DAY_MS, state))
    }

    @Test
    fun `moving the clock back can't silence it for longer than a snooze or hide a missing backup`() {
        // A snooze ending further away than any snooze can be: the clock went back, so it no longer counts.
        val farSnooze = BackupReminderState(firstSeenAt = t0, snoozedUntil = t0 + 400 * DAY_MS)
        assertTrue(due(t0 + 5 * DAY_MS, farSnooze))
        // A backup "in the future" by more than a day is not trusted as recent.
        val futureBackup = BackupReminderState(firstSeenAt = t0, lastBackupAt = t0 + 100 * DAY_MS)
        assertTrue(due(t0 + 5 * DAY_MS, futureBackup))
        // Small clock skew is tolerated.
        assertFalse(due(t0, BackupReminderState(firstSeenAt = t0, lastBackupAt = t0 + DAY_MS / 2)))
    }

    private fun TestScope.vm(
        store: FakeBackupReminderStore,
        vault: FakePasskeyRepository,
        now: Long,
        analytics: RecordingAnalyticsTracker
    ) = BackupReminderViewModel(store, vault, Clock { now }, analytics).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isDue.collect {} }
    }

    @Test
    fun `the first look records when the install started, once`() = runTest {
        val store = FakeBackupReminderStore()
        vm(store, FakePasskeyRepository(), t0, RecordingAnalyticsTracker())
        vm(store, FakePasskeyRepository(), t0 + DAY_MS, RecordingAnalyticsTracker())
        assertEquals(t0, store.flow.value.firstSeenAt)
    }

    @Test
    fun `due with entries, later snoozes a week, back up now snoozes a day`() = runTest {
        val vault = FakePasskeyRepository().apply { createPreview("Bank", Category.BANKS) }
        val store = FakeBackupReminderStore(BackupReminderState(firstSeenAt = t0))
        val analytics = RecordingAnalyticsTracker()
        val now = t0 + 4 * DAY_MS
        val vm = vm(store, vault, now, analytics)
        assertTrue(vm.isDue.value)

        vm.onIntent(BackupReminderIntent.Later)
        assertEquals(now + BackupReminderPolicy.SNOOZE_MS, store.flow.value.snoozedUntil)
        assertFalse(vm.isDue.value)

        vm.onIntent(BackupReminderIntent.BackUpNow)
        assertEquals(now + BackupReminderPolicy.CTA_SNOOZE_MS, store.flow.value.snoozedUntil)
        assertEquals(
            listOf(AppEventNames.BACKUP_REMINDER_SNOOZED, AppEventNames.BACKUP_REMINDER_TAPPED),
            analytics.names
        )
    }
}
