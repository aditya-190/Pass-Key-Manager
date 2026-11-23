package com.bhardwaj.passkey.presentation.navigation

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

/**
 * Type-safe destinations. Replaces string constants that callers concatenated by hand:
 * `Routes.DETAILS_PAGE + "?previewId=$id"` appeared in two ViewModels as independent copies of one URL format,
 * and nothing checked that an argument name matched the graph.
 *
 * Property names are the SavedStateHandle keys the destination's ViewModel reads (`previewId`, `document`).
 */
sealed interface NavRoute {
    @Serializable
    data object Splash : NavRoute

    @Serializable
    data object Onboarding : NavRoute

    /** The feature tour on its own, replayed from Settings (or an announcement); it pops back when done. */
    @Serializable
    data object FeatureTour : NavRoute

    /** Opened from Settings it pops back instead of continuing to unlock. */
    @Serializable
    data class Consent(val fromSettings: Boolean = false) : NavRoute

    @Serializable
    data object Security : NavRoute

    @Serializable
    data object Previews : NavRoute

    @Serializable
    data class Details(val previewId: Long) : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object About : NavRoute

    /** [document] is a `LegalDocument` name; LegalViewModel falls back safely on anything else. */
    @Serializable
    data class Legal(val document: String) : NavRoute

    @Serializable
    data object Licences : NavRoute
}

/**
 * The serial name Navigation uses as the base of a destination's route string. Generated at compile time from
 * the source name, so R8 renaming the class does not change it.
 */
inline fun <reified T : NavRoute> routeName(): String = serializer<T>().descriptor.serialName

object RouteNames {
    /** Screen names as the analytics dashboards have always seen them (the pre-5.8.0 string routes). */
    private val screenNames: Map<String, String> = mapOf(
        routeName<NavRoute.Splash>() to "splash_page",
        routeName<NavRoute.Onboarding>() to "onboarding_page",
        routeName<NavRoute.Consent>() to "consent_page",
        routeName<NavRoute.Security>() to "security_page",
        routeName<NavRoute.Previews>() to "preview_page",
        routeName<NavRoute.Details>() to "details_page",
        routeName<NavRoute.Settings>() to "settings_page",
        routeName<NavRoute.About>() to "about_page",
        routeName<NavRoute.Legal>() to "legal_page",
        routeName<NavRoute.Licences>() to "licences_page",
        routeName<NavRoute.FeatureTour>() to "feature_tour_page"
    )

    /** A destination route without its argument pattern or values. */
    fun baseOf(route: String): String = route.substringBefore('?').substringBefore('/')

    /** Never carries an argument value such as an entry id. */
    fun screenName(route: String): String = baseOf(route).let { screenNames[it] ?: it.substringAfterLast('.') }
}
