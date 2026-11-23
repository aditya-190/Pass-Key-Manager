package com.bhardwaj.passkey.data.security

import android.util.Log
import com.bhardwaj.passkey.BuildConfig

/** Debug-build-only breadcrumbs for lock state. Never pass vault content. */
internal fun lockTrace(message: String) {
    // runCatching: android.util.Log is a stub in JVM unit tests.
    if (BuildConfig.DEBUG) runCatching { Log.d("PassKeyLock", message) }
}
