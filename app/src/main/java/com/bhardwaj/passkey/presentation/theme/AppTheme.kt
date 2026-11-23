package com.bhardwaj.passkey.presentation.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * The user's Light / Dark / System choice (6.0.0) for any activity. Nothing is drawn until the choice is read (a few
 * milliseconds; the window background shows meanwhile), so a forced scheme never flashes the other one and the main
 * thread never waits on storage. On Android 12+ the system is told too, so the launch screen and every window,
 * including the autofill prompt, start in the chosen scheme.
 */
@Composable
fun PassKeyAppTheme(
    themeMode: Flow<ThemeMode>,
    onDarkChanged: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val mode by themeMode.collectAsStateWithLifecycle(initialValue = null)
    val current = mode ?: return
    val dark = current.isDark(systemDark = isSystemInDarkTheme())
    val context = LocalContext.current
    LaunchedEffect(current) { applySystemNightMode(context, current) }
    LaunchedEffect(dark) { onDarkChanged(dark) }
    PassKeyTheme(darkTheme = dark, content = content)
}

/** A no-op before Android 12, and when the value is already set (the system then changes nothing). */
fun applySystemNightMode(context: Context, mode: ThemeMode) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode.nightMode())
    }
}
