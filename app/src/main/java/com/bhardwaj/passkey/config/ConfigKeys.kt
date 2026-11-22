package com.bhardwaj.passkey.config

/**
 * Every Remote Config key and its safe in-app default. This is the single source of truth for defaults;
 * `firebase/remoteconfig.template.json` must match it (enforced by RemoteConfigDefaultsTest).
 * Add the app's own keys below the baseline ones and list them in [ConfigKeys.all].
 * Optional feature groups (ads: `ads/AdConfigKeys`) live with their feature and are passed to
 * `RemoteConfigManager` as `featureKeys`, so deleting the feature folder needs no edit here.
 */
sealed class ConfigKey<T : Any>(val name: String, val default: T) {
    class LongKey(name: String, default: Long) : ConfigKey<Long>(name, default)

    class BoolKey(name: String, default: Boolean) : ConfigKey<Boolean>(name, default)

    class StringKey(name: String, default: String) : ConfigKey<String>(name, default)
}

object ConfigKeys {
    // Force update / maintenance (update/)
    val MIN_SUPPORTED_VERSION_CODE = ConfigKey.LongKey("min_supported_version_code", 1)
    val LATEST_VERSION_CODE = ConfigKey.LongKey("latest_version_code", 1)
    val FORCE_UPDATE_ENABLED = ConfigKey.BoolKey("force_update_enabled", false)

    /** Kill switch for the Play In-App Update API; off = the gate screen / soft sheet only. */
    val IN_APP_UPDATE_ENABLED = ConfigKey.BoolKey("in_app_update_enabled", true)

    /** The soft update prompt shows at most once per this many days for the same latest version. */
    val SOFT_UPDATE_INTERVAL_DAYS = ConfigKey.LongKey("soft_update_interval_days", 3)
    val UPDATE_TITLE = ConfigKey.StringKey("update_title", "")
    val UPDATE_MESSAGE = ConfigKey.StringKey("update_message", "")

    /** Empty = `https://play.google.com/store/apps/details?id=<applicationId>`. */
    val PLAY_STORE_URL = ConfigKey.StringKey("play_store_url", "")
    val MAINTENANCE_MODE = ConfigKey.BoolKey("maintenance_mode", false)
    val MAINTENANCE_MESSAGE = ConfigKey.StringKey("maintenance_message", "")

    // Announcement banner (banner/): JSON object or array, see Announcement.kt. Empty = no banner.
    val ANNOUNCEMENT_JSON = ConfigKey.StringKey("announcement_json", "")

    // Legal and About (legal/, about/). Terms are in-app only for now (empty hides "read online").
    val TERMS_URL = ConfigKey.StringKey("terms_url", "")
    val PRIVACY_URL = ConfigKey.StringKey(
        "privacy_url",
        "https://github.com/aditya-190/Pass-Key-Manager/blob/master/Privacy%20Policy.md"
    )
    val SUPPORT_EMAIL = ConfigKey.StringKey("support_email", "yrkkh.cclub@gmail.com")

    /** Play developer page ("More apps"). */
    val MORE_APPS_URL = ConfigKey.StringKey("more_apps_url", "")

    /** Bumped by hand when the template is published; reported to Crashlytics. */
    val CONFIG_VERSION = ConfigKey.LongKey("config_version", 0)

    val all: List<ConfigKey<*>> =
        listOf(
            MIN_SUPPORTED_VERSION_CODE,
            LATEST_VERSION_CODE,
            FORCE_UPDATE_ENABLED,
            IN_APP_UPDATE_ENABLED,
            SOFT_UPDATE_INTERVAL_DAYS,
            UPDATE_TITLE,
            UPDATE_MESSAGE,
            PLAY_STORE_URL,
            MAINTENANCE_MODE,
            MAINTENANCE_MESSAGE,
            ANNOUNCEMENT_JSON,
            TERMS_URL,
            PRIVACY_URL,
            SUPPORT_EMAIL,
            MORE_APPS_URL,
            CONFIG_VERSION
        )

    /** Firebase defaults for the baseline keys plus any optional feature groups (e.g. `AdConfigKeys.all`). */
    fun defaultsMap(featureKeys: List<ConfigKey<*>> = emptyList()): Map<String, Any> =
        (all + featureKeys).associate { it.name to it.default }
}
