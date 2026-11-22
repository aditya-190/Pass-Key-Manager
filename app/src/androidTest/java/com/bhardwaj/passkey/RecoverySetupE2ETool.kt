package com.bhardwaj.passkey

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.presentation.MainActivity
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in driver for the 5.7.0 upgrade check (`-e recoveryPassword <pw>`): on the vault gate it sets the
 * recovery password through the real dialog, then waits while the host types the device PIN on the system
 * credential screen, until the vault home is shown. adb key events cannot reach Compose dialog windows, so
 * the dialog is driven from here. Never run on a phone with real data.
 */
@RunWith(AndroidJUnit4::class)
class RecoverySetupE2ETool {
    // Only opt-in runs launch the app; a plain test run must not even start the vault gate.
    @get:Rule
    val compose = gateComposeRule(MainActivity::class.java) {
        val args = InstrumentationRegistry.getArguments()
        args.getString("recoveryPassword") != null || args.getString("recoveryUnlock") != null
    }

    private fun text(id: Int) = compose.activity.getString(id)

    private fun visible(label: String) =
        runCatching { compose.onAllNodes(hasText(label)).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)

    @Test
    fun setRecoveryPasswordAndOpenVault() {
        val password = InstrumentationRegistry.getArguments().getString("recoveryPassword")
        assumeTrue(!password.isNullOrBlank())

        compose.waitUntil(20_000) { visible(text(R.string.recovery_setup_title)) }
        compose.onNode(hasText(text(R.string.backup_confirm))).performClick()
        compose.waitUntil(10_000) {
            runCatching { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size >= 2 }.getOrDefault(false)
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(password!!)
        compose.onAllNodes(hasSetTextAction())[1].performTextInput(password)
        compose.onAllNodes(hasText(text(R.string.backup_confirm)))[0].performClick()

        // The host enters the device PIN; then the re-key runs and the vault home appears.
        compose.waitUntil(120_000) { visible(text(R.string.cd_add_entry)) }
    }

    /** `-e recoveryUnlock <pw>`: answers the "enter your recovery password" gate and waits for the vault. */
    @Test
    fun unlockWithRecoveryPassword() {
        val password = InstrumentationRegistry.getArguments().getString("recoveryUnlock")
        assumeTrue(!password.isNullOrBlank())

        compose.waitUntil(20_000) { visible(text(R.string.recovery_unlock_title)) }
        compose.waitUntil(10_000) {
            runCatching {
                compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput(password!!)
        compose.onAllNodes(hasText(text(R.string.backup_confirm)))[0].performClick()
        compose.waitUntil(60_000) { visible(text(R.string.cd_add_entry)) }
    }
}
