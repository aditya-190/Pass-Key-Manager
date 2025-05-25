package com.bhardwaj.passkey

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PasskeyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }
}