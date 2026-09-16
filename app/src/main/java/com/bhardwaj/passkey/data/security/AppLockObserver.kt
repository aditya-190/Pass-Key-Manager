package com.bhardwaj.passkey.data.security

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.utils.SecureClipboard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-locks the vault when the app goes to the background for longer than the configured timeout.
 *
 * Registered against ProcessLifecycleOwner rather than an Activity, so a configuration change or
 * an internal activity transition does not count as leaving the app.
 */
@Singleton
class AppLockObserver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val session: VaultSession,
    private val dataStoreRepository: DataStoreRepository
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStop(owner: LifecycleOwner) {
        session.backgroundedAtElapsed = SystemClock.elapsedRealtime()
        session.unlockedWhenBackgrounded = session.isUnlocked.value
        scope.launch {
            if (session.isUnlocked.value && timeout() == AutoLockTimeout.IMMEDIATELY) lockNow(LockReason.Background)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            val lock = LockPolicy.lockOnReturn(
                timeout = timeout(),
                wasUnlocked = session.unlockedWhenBackgrounded && session.isUnlocked.value,
                backgroundedAt = session.backgroundedAtElapsed,
                now = SystemClock.elapsedRealtime()
            )
            if (lock) lockNow(LockReason.Background)
        }
    }

    /** Called from the foreground idle poll in MainActivity. */
    fun lockIfIdle() {
        scope.launch {
            if (LockPolicy.lockWhenIdle(timeout(), session.isUnlocked.value, session.idleMillis())) {
                lockNow(LockReason.Idle)
            }
        }
    }

    fun lockNow(reason: LockReason) {
        // Clear any copied secret first: once locked there is no UI left to offer a "clear" action.
        SecureClipboard.clearIfOurs(context)
        session.lock(reason)
    }

    suspend fun timeout(): AutoLockTimeout = AutoLockTimeout.fromMillis(
        dataStoreRepository.readPreference(
            key = DataStoreRepository.autoLockTimeoutKey,
            defaultValue = AutoLockTimeout.DEFAULT.millis
        ).first()
    )
}
