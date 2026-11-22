package com.bhardwaj.passkey.analytics

/**
 * Events owned by the baseline features. Merge into the app's own typed catalogue (`Events.kt`) if it has
 * one, and list every event in `docs/ANALYTICS_EVENTS.md`. Standard Firebase names are used where they exist.
 */
object BaselineEventNames {
    const val SCREEN_VIEW = "screen_view"
    const val REMOTE_CONFIG_FETCH = "remote_config_fetch"
    const val CONSENT_UPDATED = "consent_updated"

    const val FORCE_UPDATE_SHOWN = "force_update_shown"
    const val FORCE_UPDATE_CLICKED = "force_update_clicked"
    const val SOFT_UPDATE_SHOWN = "soft_update_shown"
    const val SOFT_UPDATE_DISMISSED = "soft_update_dismissed"
    const val MAINTENANCE_SHOWN = "maintenance_shown"
    const val IN_APP_UPDATE_STARTED = "in_app_update_started"
    const val IN_APP_UPDATE_RESULT = "in_app_update_result"

    const val ANNOUNCEMENT_SHOWN = "announcement_shown"
    const val ANNOUNCEMENT_CLICKED = "announcement_clicked"
    const val ANNOUNCEMENT_DISMISSED = "announcement_dismissed"

    const val TUTORIAL_BEGIN = "tutorial_begin"
    const val TUTORIAL_COMPLETE = "tutorial_complete"
    const val ONBOARDING_PAGE_VIEW = "onboarding_page_view"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
    const val ONBOARDING_REPLAYED = "onboarding_replayed"

    const val ABOUT_ACTION = "about_action"
    const val LEGAL_VIEWED = "legal_viewed"
    const val LEGAL_OPENED_ONLINE = "legal_opened_online"
}

object BaselineParams {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val SUCCESS = "success"
    const val DURATION_MS = "duration_ms"
    const val ANALYTICS = "analytics"
    const val CRASH = "crash"
    const val ADS = "ads"
    const val VERSION_CODE = "version_code"
    const val UPDATE_TYPE = "update_type"
    const val RESULT = "result"
    const val ID = "id"
    const val STYLE = "style"
    const val TARGET = "target"
    const val PAGE = "page"
    const val ACTION = "action"
    const val DOCUMENT = "document"
}

/** User property names (max 25 per Firebase project). */
object BaselineUserProps {
    const val CONSENT_ANALYTICS = "consent_analytics"
}

/** Typed factories so call sites can't misspell names or params. */
object BaselineEvents {
    fun screenView(screenName: String, screenClass: String) = AnalyticsEvent(
        BaselineEventNames.SCREEN_VIEW,
        mapOf(BaselineParams.SCREEN_NAME to screenName, BaselineParams.SCREEN_CLASS to screenClass)
    )

    fun remoteConfigFetch(success: Boolean, durationMs: Long) = AnalyticsEvent(
        BaselineEventNames.REMOTE_CONFIG_FETCH,
        mapOf(BaselineParams.SUCCESS to success, BaselineParams.DURATION_MS to durationMs)
    )

    fun consentUpdated(analytics: Boolean, crash: Boolean, ads: Boolean) = AnalyticsEvent(
        BaselineEventNames.CONSENT_UPDATED,
        mapOf(BaselineParams.ANALYTICS to analytics, BaselineParams.CRASH to crash, BaselineParams.ADS to ads)
    )

    fun forceUpdateShown(versionCode: Long) =
        AnalyticsEvent(BaselineEventNames.FORCE_UPDATE_SHOWN, mapOf(BaselineParams.VERSION_CODE to versionCode))

    fun forceUpdateClicked() = AnalyticsEvent(BaselineEventNames.FORCE_UPDATE_CLICKED)

    fun softUpdateShown(latestVersionCode: Long) =
        AnalyticsEvent(BaselineEventNames.SOFT_UPDATE_SHOWN, mapOf(BaselineParams.VERSION_CODE to latestVersionCode))

    fun softUpdateDismissed(latestVersionCode: Long) = AnalyticsEvent(
        BaselineEventNames.SOFT_UPDATE_DISMISSED,
        mapOf(BaselineParams.VERSION_CODE to latestVersionCode)
    )

    fun maintenanceShown() = AnalyticsEvent(BaselineEventNames.MAINTENANCE_SHOWN)

    /** [updateType]: "immediate" or "flexible". */
    fun inAppUpdateStarted(updateType: String) =
        AnalyticsEvent(BaselineEventNames.IN_APP_UPDATE_STARTED, mapOf(BaselineParams.UPDATE_TYPE to updateType))

    /** [result]: "ok", "cancelled", "failed" or "installed". */
    fun inAppUpdateResult(updateType: String, result: String) = AnalyticsEvent(
        BaselineEventNames.IN_APP_UPDATE_RESULT,
        mapOf(BaselineParams.UPDATE_TYPE to updateType, BaselineParams.RESULT to result)
    )

    fun announcementShown(id: String, style: String) = AnalyticsEvent(
        BaselineEventNames.ANNOUNCEMENT_SHOWN,
        mapOf(BaselineParams.ID to id.take(EventValidator.MAX_VALUE_LENGTH), BaselineParams.STYLE to style)
    )

    /** [target]: "url" or "route". */
    fun announcementClicked(id: String, target: String) = AnalyticsEvent(
        BaselineEventNames.ANNOUNCEMENT_CLICKED,
        mapOf(BaselineParams.ID to id.take(EventValidator.MAX_VALUE_LENGTH), BaselineParams.TARGET to target)
    )

    fun announcementDismissed(id: String) = AnalyticsEvent(
        BaselineEventNames.ANNOUNCEMENT_DISMISSED,
        mapOf(BaselineParams.ID to id.take(EventValidator.MAX_VALUE_LENGTH))
    )

    fun tutorialBegin() = AnalyticsEvent(BaselineEventNames.TUTORIAL_BEGIN)

    fun tutorialComplete() = AnalyticsEvent(BaselineEventNames.TUTORIAL_COMPLETE)

    /** [page] is 1-based. */
    fun onboardingPageView(page: Int) =
        AnalyticsEvent(BaselineEventNames.ONBOARDING_PAGE_VIEW, mapOf(BaselineParams.PAGE to page))

    fun onboardingSkipped(page: Int) =
        AnalyticsEvent(BaselineEventNames.ONBOARDING_SKIPPED, mapOf(BaselineParams.PAGE to page))

    fun onboardingReplayed() = AnalyticsEvent(BaselineEventNames.ONBOARDING_REPLAYED)

    /** [action]: rate, share, contact, more_apps, licences, terms, privacy. */
    fun aboutAction(action: String) =
        AnalyticsEvent(BaselineEventNames.ABOUT_ACTION, mapOf(BaselineParams.ACTION to action))

    /** [document]: "terms" or "privacy". */
    fun legalViewed(document: String) =
        AnalyticsEvent(BaselineEventNames.LEGAL_VIEWED, mapOf(BaselineParams.DOCUMENT to document))

    fun legalOpenedOnline(document: String) =
        AnalyticsEvent(BaselineEventNames.LEGAL_OPENED_ONLINE, mapOf(BaselineParams.DOCUMENT to document))
}
