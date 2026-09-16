package com.bhardwaj.passkey.analytics

import com.bhardwaj.passkey.domain.model.Category

/** Every event the app sends. Catalogued in docs/ANALYTICS_EVENTS.md; keep both in step. */
object AppEventNames {
    const val SCREEN_VIEW = "screen_view"
    const val CONSENT_UPDATED = "consent_updated"
    const val ONBOARDING_PAGE_VIEW = "onboarding_page_view"
    const val ONBOARDING_SKIPPED = "onboarding_skipped"
    const val ONBOARDING_COMPLETE = "tutorial_complete"
    const val ENTRY_CREATED = "entry_created"
    const val FIELD_SAVED = "field_saved"
    const val PASSWORD_GENERATED = "password_generated"
    const val VALUE_COPIED = "value_copied"
    const val VAULT_UNLOCKED = "vault_unlocked"
    const val UNLOCK_FAILED = "unlock_failed"
    const val BACKUP_EXPORTED = "backup_exported"
    const val BACKUP_IMPORTED = "backup_imported"
    const val SAFETY_CHECK_VIEWED = "safety_check_viewed"
    const val LANGUAGE_CHANGED = "language_changed"
    const val RECOVERY_SETUP_SHOWN = "recovery_setup_shown"
    const val RECOVERY_SETUP_COMPLETED = "recovery_setup_completed"
    const val VAULT_MIGRATION_FAILED = "vault_migration_failed"
}

object AppParams {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val ANALYTICS = "analytics"
    const val CRASH = "crash"
    const val PAGE = "page"
    const val CATEGORY = "category"
    const val IS_EDIT = "is_edit"
    const val LENGTH_BUCKET = "length_bucket"
    const val SENSITIVE = "sensitive"
    const val SUCCESS = "success"
    const val COUNT_BUCKET = "count_bucket"
    const val SCORE_BUCKET = "score_bucket"
    const val LANGUAGE = "language"
    const val EXISTING_VAULT = "existing_vault"
    const val REASON = "reason"
}

object AppUserProps {
    const val CONSENT_ANALYTICS = "consent_analytics"
}

/**
 * Typed factories, so call sites cannot misspell names or slip free text into a param.
 * Numbers that could fingerprint a vault (lengths, counts, scores) are only ever sent as buckets.
 */
object AppEvents {
    /** [route] may carry nav arguments such as an entry id; only the route name is sent. */
    fun screenView(route: String) = AnalyticsEvent(
        AppEventNames.SCREEN_VIEW,
        route.substringBefore('?').substringBefore('/').take(EventValidator.MAX_VALUE_LENGTH).let { name ->
            mapOf(AppParams.SCREEN_NAME to name, AppParams.SCREEN_CLASS to name)
        }
    )

    fun consentUpdated(analytics: Boolean, crash: Boolean) = AnalyticsEvent(
        AppEventNames.CONSENT_UPDATED,
        mapOf(AppParams.ANALYTICS to analytics, AppParams.CRASH to crash)
    )

    /** [page] is 1-based. */
    fun onboardingPageView(page: Int) =
        AnalyticsEvent(AppEventNames.ONBOARDING_PAGE_VIEW, mapOf(AppParams.PAGE to page))

    /** [page] is 1-based. */
    fun onboardingSkipped(page: Int) = AnalyticsEvent(AppEventNames.ONBOARDING_SKIPPED, mapOf(AppParams.PAGE to page))

    fun onboardingComplete() = AnalyticsEvent(AppEventNames.ONBOARDING_COMPLETE)

    fun entryCreated(category: Category) =
        AnalyticsEvent(AppEventNames.ENTRY_CREATED, mapOf(AppParams.CATEGORY to category.name.lowercase()))

    fun fieldSaved(isEdit: Boolean) = AnalyticsEvent(AppEventNames.FIELD_SAVED, mapOf(AppParams.IS_EDIT to isEdit))

    fun passwordGenerated(length: Int) =
        AnalyticsEvent(AppEventNames.PASSWORD_GENERATED, mapOf(AppParams.LENGTH_BUCKET to lengthBucket(length)))

    fun valueCopied(sensitive: Boolean) =
        AnalyticsEvent(AppEventNames.VALUE_COPIED, mapOf(AppParams.SENSITIVE to sensitive))

    fun vaultUnlocked() = AnalyticsEvent(AppEventNames.VAULT_UNLOCKED)

    fun unlockFailed() = AnalyticsEvent(AppEventNames.UNLOCK_FAILED)

    fun backupExported(entryCount: Int) =
        AnalyticsEvent(AppEventNames.BACKUP_EXPORTED, mapOf(AppParams.COUNT_BUCKET to countBucket(entryCount)))

    fun backupImported(success: Boolean, entryCount: Int) = AnalyticsEvent(
        AppEventNames.BACKUP_IMPORTED,
        mapOf(AppParams.SUCCESS to success, AppParams.COUNT_BUCKET to countBucket(entryCount))
    )

    fun safetyCheckViewed(score: Int) =
        AnalyticsEvent(AppEventNames.SAFETY_CHECK_VIEWED, mapOf(AppParams.SCORE_BUCKET to scoreBucket(score)))

    fun languageChanged(languageTag: String) = AnalyticsEvent(
        AppEventNames.LANGUAGE_CHANGED,
        mapOf(AppParams.LANGUAGE to languageTag.take(EventValidator.MAX_VALUE_LENGTH))
    )

    /** The one-time 5.7.0 step; [existingVault] separates upgrading users from new installs. */
    fun recoverySetupShown(existingVault: Boolean) =
        AnalyticsEvent(AppEventNames.RECOVERY_SETUP_SHOWN, mapOf(AppParams.EXISTING_VAULT to existingVault))

    fun recoverySetupCompleted(existingVault: Boolean) =
        AnalyticsEvent(AppEventNames.RECOVERY_SETUP_COMPLETED, mapOf(AppParams.EXISTING_VAULT to existingVault))

    /** [reason] is a VaultMigration failure enum name; nothing from the vault. */
    fun vaultMigrationFailed(reason: String) = AnalyticsEvent(
        AppEventNames.VAULT_MIGRATION_FAILED,
        mapOf(AppParams.REASON to reason.lowercase().take(EventValidator.MAX_VALUE_LENGTH))
    )

    fun lengthBucket(length: Int): String = when {
        length <= 0 -> "0"
        length < 8 -> "1-7"
        length < 12 -> "8-11"
        length < 16 -> "12-15"
        length < 24 -> "16-23"
        else -> "24+"
    }

    fun countBucket(count: Int): String = when {
        count <= 0 -> "0"
        count < 10 -> "1-9"
        count < 50 -> "10-49"
        count < 200 -> "50-199"
        else -> "200+"
    }

    fun scoreBucket(score: Int): String = when {
        score < 50 -> "0-49"
        score < 80 -> "50-79"
        else -> "80-100"
    }
}
