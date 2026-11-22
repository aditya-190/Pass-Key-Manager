package com.bhardwaj.passkey.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigKeysTest {
    @Test
    fun `keys are unique snake_case names within Remote Config limits`() {
        val names = ConfigKeys.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
        names.forEach { assertTrue(it, Regex("^[a-z][a-z0-9_]{0,255}$").matches(it)) }
    }

    @Test
    fun `defaults map covers every key`() {
        assertEquals(ConfigKeys.all.size, ConfigKeys.defaultsMap().size)
    }

    @Test
    fun `map reader falls back to defaults for missing or mistyped values`() {
        val config = AppConfig(MapConfigReader(mapOf("latest_version_code" to "oops", "maintenance_mode" to true)))
        assertEquals(1L, config.latestVersionCode)
        assertTrue(config.maintenanceMode)
    }

    @Test
    fun `store url falls back to the canonical Play listing`() {
        assertEquals("https://play.google.com/store/apps/details?id=a.b", AppConfig().storeUrl("a.b"))
        val custom = AppConfig(MapConfigReader(mapOf("play_store_url" to "https://x.test/app")))
        assertEquals("https://x.test/app", custom.storeUrl("a.b"))
    }
}
