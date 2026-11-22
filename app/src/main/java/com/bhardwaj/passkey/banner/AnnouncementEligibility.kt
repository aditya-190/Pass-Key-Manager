package com.bhardwaj.passkey.banner

/** Pure rules for whether an announcement may show now. One banner at a time: the first eligible one wins. */
object AnnouncementEligibility {
    fun isEligible(announcement: Announcement, nowMs: Long, versionCode: Long, dismissedIds: Set<String>): Boolean {
        val start = announcement.startAtMs
        val end = announcement.endAtMs
        val min = announcement.minVersionCode
        val max = announcement.maxVersionCode
        return when {
            announcement.dismissible && announcement.id in dismissedIds -> false
            start != null && nowMs < start -> false
            end != null && nowMs >= end -> false
            min != null && versionCode < min -> false
            max != null && versionCode > max -> false
            else -> true
        }
    }

    fun pick(
        announcements: List<Announcement>,
        nowMs: Long,
        versionCode: Long,
        dismissedIds: Set<String>
    ): Announcement? = announcements.firstOrNull { isEligible(it, nowMs, versionCode, dismissedIds) }
}
