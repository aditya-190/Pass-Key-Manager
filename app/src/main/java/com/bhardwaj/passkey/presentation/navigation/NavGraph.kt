package com.bhardwaj.passkey.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.consent.ConsentRoute
import com.bhardwaj.passkey.legal.LegalViewModel
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailScreen
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.OnBoardingScreen
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewScreen
import com.bhardwaj.passkey.presentation.screens.security_screen.VaultGateScreen
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsScreen
import com.bhardwaj.passkey.presentation.screens.splash_screen.SplashPage
import com.bhardwaj.passkey.utils.Categories

@Composable
fun NavGraph(navController: NavHostController, startDestination: String, analytics: AnalyticsTracker) {
    // Screen views carry only the route name; AppEvents.screenView strips arguments such as entry ids.
    DisposableEffect(navController, analytics) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { analytics.log(AppEvents.screenView(it)) }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(route = NavScreens.SplashPage.route) {
            SplashPage(
                onNavigate = {
                    navController.popBackStack()
                    navController.navigate(it.route)
                }
            )
        }
        composable(route = NavScreens.OnboardingPage.route) {
            OnBoardingScreen(
                onNavigate = {
                    navController.popBackStack()
                    navController.navigate(it.route)
                }
            )
        }
        composable(
            route = NavScreens.PreviewPage.route + "?categoryName={categoryName}",
            arguments = listOf(
                navArgument(name = "categoryName") {
                    type = NavType.StringType
                    defaultValue = Categories.BANKS.name
                }
            )
        ) {
            PreviewScreen(onNavigate = { navController.navigate(it.route) })
        }
        composable(
            route = NavScreens.DetailsPage.route + "?previewId={previewId}",
            arguments = listOf(
                navArgument(name = "previewId") {
                    type = NavType.LongType
                    defaultValue = -1
                }
            )
        ) {
            DetailScreen(onPopBackStack = { navController.popBackStack() })
        }
        composable(route = NavScreens.SettingPage.route) {
            SettingsScreen(
                onPopBackStack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(
            route = Routes.CONSENT_PAGE + "?${Routes.CONSENT_FROM_SETTINGS_ARG}={${Routes.CONSENT_FROM_SETTINGS_ARG}}",
            arguments = listOf(
                navArgument(name = Routes.CONSENT_FROM_SETTINGS_ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { entry ->
            val fromSettings = entry.arguments?.getBoolean(Routes.CONSENT_FROM_SETTINGS_ARG) ?: false
            ConsentRoute(
                fromSettings = fromSettings,
                onFinished = {
                    if (fromSettings) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.SECURITY_PAGE) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Routes.ABOUT_PAGE) {
            AboutRoute(
                onBack = { navController.popBackStack() },
                onOpenLicences = { navController.navigate(Routes.LICENCES_PAGE) },
                onOpenLegal = { document -> navController.navigate(Routes.LEGAL_PAGE + "/" + document.name) }
            )
        }
        composable(
            route = Routes.LEGAL_PAGE + "/{" + LegalViewModel.ARG_DOCUMENT + "}",
            arguments = listOf(navArgument(LegalViewModel.ARG_DOCUMENT) { type = NavType.StringType })
        ) {
            LegalRoute(onBack = { navController.popBackStack() })
        }
        composable(route = Routes.LICENCES_PAGE) {
            LicencesRoute(onBack = { navController.popBackStack() })
        }
        composable(route = NavScreens.SecurityPage.route) {
            VaultGateScreen(
                onUnlocked = {
                    navController.navigate(NavScreens.PreviewPage.route) {
                        // Nothing behind the gate should survive it.
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
