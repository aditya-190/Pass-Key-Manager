package com.bhardwaj.passkey.presentation.screens.settings_screen

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.about.ExternalLinks
import com.bhardwaj.passkey.data.backup.BackupFormat
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.legal.LegalDocument
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.common.ObserveAsEvents
import com.bhardwaj.passkey.presentation.screens.common.SecureModalBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsViewModel
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.AnalysisBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.AutoLockDialog
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.BackupPasswordDialog
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.LanguageBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.SettingsText
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins
import com.bhardwaj.passkey.utils.asString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onPopBackStack: () -> Unit,
    onNavigate: (NavRoute) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val onIntent = remember(viewModel) { viewModel::onIntent }
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    // Backup file selection. The password is collected only after a destination or source is
    // chosen, so the user is never asked for one and then cancels out of the picker.
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> pendingExportUri = uri }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            if (isEncryptedBackup(it, context)) {
                pendingImportUri = it
            } else {
                // A legacy plaintext .passkey CSV needs no password; restore it directly.
                onIntent(SettingsIntent.ImportChosen(uri = it, password = null, mode = ImportMode.MERGE))
            }
        }
    }

    // Two steps: prove the current password, then choose the new one. Reuses the same dialog.
    when (uiState.dialog) {
        SettingsDialog.RecoveryCurrent -> BackupPasswordDialog(
            confirmMode = false,
            titleRes = R.string.recovery_change_current_title,
            hintRes = R.string.recovery_password_hint,
            messageRes = R.string.recovery_change_current_message,
            onDismiss = { onIntent(SettingsIntent.DialogDismissed) },
            onConfirm = { onIntent(SettingsIntent.CurrentRecoveryEntered(it)) }
        )

        SettingsDialog.RecoveryNew -> BackupPasswordDialog(
            confirmMode = true,
            titleRes = R.string.recovery_change_new_title,
            recoveryRules = true,
            hintRes = R.string.recovery_password_hint,
            messageRes = R.string.recovery_change_new_message,
            onDismiss = { onIntent(SettingsIntent.DialogDismissed) },
            onConfirm = { onIntent(SettingsIntent.NewRecoveryEntered(it)) }
        )

        SettingsDialog.AutofillHelp -> AlertDialog(
            properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
            onDismissRequest = { onIntent(SettingsIntent.DialogDismissed) },
            title = { Text(text = stringResource(R.string.autofill)) },
            text = { Text(text = stringResource(R.string.autofill_setup_help)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.AutofillHelpContinue) }) {
                    Text(text = stringResource(R.string.backup_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DialogDismissed) }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )

        SettingsDialog.AutoLock -> AutoLockDialog(
            current = uiState.autoLockTimeout,
            onDismiss = { onIntent(SettingsIntent.DialogDismissed) },
            onSelect = { onIntent(SettingsIntent.AutoLockChosen(it)) }
        )

        else -> Unit
    }

    pendingExportUri?.let { uri ->
        BackupPasswordDialog(
            confirmMode = true,
            onDismiss = { pendingExportUri = null },
            onConfirm = { password ->
                pendingExportUri = null
                onIntent(SettingsIntent.ExportChosen(uri, password))
            }
        )
    }

    pendingImportUri?.let { uri ->
        BackupPasswordDialog(
            confirmMode = false,
            onDismiss = { pendingImportUri = null },
            onConfirm = { password ->
                pendingImportUri = null
                onIntent(SettingsIntent.ImportChosen(uri, password, ImportMode.MERGE))
            }
        )
    }

    val storeUnavailable = stringResource(R.string.store_unavailable)
    val autofillUnsupported = stringResource(R.string.autofill_unsupported)
    ObserveAsEvents(viewModel.effects) { event ->
        when (event) {
            is SettingsEffect.Navigate -> onNavigate(event.route)
            is SettingsEffect.OpenUrl -> if (!ExternalLinks.openUrl(context, event.url)) {
                // No browser or Play Store (some Android TV, kiosk and de-Googled devices).
                scope.launch { snackBarHostState.showSnackbar(storeUnavailable) }
            }

            // The system's autofill picker. There is no API to switch a service on directly, which is the right
            // restriction for something that can read other apps' forms.
            SettingsEffect.OpenAutofillSettings -> {
                val manager = context.getSystemService(AutofillManager::class.java)
                val opened = manager?.isAutofillSupported == true &&
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE
                            ).setData("package:${context.packageName}".toUri())
                        )
                    }.isSuccess
                if (!opened) {
                    scope.launch { snackBarHostState.showSnackbar(autofillUnsupported) }
                }
            }
            is SettingsEffect.ApplyLanguage -> applyAppLanguage(context, event.languageTag)
            is SettingsEffect.ShowSnackbar -> {
                scope.launch { snackBarHostState.showSnackbar(message = event.message.asString(context)) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        // Privacy, Terms and About are full screens now; no scaffold sheet (its 0 px state crashed, seed 5521).
        Box(modifier = Modifier.padding(paddingValues)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Column(
                    // Scrolls so every row stays reachable at 200% font size and in landscape.
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        IconButton(onClick = onPopBackStack, modifier = Modifier.offset(x = (-12).dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.more),
                            fontFamily = BebasNeue,
                            fontSize = 64.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            SettingsText(text = stringResource(id = R.string.change_language)) {
                                onIntent(SettingsIntent.LanguageClicked)
                            }
                            SettingsText(text = stringResource(id = R.string.analyze_passwords)) {
                                onIntent(SettingsIntent.AnalyzeClicked)
                            }
                            SettingsText(text = stringResource(id = R.string.rate_app)) {
                                onIntent(SettingsIntent.RateClicked)
                            }
                            SettingsText(text = stringResource(id = R.string.autofill)) {
                                onIntent(SettingsIntent.AutofillClicked)
                            }
                            SettingsText(text = stringResource(id = R.string.auto_lock)) {
                                onIntent(SettingsIntent.AutoLockClicked)
                            }
                            SettingsText(
                                text = stringResource(id = R.string.change_recovery_password)
                            ) {
                                onIntent(SettingsIntent.ChangeRecoveryClicked)
                            }
                            SettingsText(text = stringResource(id = R.string.import_data)) {
                                importLauncher.launch(arrayOf("*/*"))
                            }
                            SettingsText(text = stringResource(id = R.string.export_data)) {
                                exportLauncher.launch(defaultBackupFileName())
                            }
                            SettingsText(text = stringResource(id = R.string.privacy)) {
                                onNavigate(NavRoute.Legal(LegalDocument.PRIVACY.name))
                            }
                            SettingsText(text = stringResource(id = R.string.privacy_options)) {
                                onNavigate(NavRoute.Consent(fromSettings = true))
                            }
                            SettingsText(text = stringResource(id = R.string.terms_n_condition)) {
                                onNavigate(NavRoute.Legal(LegalDocument.TERMS.name))
                            }
                            SettingsText(text = stringResource(id = R.string.about)) {
                                onNavigate(NavRoute.About)
                            }
                        }
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.app_version, uiState.appVersion),
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    if (uiState.dialog == SettingsDialog.Language) {
        val sheetState = rememberModalBottomSheetState()
        SecureModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { onIntent(SettingsIntent.DialogDismissed) },
            dragHandle = {},
            shape = RectangleShape
        ) {
            LanguageBottomSheet(
                onLanguageChange = { onIntent(SettingsIntent.LanguageChosen(it)) },
                onBackIconClick = { onIntent(SettingsIntent.DialogDismissed) }
            )
        }
    }

    (uiState.dialog as? SettingsDialog.Analysis)?.let { analysis ->
        AnalysisBottomSheet(
            result = analysis.result,
            onDismiss = { onIntent(SettingsIntent.DialogDismissed) },
            onDetailClick = { detail -> onIntent(SettingsIntent.AnalysisItemClicked(detail)) }
        )
    }
}

/** Android 13+ stores the per-app language in the system; older versions go through AppCompat. */
private fun applyAppLanguage(context: Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(languageTag)
    } else {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}

/**
 * Detects the encrypted `.pkbak` format by its magic bytes rather than by filename.
 *
 * The previous check matched a display name ending in "passkey", which would have rejected
 * every new backup, and trusted a user-supplied filename to decide how to parse the contents.
 */
private fun isEncryptedBackup(uri: Uri, context: Context): Boolean = try {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val magic = ByteArray(8)
        val read = input.read(magic)
        read == magic.size && BackupFormat.hasMagic(magic)
    } == true
} catch (_: Exception) {
    false
}

private fun defaultBackupFileName(): String {
    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    return "passkey_backup_$stamp.pkbak"
}
