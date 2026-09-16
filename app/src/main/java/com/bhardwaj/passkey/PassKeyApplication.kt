package com.bhardwaj.passkey

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.bhardwaj.passkey.analytics.ConsentApplier
import com.bhardwaj.passkey.data.security.AppLockObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PasskeyApplication : Application() {
    @Inject
    lateinit var consentApplier: ConsentApplier

    @Inject
    lateinit var appLockObserver: AppLockObserver

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        // Every collection flag is off in the manifest; this switches on only what the user agreed to.
        consentApplier.start()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLockObserver)
    }
}
