package com.bhardwaj.passkey.review

import com.bhardwaj.passkey.analytics.AnalyticsEvent
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEventNames
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.config.ConfigProvider
import com.bhardwaj.passkey.core.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decides, at a good moment, whether the screen should start Play's in-app review (6.0.0). A "yes" is recorded
 * before the flow starts, so two moments at once can't both ask. Play applies its own quota on top and never says
 * whether a dialog appeared, so the event is "requested", not "shown".
 */
@Singleton
class ReviewPrompter @Inject constructor(
    private val store: ReviewPromptStore,
    private val clock: Clock,
    private val config: ConfigProvider,
    private val analytics: AnalyticsTracker
) {
    private val lock = Mutex()

    suspend fun onMoment(moment: ReviewMoment): Boolean = lock.withLock {
        val now = clock.nowMs()
        val ask = ReviewPromptPolicy.shouldAsk(now, store.state.first(), moment, pacing())
        if (ask) {
            store.recordAsked(now)
            analytics.log(
                AnalyticsEvent(AppEventNames.REVIEW_REQUESTED, mapOf(AppParams.SOURCE to moment.analyticsName))
            )
        }
        ask
    }

    suspend fun recordFill() = store.recordFill()

    /** Someone who just chose to rate shouldn't be asked again soon: counts as an ask for the cooldown. */
    suspend fun noteManualRate() = store.recordAsked(clock.nowMs())

    suspend fun markFirstSeen() = store.markFirstSeen(clock.nowMs())

    private fun pacing(): ReviewPacing {
        val current = config.current
        return ReviewPacing(
            enabled = current[ConfigKeys.REVIEW_PROMPT_ENABLED],
            minDays = current[ConfigKeys.REVIEW_MIN_DAYS],
            cooldownDays = current[ConfigKeys.REVIEW_COOLDOWN_DAYS],
            fillsBeforeAsk = current[ConfigKeys.REVIEW_AFTER_FILLS]
        )
    }
}
