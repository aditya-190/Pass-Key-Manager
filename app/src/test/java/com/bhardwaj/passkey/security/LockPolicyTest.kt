package com.bhardwaj.passkey.security

import com.bhardwaj.passkey.data.security.AutoLockTimeout
import com.bhardwaj.passkey.data.security.LockPolicy
import com.bhardwaj.passkey.data.security.LockReason
import com.bhardwaj.passkey.presentation.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    @Test
    fun `returning after the timeout re-locks, returning sooner does not`() {
        val t = AutoLockTimeout.SECONDS_30
        assertTrue(LockPolicy.lockOnReturn(t, wasUnlocked = true, backgroundedAt = 1_000, now = 31_000))
        assertFalse(LockPolicy.lockOnReturn(t, wasUnlocked = true, backgroundedAt = 1_000, now = 30_999))
    }

    @Test
    fun `immediately means any trip to the background re-locks`() {
        assertTrue(LockPolicy.lockOnReturn(AutoLockTimeout.IMMEDIATELY, true, backgroundedAt = 5, now = 5))
    }

    @Test
    fun `never means never`() {
        assertFalse(LockPolicy.lockOnReturn(AutoLockTimeout.NEVER, true, backgroundedAt = 0, now = Long.MAX_VALUE))
        assertFalse(LockPolicy.lockWhenIdle(AutoLockTimeout.NEVER, isUnlocked = true, idleMillis = Long.MAX_VALUE))
    }

    @Test
    fun `the system PIN screen backgrounding a locked app must not re-lock it after a successful unlock`() {
        // On API 28 the device-credential screen is a separate activity: the app goes to the background
        // while still locked, the user unlocks, and the app returns. That return is not "coming back".
        assertFalse(
            LockPolicy.lockOnReturn(AutoLockTimeout.IMMEDIATELY, wasUnlocked = false, backgroundedAt = 0, now = 9_999)
        )
    }

    @Test
    fun `a clock that seems to run backwards never unlocks anything and never crashes`() {
        assertFalse(LockPolicy.lockOnReturn(AutoLockTimeout.SECONDS_15, true, backgroundedAt = 50_000, now = 10_000))
    }

    @Test
    fun `idle lock fires only when unlocked and idle long enough`() {
        val t = AutoLockTimeout.MINUTE_1
        assertTrue(LockPolicy.lockWhenIdle(t, isUnlocked = true, idleMillis = 60_000))
        assertFalse(LockPolicy.lockWhenIdle(t, isUnlocked = true, idleMillis = 59_999))
        assertFalse(LockPolicy.lockWhenIdle(t, isUnlocked = false, idleMillis = 600_000))
    }

    @Test
    fun `immediately locks on leaving the app, never while the user is in it`() {
        // Found on device: treating it as a 0 ms foreground idle timeout locked the vault every 5 s in use.
        assertFalse(LockPolicy.lockWhenIdle(AutoLockTimeout.IMMEDIATELY, isUnlocked = true, idleMillis = 0))
        assertFalse(LockPolicy.lockWhenIdle(AutoLockTimeout.IMMEDIATELY, isUnlocked = true, idleMillis = 600_000))
    }

    @Test
    fun `vault screens restored after process death go to the lock screen`() {
        // Android restores the back stack after killing the app; the session starts locked (ColdStart).
        listOf(Routes.PREVIEW_PAGE, Routes.DETAILS_PAGE + "?previewId=4", Routes.SETTINGS_PAGE).forEach { route ->
            assertTrue(route, LockPolicy.mustShowLockScreen(false, LockReason.ColdStart, route))
        }
    }

    @Test
    fun `first-run screens are never interrupted by a cold-start lock`() {
        listOf(
            Routes.SPLASH_PAGE,
            Routes.ONBOARDING_PAGE,
            Routes.CONSENT_PAGE,
            Routes.SECURITY_PAGE,
            null
        ).forEach { route ->
            assertFalse("$route", LockPolicy.mustShowLockScreen(false, LockReason.ColdStart, route))
        }
    }

    @Test
    fun `any real lock sends every screen except the lock screen itself back to unlock`() {
        assertTrue(LockPolicy.mustShowLockScreen(false, LockReason.Background, Routes.CONSENT_PAGE))
        assertTrue(LockPolicy.mustShowLockScreen(false, LockReason.Idle, Routes.PREVIEW_PAGE))
        assertFalse(LockPolicy.mustShowLockScreen(false, LockReason.Idle, Routes.SECURITY_PAGE))
        assertFalse(LockPolicy.mustShowLockScreen(true, LockReason.Idle, Routes.PREVIEW_PAGE))
    }
}
