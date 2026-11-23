package com.bhardwaj.passkey.data.security

import com.bhardwaj.passkey.presentation.navigation.Routes

/**
 * Every re-lock decision, as pure functions of elapsed-realtime values so it can be tested without a
 * device. Callers pass `SystemClock.elapsedRealtime()`, never wall-clock time: changing the device clock
 * must not extend a grace period.
 */
object LockPolicy {
    /** Screens that come before the vault exists or is unlocked; a cold start must not interrupt them. */
    private val preVaultRoutes = setOf(
        Routes.SPLASH_PAGE,
        Routes.ONBOARDING_PAGE,
        Routes.CONSENT_PAGE,
        Routes.SECURITY_PAGE
    )

    /**
     * On returning to the foreground. [wasUnlocked] is the state when the app left: the system PIN screen
     * is a separate activity on some Android versions, so unlocking itself sends a *locked* app to the
     * background, and coming back from it must not lock again.
     */
    fun lockOnReturn(timeout: AutoLockTimeout, wasUnlocked: Boolean, backgroundedAt: Long, now: Long): Boolean {
        if (!wasUnlocked || timeout == AutoLockTimeout.NEVER) return false
        val away = now - backgroundedAt
        return away >= 0 && away >= timeout.millis
    }

    /**
     * Foreground idle. IMMEDIATELY means "when I leave the app", not "0 ms after my last touch": read as an idle
     * timeout it locked a vault in active use on every poll. Leaving covers the idle phone too, since the
     * screen timing out stops the app.
     */
    fun lockWhenIdle(timeout: AutoLockTimeout, isUnlocked: Boolean, idleMillis: Long): Boolean = isUnlocked &&
        timeout != AutoLockTimeout.NEVER &&
        timeout != AutoLockTimeout.IMMEDIATELY &&
        idleMillis >= timeout.millis

    /**
     * After a real lock every screen but the lock screen itself goes. On a cold start (including Android
     * restoring the back stack after process death) only vault screens go, so first-run screens still work.
     */
    fun mustShowLockScreen(isUnlocked: Boolean, reason: LockReason, currentRoute: String?): Boolean {
        if (isUnlocked || currentRoute == null) return false
        val base = currentRoute.substringBefore('?')
        if (base == Routes.SECURITY_PAGE) return false
        return reason != LockReason.ColdStart || base !in preVaultRoutes
    }
}
