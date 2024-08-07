package com.bhardwaj.passkey

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PasskeyApplication : Application()


// TODOs:

// 1. Handle data migration for existing users to prevent data loss.