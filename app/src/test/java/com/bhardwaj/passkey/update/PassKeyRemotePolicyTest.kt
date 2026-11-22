package com.bhardwaj.passkey.update

import com.bhardwaj.passkey.banner.AnnouncementRoutes
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.MapConfigReader
import com.bhardwaj.passkey.presentation.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 5.6.0 attack-pass findings 1 and 2: what Remote Config may and may not do to an offline vault. */
class PassKeyRemotePolicyTest {
    private fun config(values: Map<String, Any>) = AppConfig(MapConfigReader(values))

    @Test
    fun `maintenance mode never locks people out of an offline vault`() {
        val remote = config(mapOf("maintenance_mode" to true))
        assertEquals(UpdateDecision.None, ForceUpdateManager.decide(44, withoutMaintenance(remote)))
    }

    @Test
    fun `a real forced update still applies`() {
        val remote = config(
            mapOf("maintenance_mode" to true, "force_update_enabled" to true, "min_supported_version_code" to 50L)
        )
        assertEquals(UpdateDecision.Hard, ForceUpdateManager.decide(44, withoutMaintenance(remote)))
    }

    @Test
    fun `everything other than maintenance passes through unchanged`() {
        val remote = config(mapOf("support_email" to "help@example.com", "latest_version_code" to 99L))
        val filtered = withoutMaintenance(remote)
        assertEquals("help@example.com", filtered[com.bhardwaj.passkey.config.ConfigKeys.SUPPORT_EMAIL])
        assertEquals(99L, filtered.latestVersionCode)
    }

    @Test
    fun `announcement links may only open known safe screens`() {
        listOf(
            Routes.SETTINGS_PAGE,
            Routes.ABOUT_PAGE,
            Routes.LEGAL_PAGE + "/PRIVACY",
            Routes.LEGAL_PAGE + "/TERMS",
            Routes.CONSENT_PAGE + "?fromSettings=true"
        ).forEach { assertEquals(it, it, AnnouncementRoutes.safeRoute(it)) }
    }

    @Test
    fun `unknown, malformed or vault-internal links are dropped instead of crashing`() {
        listOf(
            "settngs_page",
            "",
            "   ",
            Routes.LEGAL_PAGE + "/SECRETS",
            Routes.DETAILS_PAGE + "?previewId=1",
            Routes.SECURITY_PAGE,
            "https://evil.example",
            Routes.SETTINGS_PAGE + "/../" + Routes.DETAILS_PAGE
        ).forEach { assertNull(it, AnnouncementRoutes.safeRoute(it)) }
    }
}
