package com.bhardwaj.passkey

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bhardwaj.passkey.analytics.ConsentApplier
import com.bhardwaj.passkey.config.RemoteConfigManager
import com.bhardwaj.passkey.data.security.AppLockObserver
import com.bhardwaj.passkey.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PasskeyApplication : Application() {
    @Inject
    lateinit var consentApplier: ConsentApplier

    @Inject
    lateinit var appLockObserver: AppLockObserver

    @Inject
    lateinit var remoteConfig: RemoteConfigManager

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        // Every collection flag is off in the manifest; this switches on only what the user agreed to.
        consentApplier.start()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockObserver)
        // Remote Config: defaults immediately, a bounded fetch at launch, a refresh when stale on return.
        remoteConfig.initialize()
        appScope.launch { remoteConfig.fetchAndActivate() }
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    appScope.launch { remoteConfig.refreshIfStale() }
                }
            }
        )
    }
}
