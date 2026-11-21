package com.bhardwaj.passkey.presentation.navigation

import com.bhardwaj.passkey.launch.LaunchStep

val LaunchStep.route: String
    get() = when (this) {
        LaunchStep.Onboarding -> Routes.ONBOARDING_PAGE
        LaunchStep.Consent -> Routes.CONSENT_PAGE
        LaunchStep.Unlock -> Routes.SECURITY_PAGE
    }
