package com.bhardwaj.passkey

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PasskeyApplication : Application()


// TODOs:

// 1. Drag and Drop to reorder list.
// 2. Import / Export data to and from storage with a good permission handling class.
// 3. Handle data migration for existing users to prevent data loss.