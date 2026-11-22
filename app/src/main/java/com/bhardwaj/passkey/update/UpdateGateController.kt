package com.bhardwaj.passkey.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.core.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the update gate, owned by the root ViewModel (pass its `viewModelScope`).
 * - [gate]: Hard / Maintenance replace the whole UI, at cold start or whenever config changes.
 * - [softPromptDue]: becomes true on a main screen when a soft update is due (throttled per version via
 *   DataStore). The UI then runs [InAppUpdater.run]; FLEXIBLE or the soft sheet both end in [onSoftPromptHandled].
 */
class UpdateGateController(
    private val config: StateFlow<AppConfig>,
    private val versionCode: Long,
    private val dataStore: DataStore<Preferences>,
    private val analytics: AnalyticsTracker,
    private val clock: Clock,
    private val scope: CoroutineScope
) {
    val gate: StateFlow<UpdateDecision> =
        config
            .map { ForceUpdateManager.decide(versionCode, it) }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, ForceUpdateManager.decide(versionCode, config.value))

    private val _softPromptDue = MutableStateFlow(false)
    val softPromptDue: StateFlow<Boolean> = _softPromptDue.asStateFlow()
    private var softCheckedForVersion: Long? = null

    init {
        scope.launch {
            gate.collect { decision ->
                when (decision) {
                    UpdateDecision.Hard -> analytics.log(BaselineEvents.forceUpdateShown(versionCode))
                    UpdateDecision.Maintenance -> analytics.log(BaselineEvents.maintenanceShown())
                    else -> Unit
                }
            }
        }
    }

    /** Call when a main (non-onboarding, non-gameplay) screen is showing. Checks once per latest version. */
    fun onMainScreenVisible() {
        val decision = gate.value as? UpdateDecision.Soft ?: return
        if (softCheckedForVersion == decision.latestVersionCode) return
        softCheckedForVersion = decision.latestVersionCode
        scope.launch {
            val prefs = dataStore.data.first()
            val show =
                ForceUpdateManager.shouldShowSoftPrompt(
                    latestVersionCode = decision.latestVersionCode,
                    lastDismissedVersionCode = prefs[SOFT_DISMISSED_VERSION],
                    lastDismissedAtMs = prefs[SOFT_DISMISSED_AT],
                    nowMs = clock.nowMs(),
                    intervalDays = config.value[ConfigKeys.SOFT_UPDATE_INTERVAL_DAYS]
                )
            if (show) {
                analytics.log(BaselineEvents.softUpdateShown(decision.latestVersionCode))
                _softPromptDue.value = true
            }
        }
    }

    /** The soft offer was shown (Play FLEXIBLE dialog or our sheet) and closed; start the throttle window. */
    fun onSoftPromptHandled(dismissed: Boolean) {
        val latest = config.value.latestVersionCode
        _softPromptDue.value = false
        if (dismissed) analytics.log(BaselineEvents.softUpdateDismissed(latest))
        scope.launch {
            dataStore.edit {
                it[SOFT_DISMISSED_VERSION] = latest
                it[SOFT_DISMISSED_AT] = clock.nowMs()
            }
        }
    }

    fun onUpdateClicked() {
        analytics.log(BaselineEvents.forceUpdateClicked())
    }

    private companion object {
        val SOFT_DISMISSED_VERSION = longPreferencesKey("soft_update_dismissed_version")
        val SOFT_DISMISSED_AT = longPreferencesKey("soft_update_dismissed_at_ms")
    }
}
