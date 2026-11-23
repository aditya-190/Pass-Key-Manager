package com.bhardwaj.passkey.simulation

import androidx.lifecycle.SavedStateHandle
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.analytics.EventValidator
import com.bhardwaj.passkey.data.backup.BackupCodec
import com.bhardwaj.passkey.domain.backup.BackupReminderPolicy
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.repository.BackupReminderState
import com.bhardwaj.passkey.domain.totp.TotpConfig
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEffect
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourEvent
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.FeatureTourViewModel
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnboardingPages
import com.bhardwaj.passkey.testing.FakePasskeyRepository
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Feature workflow step 3 for 6.0.0 (feature tour): 1,000 seeded users mash Skip and Next in random orders,
 * including double taps. Every assertion names its seed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Release600SimulationTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `a replayed tour always ends exactly once, with one clean analytics trail`() = runTest {
        val pages = OnboardingPages.tour.size
        repeat(SEEDS) { seed ->
            val random = Random(seed)
            val analytics = RecordingAnalyticsTracker()
            val vm = FeatureTourViewModel(analytics, SavedStateHandle())
            val effects = mutableListOf<FeatureTourEffect>()
            val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.effects.toList(effects)
            }
            repeat(random.nextInt(1, 12)) {
                if (random.nextBoolean()) {
                    vm.onEvent(FeatureTourEvent.Skip(random.nextInt(pages)))
                } else {
                    vm.onEvent(FeatureTourEvent.Complete)
                }
            }
            advanceUntilIdle()
            collector.cancel()

            assertEquals("seed $seed", listOf(FeatureTourEffect.Close), effects)
            assertEquals("seed $seed", AppEventNames.FEATURE_TOUR_STARTED, analytics.names.first())
            assertEquals("seed $seed", 2, analytics.events.size)
            val end = analytics.events.last()
            assertTrue(
                "seed $seed",
                end.name in setOf(AppEventNames.FEATURE_TOUR_COMPLETED, AppEventNames.FEATURE_TOUR_SKIPPED)
            )
            (end.params[AppParams.PAGE] as Int?)?.let { assertTrue("seed $seed", it in 1..pages) }
            analytics.events.forEach { assertEquals("seed $seed", emptyList<String>(), EventValidator.validate(it)) }
        }
    }

    @Test
    fun `the backup reminder is never shown for an empty vault, never inside a snooze, and never silenced for good`() {
        val day = BackupReminderPolicy.DAY_MS
        repeat(REMINDER_SEEDS) { seed ->
            val rnd = Random(seed)
            val t0 = 1_700_000_000_000L + rnd.nextLong(0, 400 * day)
            fun around() = t0 + rnd.nextLong(-60 * day, 60 * day)
            val state = BackupReminderState(
                lastBackupAt = if (rnd.nextBoolean()) around() else null,
                firstSeenAt = if (rnd.nextInt(10) > 0) around() else null,
                snoozedUntil = if (rnd.nextBoolean()) around() else null
            )
            val now = around()
            val entries = rnd.nextInt(0, 4)

            assertFalse("seed $seed", BackupReminderPolicy.isDue(now, state, entryCount = 0))
            // A snooze just taken always holds for its whole length.
            val snoozed = state.copy(snoozedUntil = now + BackupReminderPolicy.SNOOZE_MS)
            assertFalse(
                "seed $seed",
                BackupReminderPolicy.isDue(now + rnd.nextLong(0, BackupReminderPolicy.SNOOZE_MS), snoozed, entries)
            )
            // A fresh backup always quiets it for 30 days.
            val backedUp = state.copy(lastBackupAt = now, snoozedUntil = null)
            assertFalse(
                "seed $seed",
                BackupReminderPolicy.isDue(
                    now + rnd.nextLong(0, BackupReminderPolicy.REMIND_AFTER_MS),
                    backedUp,
                    entries
                )
            )
            // Liveness: with entries and a known start, it always comes back after every stored time is long past.
            if (entries > 0 && state.firstSeenAt != null) {
                val later =
                    listOfNotNull(state.lastBackupAt, state.firstSeenAt, state.snoozedUntil, now).max() + 31 * day
                assertTrue("seed $seed", BackupReminderPolicy.isDue(later, state, entries))
            }
        }
    }

    @Test
    fun `verifying random backups reports exactly what they hold, and damaged files fail cleanly`() =
        runTest(timeout = 5.minutes) {
            val password = "simulation password".toCharArray()
            repeat(VERIFY_SEEDS) { seed ->
                val rnd = Random(seed)
                val source = FakePasskeyRepository()
                repeat(rnd.nextInt(8)) { i ->
                    val id = source.createPreview("entry $i ${rnd.nextInt()}", Category.entries.random(rnd), i.toLong())
                    repeat(rnd.nextInt(5)) { j ->
                        source.createDetail(id, "q$j", "a".repeat(rnd.nextInt(1, 300)), j.toLong())
                    }
                    repeat(rnd.nextInt(2)) { source.createTotp(id, "acct", TotpConfig(secret = rnd.nextBytes(10))) }
                }
                val bytes = BackupCodec(
                    source
                ).encode(password.copyOf(), exportedAt = seed.toLong(), appVersion = "sim")
                val target = FakePasskeyRepository()
                val codec = BackupCodec(target)

                val check = codec.check(codec.decode(bytes, password.copyOf()).getOrThrow())
                assertEquals("seed $seed", source.allPreviews.size, check.entries)
                assertEquals("seed $seed", source.allDetails.size, check.fields)
                assertEquals("seed $seed", source.allTotp.size, check.authenticators)
                assertEquals("seed $seed", seed.toLong(), check.exportedAt)

                val damaged = when (rnd.nextInt(3)) {
                    0 -> bytes.copyOf().also { it[rnd.nextInt(it.size)] = (it[rnd.nextInt(it.size)] + 1).toByte() }
                    1 -> bytes.copyOf(rnd.nextInt(1, bytes.size))
                    else -> bytes
                }
                val wrongPassword = damaged === bytes
                val result = codec.decode(damaged, if (wrongPassword) "wrong".toCharArray() else password.copyOf())
                if (wrongPassword) assertTrue("seed $seed", result.isFailure)
                result.onSuccess { codec.check(it) } // a lucky flip in padding may still decode; it must not throw
                assertTrue("seed $seed: verify never writes", target.allPreviews.isEmpty() && target.transactions == 0)
            }
        }

    @Test
    fun `random files picked by mistake never pass as an empty backup and never crash`() = runTest {
        val codec = BackupCodec(FakePasskeyRepository())
        repeat(JUNK_SEEDS) { seed ->
            val rnd = Random(seed)
            val junk = rnd.nextBytes(rnd.nextInt(0, 4_000))
            val result = codec.decode(junk, null)
            result.onSuccess { assertTrue("seed $seed: a readable CSV has entries", it.previews.isNotEmpty()) }
            result.onFailure {
                assertTrue("seed $seed", it is com.bhardwaj.passkey.data.backup.BackupException)
            }
        }
    }

    private companion object {
        const val SEEDS = 1_000
        const val REMINDER_SEEDS = 2_000
        const val VERIFY_SEEDS = 40
        const val JUNK_SEEDS = 1_000
    }
}
