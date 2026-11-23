package com.bhardwaj.passkey.presentation.screens.settings_screen

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.backup.BackupFormat
import com.bhardwaj.passkey.data.backup.ImportMode
import com.bhardwaj.passkey.legal.LegalDocument
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.common.ObserveAsEvents
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsEvents
import com.bhardwaj.passkey.presentation.screens.settings_screen.SettingsViewModel
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.AnalysisBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.AutoLockDialog
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.BackupPasswordDialog
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.LanguageBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.SettingsText
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins
import com.bhardwaj.passkey.utils.UiEvents
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
                viewModel.onEvent(
                    SettingsEvents.OnImportFileChosen(
                        uri = it,
                        password = null,
                        mode = ImportMode.MERGE
                    )
                )
            }
        }
    }

    // Two steps: prove the current password, then choose the new one. Reuses the same dialog.
    when (viewModel.recoveryChangeStep) {
        false -> BackupPasswordDialog(
            confirmMode = false,
            titleRes = R.string.recovery_change_current_title,
            hintRes = R.string.recovery_password_hint,
            messageRes = R.string.recovery_change_current_message,
            onDismiss = { viewModel.onEvent(SettingsEvents.OnDismissRecoveryChange) },
            onConfirm = { viewModel.onEvent(SettingsEvents.OnCurrentRecoveryPasswordEntered(it)) }
        )

        true -> BackupPasswordDialog(
            confirmMode = true,
            titleRes = R.string.recovery_change_new_title,
            recoveryRules = true,
            hintRes = R.string.recovery_password_hint,
            messageRes = R.string.recovery_change_new_message,
            onDismiss = { viewModel.onEvent(SettingsEvents.OnDismissRecoveryChange) },
            onConfirm = { viewModel.onEvent(SettingsEvents.OnNewRecoveryPasswordEntered(it)) }
        )

        null -> Unit
    }

    val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
    if (viewModel.isAutoLockDialogOpen) {
        AutoLockDialog(
            current = autoLockTimeout,
            onDismiss = { viewModel.onEvent(SettingsEvents.OnDismissAutoLockDialog) },
            onSelect = { viewModel.onEvent(SettingsEvents.OnAutoLockTimeoutChange(it)) }
        )
    }

    pendingExportUri?.let { uri ->
        BackupPasswordDialog(
            confirmMode = true,
            onDismiss = { pendingExportUri = null },
            onConfirm = { password ->
                pendingExportUri = null
                viewModel.onEvent(SettingsEvents.OnExportFileChosen(uri, password))
            }
        )
    }

    pendingImportUri?.let { uri ->
        BackupPasswordDialog(
            confirmMode = false,
            onDismiss = { pendingImportUri = null },
            onConfirm = { password ->
                pendingImportUri = null
                viewModel.onEvent(
                    SettingsEvents.OnImportFileChosen(uri, password, ImportMode.MERGE)
                )
            }
        )
    }

    val isAnalysisSheetOpen = viewModel.isAnalysisSheetOpen
    val analysisResult = viewModel.analysisResult

    ObserveAsEvents(viewModel.uiEvents) { event ->
        when (event) {
            is UiEvents.PopBackStack -> onPopBackStack()
            is UiEvents.Navigate -> onNavigate(event.route)
            is UiEvents.ShowSnackBar -> {
                scope.launch {
                    snackBarHostState.showSnackbar(
                        message = event.message.asString(context),
                        actionLabel = event.action?.asString(context)
                    )
                }
            }

            else -> Unit
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
                                viewModel.onEvent(SettingsEvents.OnLanguageClick)
                            }
                            SettingsText(text = stringResource(id = R.string.analyze_passwords)) {
                                viewModel.onEvent(SettingsEvents.OnAnalyzePasswordsClick)
                            }
                            SettingsText(text = stringResource(id = R.string.rate_app)) {
                                viewModel.onEvent(SettingsEvents.OnRateAppClick)
                            }
                            SettingsText(text = stringResource(id = R.string.auto_lock)) {
                                viewModel.onEvent(SettingsEvents.OnAutoLockClick)
                            }
                            SettingsText(
                                text = stringResource(id = R.string.change_recovery_password)
                            ) {
                                viewModel.onEvent(SettingsEvents.OnChangeRecoveryPasswordClick)
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
                        text = stringResource(id = R.string.app_version, viewModel.appVersion),
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    if (viewModel.isSheetOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            properties = ModalBottomSheetProperties(
                securePolicy = SecureFlagPolicy.SecureOn
            ),
            sheetState = sheetState,
            onDismissRequest = { viewModel.onEvent(SettingsEvents.OnDismissBottomSheet) },
            dragHandle = {},
            shape = RectangleShape
        ) {
            LanguageBottomSheet(
                onLanguageChange = {
                    viewModel.onEvent(
                        SettingsEvents.OnLanguageChange(
                            newLanguage = it
                        )
                    )
                },
                onBackIconClick = {
                    viewModel.onEvent(SettingsEvents.OnDismissBottomSheet)
                }
            )
        }
    }

    if (isAnalysisSheetOpen) {
        AnalysisBottomSheet(
            result = analysisResult,
            onDismiss = { viewModel.onEvent(SettingsEvents.OnDismissAnalysisSheet) },
            onDetailClick = { detail ->
                viewModel.onEvent(SettingsEvents.OnAnalysisItemClick(detail))
            }
        )
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
