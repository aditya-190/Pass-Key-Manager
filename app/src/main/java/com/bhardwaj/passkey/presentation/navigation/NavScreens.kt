package com.bhardwaj.passkey.presentation.navigation

import com.bhardwaj.passkey.presentation.navigation.Routes.DETAILS_PAGE
import com.bhardwaj.passkey.presentation.navigation.Routes.ONBOARDING_PAGE
import com.bhardwaj.passkey.presentation.navigation.Routes.PREVIEW_PAGE
import com.bhardwaj.passkey.presentation.navigation.Routes.SECURITY_PAGE
import com.bhardwaj.passkey.presentation.navigation.Routes.SETTINGS_PAGE
import com.bhardwaj.passkey.presentation.navigation.Routes.SPLASH_PAGE

sealed class NavScreens(val route: String) {
    data object SplashPage : NavScreens(route = SPLASH_PAGE)
    data object OnboardingPage : NavScreens(route = ONBOARDING_PAGE)
    data object PreviewPage : NavScreens(route = PREVIEW_PAGE)
    data object DetailsPage : NavScreens(route = DETAILS_PAGE)
    data object SettingPage : NavScreens(route = SETTINGS_PAGE)
    data object SecurityPage : NavScreens(route = SECURITY_PAGE)
}