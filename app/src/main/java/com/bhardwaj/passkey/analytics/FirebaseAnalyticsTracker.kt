package com.bhardwaj.passkey.analytics

import android.content.Context
import android.os.Bundle
import com.bhardwaj.passkey.core.FirebaseAvailability
import com.bhardwaj.passkey.crash.CrashReporter
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.ConsentStatus
import com.google.firebase.analytics.FirebaseAnalytics.ConsentType
import com.google.firebase.perf.FirebasePerformance

class FirebaseAnalyticsTracker(private val analytics: FirebaseAnalytics, private val crashReporter: CrashReporter) :
    AnalyticsTracker {
    override fun log(event: AnalyticsEvent) {
        val problems = EventValidator.validate(event)
        if (problems.isNotEmpty()) {
            // Dropped rather than sent malformed; surfaced as a non-fatal so it gets fixed.
            crashReporter.recordException(IllegalArgumentException("Invalid analytics event: $problems"))
            return
        }
        analytics.logEvent(event.name, event.params.toBundle())
    }

    override fun setUserProperty(name: String, value: String?) =
        analytics.setUserProperty(name, value?.take(EventValidator.MAX_VALUE_LENGTH))

    override fun setCollectionEnabled(enabled: Boolean) = analytics.setAnalyticsCollectionEnabled(enabled)

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().also { bundle ->
        forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                // Firebase has no boolean param type; 1/0 is aggregatable in reports.
                is Boolean -> bundle.putLong(key, if (value) 1L else 0L)
            }
        }
    }

    companion object {
        /** Firebase when configured, otherwise [NoOpAnalyticsTracker]. Never crashes without google-services.json. */
        fun create(context: Context, crashReporter: CrashReporter): AnalyticsTracker =
            if (FirebaseAvailability.isConfigured(context)) {
                FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(context), crashReporter)
            } else {
                NoOpAnalyticsTracker
            }
    }
}

class FirebaseConsentMode(private val firebaseAnalytics: FirebaseAnalytics) : ConsentModeSink {
    override fun apply(analyticsGranted: Boolean) {
        val analyticsStatus = if (analyticsGranted) ConsentStatus.GRANTED else ConsentStatus.DENIED
        firebaseAnalytics.setConsent(
            mapOf(
                ConsentType.ANALYTICS_STORAGE to analyticsStatus,
                ConsentType.AD_STORAGE to ConsentStatus.DENIED,
                ConsentType.AD_USER_DATA to ConsentStatus.DENIED,
                ConsentType.AD_PERSONALIZATION to ConsentStatus.DENIED
            )
        )
    }

    companion object {
        fun create(context: Context): ConsentModeSink = if (FirebaseAvailability.isConfigured(context)) {
            FirebaseConsentMode(FirebaseAnalytics.getInstance(context))
        } else {
            ConsentModeSink { }
        }
    }
}

object FirebasePerformanceSink {
    fun create(context: Context): DiagnosticsSink = if (FirebaseAvailability.isConfigured(context)) {
        DiagnosticsSink { enabled -> FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled }
    } else {
        DiagnosticsSink { }
    }
}
