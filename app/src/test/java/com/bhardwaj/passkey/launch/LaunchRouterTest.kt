package com.bhardwaj.passkey.launch

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRouterTest {
    @Test
    fun `new users see onboarding first`() {
        assertEquals(LaunchStep.Onboarding, LaunchRouter.atStart(onboardingDone = false, consentDecided = false))
        assertEquals(LaunchStep.Onboarding, LaunchRouter.atStart(onboardingDone = false, consentDecided = true))
    }

    @Test
    fun `existing users who never chose are asked once, before the vault`() {
        assertEquals(LaunchStep.Consent, LaunchRouter.atStart(onboardingDone = true, consentDecided = false))
    }

    @Test
    fun `decided users go straight to unlock`() {
        assertEquals(LaunchStep.Unlock, LaunchRouter.atStart(onboardingDone = true, consentDecided = true))
    }

    @Test
    fun `after onboarding, consent comes next unless already decided`() {
        assertEquals(LaunchStep.Consent, LaunchRouter.afterOnboarding(consentDecided = false))
        assertEquals(LaunchStep.Unlock, LaunchRouter.afterOnboarding(consentDecided = true))
    }
}
