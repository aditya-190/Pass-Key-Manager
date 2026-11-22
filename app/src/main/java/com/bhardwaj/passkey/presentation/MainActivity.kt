package com.bhardwaj.passkey.presentation

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.config.RemoteConfigManager
import com.bhardwaj.passkey.data.security.AppLockObserver
import com.bhardwaj.passkey.data.security.LockPolicy
import com.bhardwaj.passkey.data.security.VaultSession
import com.bhardwaj.passkey.data.security.lockTrace
import com.bhardwaj.passkey.presentation.navigation.NavGraph
import com.bhardwaj.passkey.presentation.navigation.NavScreens
import com.bhardwaj.passkey.presentation.navigation.Routes
import com.bhardwaj.passkey.presentation.theme.PassKeyTheme
import com.bhardwaj.passkey.update.InAppUpdateController
import com.bhardwaj.passkey.update.UpdateGateController
import com.bhardwaj.passkey.update.UpdateHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var analytics: AnalyticsTracker

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inAppUpdates = InAppUpdateController.create(this)
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        setContent {
            PassKeyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val route = navController.currentBackStackEntryAsState().value?.destination?.route
                    val isUnlocked by vaultSession.isUnlocked.collectAsStateWithLifecycle()
                    VaultLockGate(navController)
                    UpdateHost(
                        gateController = updateGate,
                        config = remoteConfig.config,
                        updateController = inAppUpdates,
                        analytics = analytics,
                        // Soft prompts only on the vault home, never during onboarding, unlock or editing.
                        isMainScreen = isUnlocked && route?.startsWith(Routes.PREVIEW_PAGE) == true,
                        packageName = packageName
                    ) {
                        NavGraph(
                            navController = navController,
                            startDestination = NavScreens.SplashPage.route,
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
                navController.navigate(Routes.SECURITY_PAGE) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    private companion object {
        const val IDLE_POLL_INTERVAL_MS = 5_000L
    }
}
