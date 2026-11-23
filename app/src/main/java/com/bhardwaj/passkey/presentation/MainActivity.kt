package com.bhardwaj.passkey.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.config.RemoteConfigManager
import com.bhardwaj.passkey.data.security.AppLockObserver
import com.bhardwaj.passkey.data.security.LockPolicy
import com.bhardwaj.passkey.data.security.VaultSession
import com.bhardwaj.passkey.data.security.lockTrace
import com.bhardwaj.passkey.domain.model.PendingShortcut
import com.bhardwaj.passkey.domain.model.ShortcutAction
import com.bhardwaj.passkey.domain.model.ThemeMode
import com.bhardwaj.passkey.domain.repository.PreferencesRepository
import com.bhardwaj.passkey.presentation.navigation.NavGraph
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.theme.PassKeyTheme
import com.bhardwaj.passkey.update.InAppUpdateController
import com.bhardwaj.passkey.update.UpdateGateController
import com.bhardwaj.passkey.update.UpdateHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var analytics: AnalyticsTracker

    @Inject
    lateinit var preferences: PreferencesRepository

    @Inject
    lateinit var pendingShortcut: PendingShortcut

    @Inject
    lateinit var vaultSession: VaultSession

    @Inject
    lateinit var appLockObserver: AppLockObserver

    /**
     * Dispatched before the window handles the event, so it sees every touch and key press without
     * competing with Compose gesture consumption. Feeds the foreground idle timer.
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        vaultSession.touch()
    }

    @Inject
    lateinit var updateGate: UpdateGateController

    @Inject
    lateinit var remoteConfig: RemoteConfigManager

    private lateinit var inAppUpdates: InAppUpdateController

    override fun onDestroy() {
        inAppUpdates.release()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        offerShortcut(intent)
    }

    /** Icon shortcuts only ask Home to act; Home runs them after the vault is unlocked. */
    private fun offerShortcut(intent: Intent?) {
        ShortcutAction.fromIntentAction(intent?.action)?.let(pendingShortcut::offer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) offerShortcut(intent)
        // Read once before the first frame so a chosen Light or Dark never flashes the other scheme; a tiny
        // preferences file, bounded in case storage is slow.
        val initialTheme = runBlocking { withTimeoutOrNull(THEME_READ_TIMEOUT_MS) { preferences.themeMode.first() } }
            ?: ThemeMode.SYSTEM
        inAppUpdates = InAppUpdateController.create(this)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        // FLAG_SECURE already blanks the recents thumbnail, but only as a side effect of blocking capture; this says
        // so explicitly, so the two survive independently.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setRecentsScreenshotEnabled(false)
        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialTheme)
            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status and navigation bar icons follow the app's scheme, not the phone's, or they vanish.
            DisposableEffect(dark) {
                val bars = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                onDispose {}
            }
            PassKeyTheme(darkTheme = dark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val destination = navController.currentBackStackEntryAsState().value?.destination
                    val isUnlocked by vaultSession.isUnlocked.collectAsStateWithLifecycle()
                    VaultLockGate(navController)
                    UpdateHost(
                        gateController = updateGate,
                        config = remoteConfig.config,
                        updateController = inAppUpdates,
                        analytics = analytics,
                        // Soft prompts only on the vault home, never during onboarding, unlock or editing.
                        isMainScreen = isUnlocked && destination?.hasRoute<NavRoute.Previews>() == true,
                        packageName = packageName
                    ) {
                        NavGraph(
                            navController = navController,
                            analytics = analytics
                        )
                    }
                }
            }
        }
    }

    /**
     * Sends the user to the unlock screen whenever [LockPolicy.mustShowLockScreen] says so: after a
     * background or idle lock, and after process death when Android restores vault screens into a session
     * that starts locked. Navigating with popUpTo(0) destroys every screen that held vault content.
     */
    @Composable
    private fun VaultLockGate(navController: NavHostController) {
        val isUnlocked by vaultSession.isUnlocked.collectAsStateWithLifecycle()
        val lockReason by vaultSession.lockReason.collectAsStateWithLifecycle()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route

        // Foreground idle timer: onStop covers backgrounding, this covers an unlocked phone left untouched.
        LaunchedEffect(isUnlocked) {
            while (isUnlocked) {
                delay(IDLE_POLL_INTERVAL_MS)
                appLockObserver.lockIfIdle()
            }
        }
        LaunchedEffect(isUnlocked, lockReason, route) {
            if (LockPolicy.mustShowLockScreen(isUnlocked, lockReason, route)) {
                lockTrace("gate -> lock screen from $route reason=$lockReason")
                navController.navigate(NavRoute.Security) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    private companion object {
        const val IDLE_POLL_INTERVAL_MS = 5_000L
        const val THEME_READ_TIMEOUT_MS = 300L
    }
}
