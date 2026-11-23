package com.bhardwaj.passkey.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.consent.ConsentRoute
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailScreen
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingScreen
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewScreen
import com.bhardwaj.passkey.presentation.screens.security_screen.VaultGateScreen
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsScreen
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashPage

/** Splash is a fixed start destination: changing NavHost's start after first composition resets the graph. */
@Composable
fun NavGraph(navController: NavHostController, analytics: AnalyticsTracker) {
    // Screen views carry only the screen name; never an argument such as an entry id.
    DisposableEffect(navController, analytics) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { analytics.log(AppEvents.screenView(RouteNames.screenName(it))) }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    NavHost(navController = navController, startDestination = NavRoute.Splash) {
        composable<NavRoute.Splash> {
            SplashPage(
                onNavigate = {
                    navController.popBackStack()
                    navController.navigate(it.route)
                }
            )
        }
        composable<NavRoute.Onboarding> {
            OnBoardingScreen(
                onNavigate = {
                    navController.popBackStack()
                    navController.navigate(it.route)
                }
            )
        }
        composable<NavRoute.Previews> {
            PreviewScreen(onNavigate = { navController.navigate(it) })
        }
        composable<NavRoute.Details> {
            DetailScreen(onPopBackStack = { navController.popBackStack() })
        }
        composable<NavRoute.Settings> {
            SettingsScreen(
                onPopBackStack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable<NavRoute.Consent> { entry ->
            val fromSettings = entry.toRoute<NavRoute.Consent>().fromSettings
            ConsentRoute(
                fromSettings = fromSettings,
                onFinished = {
                    if (fromSettings) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoute.Security) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<NavRoute.About> {
            AboutRoute(
                onBack = { navController.popBackStack() },
                onOpenLicences = { navController.navigate(NavRoute.Licences) },
                onOpenLegal = { document -> navController.navigate(NavRoute.Legal(document.name)) }
            )
        }
        composable<NavRoute.Legal> {
            LegalRoute(onBack = { navController.popBackStack() })
        }
        composable<NavRoute.Licences> {
            LicencesRoute(onBack = { navController.popBackStack() })
        }
        composable<NavRoute.Security> {
            VaultGateScreen(
                onUnlocked = {
                    navController.navigate(NavRoute.Previews) {
                        // Nothing behind the gate should survive it.
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
