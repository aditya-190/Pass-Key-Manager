package com.bhardwaj.passkey.update

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKeys
import kotlinx.coroutines.flow.StateFlow

/**
 * Hosts the force-update and maintenance gates over [content].
 *
 * - Hard update: the blocking screen stays under Play's IMMEDIATE flow, so cancelling Play never opens the vault.
 * - Maintenance: a blocking screen.
 * - Soft update: offered only while [isMainScreen] (never mid-flow), at most once per interval, through Play's
 *   FLEXIBLE flow or our dismissible sheet; a downloaded update offers a restart bar.
 */
@Composable
fun UpdateHost(
    gateController: UpdateGateController,
    config: StateFlow<AppConfig>,
    updateController: InAppUpdateController,
    analytics: AnalyticsTracker,
    isMainScreen: Boolean,
    packageName: String,
    content: @Composable () -> Unit
) {
    val decision by gateController.gate.collectAsStateWithLifecycle()
    val softDue by gateController.softPromptDue.collectAsStateWithLifecycle()
    val appConfig by config.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var action by remember { mutableStateOf(InAppUpdateAction.NONE) }

    val updater = rememberInAppUpdater(updateController) { resultCode ->
        val flow = if (decision == UpdateDecision.Hard) "immediate" else "flexible"
        analytics.log(BaselineEvents.inAppUpdateResult(flow, inAppUpdateResultName(resultCode)))
        if (decision is UpdateDecision.Soft) gateController.onSoftPromptHandled(dismissed = resultCode != -1)
    }

    LaunchedEffect(isMainScreen, decision) {
        if (isMainScreen) gateController.onMainScreenVisible()
    }
    LaunchedEffect(decision, softDue) {
        val wanted = decision == UpdateDecision.Hard ||
            (decision is UpdateDecision.Soft && softDue) ||
            decision == UpdateDecision.None
        if (wanted) action = updater.run(decision, appConfig.inAppUpdateEnabled)
    }

    val storeUrl = appConfig.storeUrl(packageName)
    Box(Modifier.fillMaxSize()) {
        content()
        when {
            decision == UpdateDecision.Maintenance -> MaintenanceScreen(
                message = appConfig[ConfigKeys.MAINTENANCE_MESSAGE].ifBlank {
                    stringResource(R.string.maintenance_body)
                }
            )

            decision == UpdateDecision.Hard -> HardUpdateScreen(
                title = appConfig[ConfigKeys.UPDATE_TITLE].ifBlank { stringResource(R.string.update_hard_title) },
                message = appConfig[ConfigKeys.UPDATE_MESSAGE].ifBlank { stringResource(R.string.update_hard_body) },
                onUpdate = {
                    gateController.onUpdateClicked()
                    openStoreListing(context, storeUrl)
                }
            )

            action == InAppUpdateAction.SHOW_SOFT_PROMPT && softDue -> SoftUpdateSheet(
                title = stringResource(R.string.update_soft_title),
                message = stringResource(R.string.update_soft_body),
                onUpdate = {
                    gateController.onSoftPromptHandled(dismissed = false)
                    openStoreListing(context, storeUrl)
                },
                onDismiss = { gateController.onSoftPromptHandled(dismissed = true) }
            )

            action == InAppUpdateAction.COMPLETE_FLEXIBLE -> FlexibleUpdateReadyBar(
                onRestart = { updateController.completeFlexibleUpdate() },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
