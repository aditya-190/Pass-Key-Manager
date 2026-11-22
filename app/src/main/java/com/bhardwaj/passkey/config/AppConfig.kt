package com.bhardwaj.passkey.config

/** Typed read access to config values; implemented over Firebase Remote Config and by test fakes. */
interface ConfigReader {
    fun <T : Any> get(key: ConfigKey<T>): T
}

/** Reader that always returns the in-app defaults (no Firebase, previews, tests). */
object DefaultConfigReader : ConfigReader {
    override fun <T : Any> get(key: ConfigKey<T>): T = key.default
}

/** Reader over a plain map (debug overrides, tests). Missing or mistyped values fall back to the default. */
class MapConfigReader(private val values: Map<String, Any>) : ConfigReader {
    override fun <T : Any> get(key: ConfigKey<T>): T {
        val value = values[key.name] ?: return key.default
        @Suppress("UNCHECKED_CAST")
        return if (key.default::class.isInstance(value)) value as T else key.default
    }
}

/** Immutable snapshot of the active config. Read the latest one via [ConfigProvider]. */
class AppConfig(private val reader: ConfigReader = DefaultConfigReader) {
    operator fun <T : Any> get(key: ConfigKey<T>): T = reader.get(key)

    val minSupportedVersionCode get() = this[ConfigKeys.MIN_SUPPORTED_VERSION_CODE]
    val latestVersionCode get() = this[ConfigKeys.LATEST_VERSION_CODE]
    val forceUpdateEnabled get() = this[ConfigKeys.FORCE_UPDATE_ENABLED]
    val maintenanceMode get() = this[ConfigKeys.MAINTENANCE_MODE]
    val inAppUpdateEnabled get() = this[ConfigKeys.IN_APP_UPDATE_ENABLED]
    val configVersion get() = this[ConfigKeys.CONFIG_VERSION]

    /** Store listing for this app: the RC override, else the canonical Play URL for [packageName]. */
    fun storeUrl(packageName: String): String =
        this[ConfigKeys.PLAY_STORE_URL].ifBlank { "https://play.google.com/store/apps/details?id=$packageName" }
}

interface ConfigProvider {
    val current: AppConfig
}
