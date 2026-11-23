package com.bhardwaj.passkey.launch

enum class LaunchStep { Onboarding, Consent, Unlock }

/**
 * Launch order: onboarding → consent → unlock. Upgrading users who finished onboarding before consent
 * existed are asked once, before the vault opens; after that the choice lives in Settings.
 */
object LaunchRouter {
    fun atStart(onboardingDone: Boolean, consentDecided: Boolean): LaunchStep = when {
        !onboardingDone -> LaunchStep.Onboarding
        else -> afterOnboarding(consentDecided)
    }

    fun afterOnboarding(consentDecided: Boolean): LaunchStep =
        if (consentDecided) LaunchStep.Unlock else LaunchStep.Consent
}
