package com.bhardwaj.passkey.core

import android.content.Context
import com.google.firebase.FirebaseApp

/**
 * True when a [FirebaseApp] exists (google-services.json was present at build time). Every Firebase
 * wrapper asks this first and falls back to its NoOp twin, so a build without the config file still runs.
 */
object FirebaseAvailability {
    fun isConfigured(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()
}
