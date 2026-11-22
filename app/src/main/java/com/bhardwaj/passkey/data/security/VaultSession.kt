package com.bhardwaj.passkey.data.security

import android.os.SystemClock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LockReason { ColdStart, Manual, Background, Idle }

/**
 * Whether the vault is currently unlocked, and how long since the user last touched it.
 *
 * Before this existed there was no re-lock at all: once past the biometric screen, backgrounding
 * the app and returning granted full access indefinitely, and SecurityViewModel's isAuthenticated
 * flag was written but never read, so navigation was the only gate.
 *
 * In 5.6.0 this tracks lock state only. In 5.7.0 it also owns the decrypted database key, and
 * locking will zero that key rather than merely flipping a boolean.
 */
@Singleton
class VaultSession(private val now: () -> Long) {
    @Inject
    constructor() : this(SystemClock::elapsedRealtime)

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _lockReason = MutableStateFlow(LockReason.ColdStart)
    val lockReason: StateFlow<LockReason> = _lockReason.asStateFlow()

    /**
     * elapsedRealtime, never currentTimeMillis: wall-clock time lets a user extend their own
     * grace period by changing the device clock, and jumps on NTP sync.
     */
    @Volatile
    var lastInteractionElapsed: Long = now()
        private set

    @Volatile
    var backgroundedAtElapsed: Long = 0L

    /** Whether the vault was unlocked when the app last went to the background. */
    @Volatile
    var unlockedWhenBackgrounded: Boolean = false

    fun onUnlocked() {
        lockTrace("session unlocked")
        _isUnlocked.value = true
        touch()
    }

    fun lock(reason: LockReason) {
        lockTrace("session lock reason=$reason wasUnlocked=${_isUnlocked.value}")
        _lockReason.value = reason
        _isUnlocked.value = false
    }

    fun touch() {
        lastInteractionElapsed = now()
    }

    fun idleMillis(): Long = now() - lastInteractionElapsed
}
