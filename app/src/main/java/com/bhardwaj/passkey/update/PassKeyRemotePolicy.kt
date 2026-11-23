package com.bhardwaj.passkey.update

import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKey
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.config.ConfigReader

/**
 * PassKey has no server, so there is never anything to be "in maintenance". A remote maintenance switch could
 * only lock people out of their own offline vault, and a cached "on" would outlive the fix on a phone that is
 * offline. The key is therefore always read as its default (off). Forced updates still apply.
 */
fun withoutMaintenance(config: AppConfig): AppConfig = AppConfig(
    object : ConfigReader {
        override fun <T : Any> get(key: ConfigKey<T>): T =
            if (key.name == ConfigKeys.MAINTENANCE_MODE.name) key.default else config[key]
    }
)
