package com.bhardwaj.passkey.crash

import android.content.Context
import com.bhardwaj.passkey.core.FirebaseAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CancellationException

/**
 * Crashlytics wrapper. Never pass vault content, user-typed text or secrets to any of these methods:
 * exception messages included, so wrap a failure in a new exception rather than forwarding one whose
 * message might quote a field value.
 */
interface CrashReporter {
    fun setCustomKey(key: String, value: String)

    fun setCustomKey(key: String, value: Long)

    fun setCustomKey(key: String, value: Boolean)

    /** Breadcrumb attached to the next crash report. */
    fun log(message: String)

    fun recordException(throwable: Throwable)

    fun setCollectionEnabled(enabled: Boolean)
}

object CrashKeys {
    const val SCREEN = "screen"
}

class FirebaseCrashReporter(private val crashlytics: FirebaseCrashlytics) : CrashReporter {
    override fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    override fun setCustomKey(key: String, value: Long) = crashlytics.setCustomKey(key, value)

    override fun setCustomKey(key: String, value: Boolean) = crashlytics.setCustomKey(key, value)

    override fun log(message: String) = crashlytics.log(message)

    override fun recordException(throwable: Throwable) = crashlytics.recordException(throwable)

    override fun setCollectionEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
        // Reports captured while collection was off are kept on the device. If the user said no,
        // they must never be sent later, so drop them.
        if (!enabled) crashlytics.deleteUnsentReports()
    }

    companion object {
        /** Crashlytics when Firebase is configured, otherwise [NoOpCrashReporter]. */
        fun create(context: Context): CrashReporter = if (FirebaseAvailability.isConfigured(context)) {
            FirebaseCrashReporter(FirebaseCrashlytics.getInstance())
        } else {
            NoOpCrashReporter
        }
    }
}

object NoOpCrashReporter : CrashReporter {
    override fun setCustomKey(key: String, value: String) = Unit

    override fun setCustomKey(key: String, value: Long) = Unit

    override fun setCustomKey(key: String, value: Boolean) = Unit

    override fun log(message: String) = Unit

    override fun recordException(throwable: Throwable) = Unit

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}

/**
 * Error boundary: runs [block], records any failure as a non-fatal tagged with [area] and returns it as a
 * [Result]. Cancellation is rethrown so structured concurrency keeps working.
 */
suspend fun <T> CrashReporter.recordingFailures(area: String, block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (
    @Suppress("TooGenericExceptionCaught") e: Exception
) {
    log("non-fatal at $area")
    recordException(e)
    Result.failure(e)
}
