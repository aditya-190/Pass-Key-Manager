package com.bhardwaj.passkey.navigation

import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.analytics.AppParams
import com.bhardwaj.passkey.banner.AnnouncementRoutes
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.navigation.RouteNames
import com.bhardwaj.passkey.presentation.navigation.routeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Type-safe routes. A destination's route string is its serial name plus an argument pattern, e.g.
 * `<serialName>/{previewId}` for a required argument and `<serialName>?fromSettings={fromSettings}` for one with
 * a default. The patterns below mirror what Navigation generates.
 */
class NavRouteTest {
    private val details = routeName<NavRoute.Details>() + "/{previewId}"
    private val consent = routeName<NavRoute.Consent>() + "?fromSettings={fromSettings}"
    private val legal = routeName<NavRoute.Legal>() + "/{document}"

    @Test
    fun `screen views keep the names the analytics dashboards already use`() {
        mapOf(
            routeName<NavRoute.Splash>() to "splash_page",
            routeName<NavRoute.Onboarding>() to "onboarding_page",
            consent to "consent_page",
            routeName<NavRoute.Security>() to "security_page",
            routeName<NavRoute.Previews>() to "preview_page",
            details to "details_page",
            routeName<NavRoute.Settings>() to "settings_page",
            routeName<NavRoute.About>() to "about_page",
            legal to "legal_page",
            routeName<NavRoute.Licences>() to "licences_page"
        ).forEach { (route, name) -> assertEquals(route, name, RouteNames.screenName(route)) }
    }

    @Test
    fun `a screen view never carries an argument such as an entry id`() {
        val event = AppEvents.screenView(RouteNames.screenName(details.replace("{previewId}", "42")))
        assertEquals("details_page", event.params[AppParams.SCREEN_NAME])
    }

    @Test
    fun `unknown routes are reported by their short class name, not a package path`() {
        assertEquals("Mystery", RouteNames.screenName("com.example.Mystery/{id}"))
    }

    @Test
    fun `remote announcements keep the strings already used in the console, mapped to typed routes`() {
        mapOf(
            "settings_page" to NavRoute.Settings,
            "about_page" to NavRoute.About,
            "legal_page/PRIVACY" to NavRoute.Legal("PRIVACY"),
            "legal_page/TERMS" to NavRoute.Legal("TERMS"),
            " consent_page?fromSettings=true " to NavRoute.Consent(fromSettings = true)
        ).forEach { (text, route) -> assertEquals(text, route, AnnouncementRoutes.safeRoute(text)) }
    }

    @Test
    fun `serial names and vault screens can never be opened from a remote string`() {
        listOf(
            details,
            routeName<NavRoute.Details>(),
            routeName<NavRoute.Settings>(),
            "details_page?previewId=1",
            "security_page",
            "preview_page"
        ).forEach { assertNull(it, AnnouncementRoutes.safeRoute(it)) }
    }
}
