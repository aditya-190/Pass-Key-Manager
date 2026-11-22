package com.bhardwaj.passkey.update

import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKeys

sealed interface UpdateDecision {
    data object None : UpdateDecision

    data object Maintenance : UpdateDecision

    /** Below `min_supported_version_code` with `force_update_enabled`: the app is blocked. */
    data object Hard : UpdateDecision

    /** Below `latest_version_code`: offer the update, never block. */
    data class Soft(val latestVersionCode: Long) : UpdateDecision
}

/** Pure decision logic for the update gate. */
object ForceUpdateManager {
    const val DAY_MS = 24L * 60 * 60 * 1000

    fun decide(
        currentVersionCode: Long,
        maintenanceMode: Boolean,
        forceUpdateEnabled: Boolean,
        minSupportedVersionCode: Long,
        latestVersionCode: Long
    ): UpdateDecision = when {
        maintenanceMode -> UpdateDecision.Maintenance
        forceUpdateEnabled && currentVersionCode < minSupportedVersionCode -> UpdateDecision.Hard
        currentVersionCode < latestVersionCode -> UpdateDecision.Soft(latestVersionCode)
        else -> UpdateDecision.None
    }

    fun decide(currentVersionCode: Long, config: AppConfig): UpdateDecision = decide(
        currentVersionCode = currentVersionCode,
        maintenanceMode = config.maintenanceMode,
        forceUpdateEnabled = config.forceUpdateEnabled,
        minSupportedVersionCode = config.minSupportedVersionCode,
        latestVersionCode = config.latestVersionCode
    )

    /**
     * The soft prompt shows at most once per [intervalDays] for the same latest version. A newer latest
     * version resets the throttle. [intervalDays] comes from `soft_update_interval_days` (min 1).
     */
    fun shouldShowSoftPrompt(
        latestVersionCode: Long,
        lastDismissedVersionCode: Long?,
        lastDismissedAtMs: Long?,
        nowMs: Long,
        intervalDays: Long = 1
    ): Boolean {
        if (lastDismissedVersionCode != latestVersionCode || lastDismissedAtMs == null) return true
        return nowMs - lastDismissedAtMs >= intervalDays.coerceAtLeast(1) * DAY_MS
    }

    fun softIntervalDays(config: AppConfig): Long = config[ConfigKeys.SOFT_UPDATE_INTERVAL_DAYS]
}
