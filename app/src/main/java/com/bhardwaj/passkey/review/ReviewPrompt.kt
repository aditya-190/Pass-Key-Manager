package com.bhardwaj.passkey.review

import kotlinx.coroutines.flow.Flow

/** The good moments after which PassKey may ask for a Play review (6.0.0). */
enum class ReviewMoment(val analyticsName: String) {
    BACKUP_SAVED("backup"),
    AUTOFILL_WORKED("autofill"),
    TOUR_FINISHED("tour")
}

/** Pacing from Remote Config. */
data class ReviewPacing(
    val enabled: Boolean = true,
    val minDays: Long = 3,
    val cooldownDays: Long = 120,
    val fillsBeforeAsk: Long = 3
)

/** Wall-clock times; null = never. */
data class ReviewPromptState(val firstSeenAt: Long? = null, val lastAskedAt: Long? = null, val autofillFills: Long = 0)

interface ReviewPromptStore {
    val state: Flow<ReviewPromptState>
    suspend fun markFirstSeen(at: Long)
    /** Keeps the later of the stored and the given time. */
    suspend fun recordAsked(at: Long)
    suspend fun recordFill()
}

/**
 * Whether to ask now. Never in the first [ReviewPacing.minDays], at most once per [ReviewPacing.cooldownDays], and
 * for the autofill moment only after [ReviewPacing.fillsBeforeAsk] successful fills. A last ask dated in the future
 * (the clock went back) counts as just asked, so moving the clock can't bring the prompt back early.
 */
object ReviewPromptPolicy {
    private const val DAY_MS = 86_400_000L

    fun shouldAsk(now: Long, state: ReviewPromptState, moment: ReviewMoment, pacing: ReviewPacing): Boolean {
        val firstSeen = state.firstSeenAt ?: return false
        val enoughFills = moment != ReviewMoment.AUTOFILL_WORKED || state.autofillFills >= pacing.fillsBeforeAsk
        val settled = now - firstSeen >= pacing.minDays * DAY_MS
        val cooled =
            state.lastAskedAt?.let { last -> now >= last && now - last >= pacing.cooldownDays * DAY_MS } ?: true
        return pacing.enabled && enoughFills && settled && cooled
    }
}
