package com.bhardwaj.passkey.analytics

import com.bhardwaj.passkey.crash.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * The user's stored choice. [analytics] covers usage statistics; [crash] covers diagnostics, meaning
 * crash reports and performance traces. Until [decided], nothing is collected.
 */
data class ConsentState(val decided: Boolean, val analytics: Boolean, val crash: Boolean) {
    companion object {
        val Undecided = ConsentState(decided = false, analytics = false, crash = false)
    }
}

/** Google consent mode v2. Ad signals are always denied: PassKey has no ads. */
fun interface ConsentModeSink {
    fun apply(analyticsGranted: Boolean)
}

/** Performance Monitoring, which follows the diagnostics choice. */
fun interface DiagnosticsSink {
    fun setEnabled(enabled: Boolean)
}

/**
 * Applies the stored consent to Analytics, Crashlytics, Performance and consent mode whenever it
 * changes. The manifest keeps every collection flag OFF, so nothing is sent before this first runs.
 */
class ConsentApplier(
    private val consent: Flow<ConsentState>,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val consentMode: ConsentModeSink,
    private val performance: DiagnosticsSink,
    private val scope: CoroutineScope
) {
    /** Call once from Application.onCreate. */
    fun start(): Job = scope.launch { consent.distinctUntilChanged().collect(::apply) }

    fun apply(state: ConsentState) {
        analytics.setCollectionEnabled(state.analytics)
        crashReporter.setCollectionEnabled(state.crash)
        // Crashes captured before the choice (onboarding, the consent screen itself) wait on the device.
        // An explicit "no" deletes them; a "yes" lets Crashlytics send them; undecided keeps waiting.
        if (state.decided && !state.crash) crashReporter.discardUnsentReports()
        performance.setEnabled(state.crash)
        consentMode.apply(analyticsGranted = state.analytics)
        analytics.setUserProperty(AppUserProps.CONSENT_ANALYTICS, state.analytics.toString())
    }
}
