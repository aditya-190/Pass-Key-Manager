package com.bhardwaj.passkey.update

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin wrapper over the Play In-App Update API (`com.google.android.play:app-update-ktx`). All decisions live
 * in [InAppUpdatePolicy]; this class only talks to Play. A null [manager] (or any Play error) reports "Play
 * unavailable", so the policy falls back to the gate screen / soft sheet. Create one per Activity and call
 * [release] in `onDestroy` (or use [rememberInAppUpdater], which the Activity's composition owns).
 */
class InAppUpdateController(private val manager: AppUpdateManager?) {
    private var lastInfo: AppUpdateInfo? = null
    private var listening = false
    private val _flexibleDownloaded = MutableStateFlow(false)

    /** True once a FLEXIBLE download is ready; show [FlexibleUpdateReadyBar] and call [completeFlexibleUpdate]. */
    val flexibleDownloaded: StateFlow<Boolean> = _flexibleDownloaded.asStateFlow()

    private val listener =
        InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) _flexibleDownloaded.value = true
        }

    /** null = Play can't help (no Play Store, not installed from Play, API error). */
    suspend fun playInfo(): PlayUpdateInfo? {
        val appUpdateManager = manager ?: return null
        val info =
            try {
                appUpdateManager.requestAppUpdateInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception
            ) {
                return null
            }
        lastInfo = info
        val downloaded = info.installStatus() == InstallStatus.DOWNLOADED
        if (downloaded) _flexibleDownloaded.value = true
        return PlayUpdateInfo(
            updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            immediateInProgress =
            info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            flexibleDownloaded = downloaded
        )
    }

    /** Starts Play's flow for a START_* action. False = couldn't start; the caller shows the fallback. */
    fun start(action: InAppUpdateAction, launcher: ActivityResultLauncher<IntentSenderRequest>): Boolean {
        val appUpdateManager = manager ?: return false
        // An AppUpdateInfo can start only one flow, so it's consumed here; call playInfo() again to retry.
        val info = lastInfo ?: return false
        val type =
            when (action) {
                InAppUpdateAction.START_IMMEDIATE -> AppUpdateType.IMMEDIATE
                InAppUpdateAction.START_FLEXIBLE -> AppUpdateType.FLEXIBLE
                else -> return false
            }
        lastInfo = null
        if (type == AppUpdateType.FLEXIBLE && !listening) {
            appUpdateManager.registerListener(listener)
            listening = true
        }
        return runCatching {
            appUpdateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.defaultOptions(type))
        }.getOrDefault(false)
    }

    /** Installs a downloaded FLEXIBLE update. Play restarts the app. */
    fun completeFlexibleUpdate() {
        manager?.completeUpdate()
    }

    fun release() {
        if (listening) manager?.unregisterListener(listener)
        listening = false
    }

    companion object {
        fun create(context: Context): InAppUpdateController =
            InAppUpdateController(runCatching { AppUpdateManagerFactory.create(context) }.getOrNull())
    }
}

/** Binds an [InAppUpdateController] to an activity-result launcher. Obtain with [rememberInAppUpdater]. */
class InAppUpdater internal constructor(
    val controller: InAppUpdateController,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>
) {
    /**
     * Asks Play, applies [InAppUpdatePolicy] and starts Play's UI when chosen. Returns the effective action:
     * START_* = Play's UI is up; SHOW_GATE / SHOW_SOFT_PROMPT = show our own screen; COMPLETE_FLEXIBLE = offer
     * a restart. Hard gates stay on screen under Play's IMMEDIATE UI, so cancelling it never unblocks the app.
     */
    suspend fun run(decision: UpdateDecision, inAppEnabled: Boolean): InAppUpdateAction {
        val action = InAppUpdatePolicy.choose(decision, controller.playInfo(), inAppEnabled)
        return when (action) {
            InAppUpdateAction.START_IMMEDIATE ->
                if (controller.start(action, launcher)) action else InAppUpdateAction.SHOW_GATE
            InAppUpdateAction.START_FLEXIBLE ->
                if (controller.start(action, launcher)) action else InAppUpdateAction.SHOW_SOFT_PROMPT
            else -> action
        }
    }
}

/** [onResult] receives the activity result code (see [inAppUpdateResultName]). */
@Composable
fun rememberInAppUpdater(controller: InAppUpdateController, onResult: (resultCode: Int) -> Unit): InAppUpdater {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            currentOnResult(result.resultCode)
        }
    return remember(controller, launcher) { InAppUpdater(controller, launcher) }
}
