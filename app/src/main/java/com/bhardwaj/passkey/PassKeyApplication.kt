package com.bhardwaj.passkey

import android.app.Application
import com.bhardwaj.passkey.analytics.ConsentApplier
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PasskeyApplication : Application() {
    @Inject
    lateinit var consentApplier: ConsentApplier

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        // Every collection flag is off in the manifest; this switches on only what the user agreed to.
        consentApplier.start()
    }
}
