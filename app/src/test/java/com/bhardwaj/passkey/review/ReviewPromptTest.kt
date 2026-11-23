package com.bhardwaj.passkey.review

import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.testing.FakeReviewPromptStore
import com.bhardwaj.passkey.testing.FixedConfigProvider
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 6.0.0: ask for a Play review only after a good moment, never early, rarely. */
class ReviewPromptTest {
    private val day = 86_400_000L
    private val t0 = 1_800_000_000_000L
    private val pacing = ReviewPacing()

    private fun ask(now: Long, state: ReviewPromptState, moment: ReviewMoment = ReviewMoment.BACKUP_SAVED) =
        ReviewPromptPolicy.shouldAsk(now, state, moment, pacing)

    @Test
    fun `never in the first three days, never without a known start`() {
        val state = ReviewPromptState(firstSeenAt = t0)
        assertFalse(ask(t0 + 3 * day - 1, state))
        assertTrue(ask(t0 + 3 * day, state))
        assertFalse(ask(t0 + 30 * day, ReviewPromptState()))
    }

    @Test
    fun `at most once per cooldown`() {
        val state = ReviewPromptState(firstSeenAt = t0, lastAskedAt = t0 + 10 * day)
        assertFalse(ask(t0 + 130 * day - 1, state))
        assertTrue(ask(t0 + 130 * day, state))
    }

    @Test
    fun `the autofill moment needs enough successful fills`() {
        val state = ReviewPromptState(firstSeenAt = t0, autofillFills = 2)
        assertFalse(ask(t0 + 10 * day, state, ReviewMoment.AUTOFILL_WORKED))
        assertTrue(ask(t0 + 10 * day, state.copy(autofillFills = 3), ReviewMoment.AUTOFILL_WORKED))
        assertTrue("other moments don't need fills", ask(t0 + 10 * day, state, ReviewMoment.TOUR_FINISHED))
    }

    @Test
    fun `switched off remotely, it never asks`() {
        val state = ReviewPromptState(firstSeenAt = t0)
        assertFalse(
            ReviewPromptPolicy.shouldAsk(t0 + 50 * day, state, ReviewMoment.BACKUP_SAVED, pacing.copy(enabled = false))
        )
    }

    @Test
    fun `a moved clock can't make it ask again early`() {
        // lastAskedAt in the future (clock went back): treat as just asked.
        val state = ReviewPromptState(firstSeenAt = t0, lastAskedAt = t0 + 400 * day)
        assertFalse(ask(t0 + 200 * day, state))
    }

    @Test
    fun `the prompter asks once, records it, reads pacing from Remote Config and counts only the moment`() = runTest {
        val store = FakeReviewPromptStore(ReviewPromptState(firstSeenAt = t0))
        val analytics = RecordingAnalyticsTracker()
        var now = t0 + 5 * day
        val prompter = ReviewPrompter(store, Clock { now }, FixedConfigProvider(), analytics)

        assertTrue(prompter.onMoment(ReviewMoment.BACKUP_SAVED))
        assertEquals(now, store.flow.value.lastAskedAt)
        assertFalse("cooldown", prompter.onMoment(ReviewMoment.TOUR_FINISHED))
        assertEquals(
            mapOf(AppParams.SOURCE to "backup"),
            analytics.events.single {
                it.name ==
                    AppEventNames.REVIEW_REQUESTED
            }.params
        )

        now += 200 * day
        val off =
            ReviewPrompter(
                store,
                Clock {
                    now
                },
                FixedConfigProvider(mapOf("review_prompt_enabled" to false)),
                analytics
            )
        assertFalse(off.onMoment(ReviewMoment.BACKUP_SAVED))
    }

    @Test
    fun `fills are counted and the first look is recorded once`() = runTest {
        val store = FakeReviewPromptStore()
        val prompter = ReviewPrompter(store, Clock { t0 }, FixedConfigProvider(), RecordingAnalyticsTracker())
        prompter.markFirstSeen()
        prompter.recordFill()
        prompter.recordFill()
        val later = ReviewPrompter(store, Clock { t0 + day }, FixedConfigProvider(), RecordingAnalyticsTracker())
        later.markFirstSeen()
        assertEquals(ReviewPromptState(firstSeenAt = t0, autofillFills = 2), store.flow.value)
    }

    @Test
    fun `rating by hand counts as asked, so the prompt stays away`() = runTest {
        val store = FakeReviewPromptStore(ReviewPromptState(firstSeenAt = t0))
        val prompter =
            ReviewPrompter(store, Clock { t0 + 10 * day }, FixedConfigProvider(), RecordingAnalyticsTracker())
        prompter.noteManualRate()
        assertFalse(prompter.onMoment(ReviewMoment.BACKUP_SAVED))
    }
}
