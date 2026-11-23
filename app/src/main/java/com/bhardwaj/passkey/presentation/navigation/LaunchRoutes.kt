package com.bhardwaj.passkey.presentation.navigation

import com.bhardwaj.passkey.launch.LaunchStep

val LaunchStep.route: NavRoute
    get() = when (this) {
        LaunchStep.Onboarding -> NavRoute.Onboarding
        LaunchStep.Consent -> NavRoute.Consent()
        LaunchStep.Unlock -> NavRoute.Security
    }
