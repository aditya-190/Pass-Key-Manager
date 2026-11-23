package com.bhardwaj.passkey.banner

import com.bhardwaj.passkey.presentation.navigation.NavRoute

/**
 * Screens a Remote Config announcement may open. Anything else is ignored: an unknown route made
 * NavController.navigate throw, so one typo in the console would have crashed the app for everyone who
 * tapped the banner, and vault-internal screens must never be reachable from a remote string.
 *
 * The console keeps using the short strings it always has; they map to typed routes here, so a remote value
 * is never parsed into a route.
 */
object AnnouncementRoutes {
    private val allowed: Map<String, NavRoute> = mapOf(
        "settings_page" to NavRoute.Settings,
        "about_page" to NavRoute.About,
        "legal_page/PRIVACY" to NavRoute.Legal("PRIVACY"),
        "legal_page/TERMS" to NavRoute.Legal("TERMS"),
        "consent_page?fromSettings=true" to NavRoute.Consent(fromSettings = true),
        "feature_tour_page" to NavRoute.FeatureTour
    )

    fun safeRoute(route: String): NavRoute? = allowed[route.trim()]
}
