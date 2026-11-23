package com.bhardwaj.passkey

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.core.app.ActivityScenario
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Launches [activityClass] for a Compose test and closes it best-effort afterwards. The vault gate starts the system
 * PIN or biometric prompt on its own, which keeps the activity PAUSED, so ActivityScenarioRule's close() fails
 * waiting for DESTROYED. [launchIf] lets opt-in tools skip launching (and prompting) entirely.
 */
class GateActivityRule<A : ComponentActivity>(
    private val activityClass: Class<A>,
    private val launchIf: () -> Boolean = { true }
) : TestRule {
    private var scenario: ActivityScenario<A>? = null

    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            if (launchIf()) scenario = ActivityScenario.launch(activityClass)
            try {
                base.evaluate()
            } finally {
                // Closes when the vault ended unlocked; behind a system auth prompt close() cannot finish, and
                // that must not fail an otherwise passing test.
                scenario?.let { runCatching { it.close() } }
            }
        }
    }

    fun activity(): A {
        var activity: A? = null
        checkNotNull(scenario) { "activity was not launched" }.onActivity { activity = it }
        return checkNotNull(activity)
    }
}

fun <A : ComponentActivity> gateComposeRule(activityClass: Class<A>, launchIf: () -> Boolean = { true }) =
    AndroidComposeTestRule(GateActivityRule(activityClass, launchIf)) { it.activity() }
