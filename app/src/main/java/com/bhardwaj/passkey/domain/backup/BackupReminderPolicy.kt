package com.bhardwaj.passkey.domain.backup

import com.bhardwaj.passkey.domain.repository.BackupReminderState

/**
 * When Home reminds the user to back up (6.0.0). Lost vaults after a reset or a new phone are the top complaint in
 * this category, and a backup is the only way back. Wall-clock time is fine at this scale (days), but the clock can
 * be moved: a snooze can never last longer than one snooze, and a backup dated in the future is not trusted.
 */
object BackupReminderPolicy {
    const val DAY_MS = 86_400_000L
    const val FIRST_REMINDER_MS = 3 * DAY_MS
    const val REMIND_AFTER_MS = 30 * DAY_MS
    const val SNOOZE_MS = 7 * DAY_MS
    const val CTA_SNOOZE_MS = 1 * DAY_MS
    private const val CLOCK_SKEW_MS = 1 * DAY_MS

    fun isDue(now: Long, state: BackupReminderState, entryCount: Int): Boolean {
        if (entryCount <= 0 || isSnoozed(now, state.snoozedUntil)) return false
        val last = state.lastBackupAt
        return if (last != null) {
            isOlderThan(now, last, REMIND_AFTER_MS)
        } else {
            // The install's first look is recorded on the first Home visit; until then there is nothing to measure.
            state.firstSeenAt?.let { isOlderThan(now, it, FIRST_REMINDER_MS) } ?: false
        }
    }

    private fun isSnoozed(now: Long, snoozedUntil: Long?): Boolean =
        snoozedUntil != null && now < snoozedUntil && snoozedUntil - now <= SNOOZE_MS

    /** A time more than a day in the future means the clock went back: treat it as unknown, so remind. */
    private fun isOlderThan(now: Long, since: Long, limit: Long): Boolean {
        val age = now - since
        return age >= limit || age < -CLOCK_SKEW_MS
    }
}
