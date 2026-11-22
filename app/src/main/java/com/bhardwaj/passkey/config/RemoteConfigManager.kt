package com.bhardwaj.passkey.config

import android.content.Context
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import com.bhardwaj.passkey.core.Clock
import com.bhardwaj.passkey.core.FirebaseAvailability
import com.bhardwaj.passkey.crash.CrashKeys
import com.bhardwaj.passkey.crash.CrashReporter
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Firebase Remote Config behind [ConfigProvider]. With [remoteConfig] == null (Firebase not configured) every
 * value is the in-app default from [ConfigKeys] and fetches report `false`, so callers never branch on Firebase.
 *
 * Wiring: call [initialize] once from `Application.onCreate`, [fetchAndActivate] from the splash/launch step
 * (bounded by a timeout) and [refreshIfStale] from `ProcessLifecycleOwner.onStart`.
 */
class RemoteConfigManager(
    private val remoteConfig: FirebaseRemoteConfig?,
    private val analytics: AnalyticsTracker,
    private val crashReporter: CrashReporter,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val isDebug: Boolean,
    /** Optional feature key groups (ads apps: `AdConfigKeys.all`); their defaults are registered too. */
    private val featureKeys: List<ConfigKey<*>> = emptyList()
) : ConfigProvider {
    private val reader: ConfigReader =
        remoteConfig?.let { rc ->
            object : ConfigReader {
                @Suppress("UNCHECKED_CAST")
                override fun <T : Any> get(key: ConfigKey<T>): T = when (key) {
                    is ConfigKey.LongKey -> rc.getLong(key.name)
                    is ConfigKey.BoolKey -> rc.getBoolean(key.name)
                    is ConfigKey.StringKey -> rc.getString(key.name)
                } as T
            }
        } ?: DefaultConfigReader

    private val _config = MutableStateFlow(AppConfig(reader))
    val config: StateFlow<AppConfig> = _config.asStateFlow()

    override val current: AppConfig get() = _config.value

    @Volatile private var lastFetchAtMs = 0L

    /** Called once from Application.onCreate. Defaults apply synchronously from memory. */
    fun initialize() {
        val rc = remoteConfig ?: return
        rc.setDefaultsAsync(ConfigKeys.defaultsMap(featureKeys))
        rc.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings
                .Builder()
                .setMinimumFetchIntervalInSeconds(if (isDebug) 0 else PROD_MIN_FETCH_INTERVAL_SEC)
                .build()
        )
        rc.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    scope.launch {
                        runCatching { rc.activate().await() }
                            .onSuccess { publish() }
                            .onFailure { crashReporter.recordException(it) }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    crashReporter.log("remote_config realtime error: ${error.code}")
                }
            }
        )
    }

    /** App resume: refetch at most every [maxAgeMs] so the update gate reacts without a cold start. */
    suspend fun refreshIfStale(maxAgeMs: Long = RESUME_REFRESH_MS) {
        // lastFetchAtMs == 0: cold start, the launch step does the first fetch.
        if (lastFetchAtMs != 0L && clock.nowMs() - lastFetchAtMs >= maxAgeMs) fetchAndActivate()
    }

    /** Fetch and activate within [timeoutMs]; on timeout or failure the last active/default values stay. */
    suspend fun fetchAndActivate(timeoutMs: Long = DEFAULT_TIMEOUT_MS): Boolean {
        val rc = remoteConfig ?: return false
        val start = clock.nowMs()
        lastFetchAtMs = start
        // A fetch that outlives the timeout keeps running; its values are published when it lands.
        val task = rc.fetchAndActivate().addOnSuccessListener { publish() }
        val success =
            try {
                withTimeoutOrNull(timeoutMs) { task.await() } != null
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                crashReporter.recordException(e)
                false
            }
        val durationMs = clock.nowMs() - start
        analytics.log(BaselineEvents.remoteConfigFetch(success, durationMs))
        crashReporter.log("remote_config fetch success=$success duration_ms=$durationMs")
        publish()
        return success
    }

    private fun publish() {
        _config.value = AppConfig(reader)
        crashReporter.setCustomKey(CrashKeys.REMOTE_CONFIG_VERSION, _config.value.configVersion)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 3_000L
        const val PROD_MIN_FETCH_INTERVAL_SEC = 3_600L
        const val RESUME_REFRESH_MS = 30L * 60 * 1000

        /** Binds the real Firebase instance only when Firebase is configured; defaults-only otherwise. */
        fun create(
            context: Context,
            analytics: AnalyticsTracker,
            crashReporter: CrashReporter,
            clock: Clock,
            scope: CoroutineScope,
            isDebug: Boolean,
            featureKeys: List<ConfigKey<*>> = emptyList()
        ): RemoteConfigManager = RemoteConfigManager(
            remoteConfig = if (FirebaseAvailability.isConfigured(context)) FirebaseRemoteConfig.getInstance() else null,
            analytics = analytics,
            crashReporter = crashReporter,
            clock = clock,
            scope = scope,
            isDebug = isDebug,
            featureKeys = featureKeys
        )
    }
}
