package com.bhardwaj.passkey.banner

import com.bhardwaj.passkey.presentation.navigation.Routes

/**
 * Screens a Remote Config announcement may open. Anything else is ignored: an unknown route made
 * NavController.navigate throw, so one typo in the console would have crashed the app for everyone who
 * tapped the banner, and vault-internal screens must never be reachable from a remote string.
 */
object AnnouncementRoutes {
    private val allowed = setOf(
        Routes.SETTINGS_PAGE,
        Routes.ABOUT_PAGE,
        Routes.LEGAL_PAGE + "/PRIVACY",
        Routes.LEGAL_PAGE + "/TERMS",
        Routes.CONSENT_PAGE + "?" + Routes.CONSENT_FROM_SETTINGS_ARG + "=true"
    )

    fun safeRoute(route: String): String? = route.trim().takeIf { it in allowed }
}
