package com.bhardwaj.passkey.presentation.screens.settings_screen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.events.SettingsEvents
import com.bhardwaj.passkey.domain.viewModels.SettingsViewModel
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.FaqItem
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.LanguageBottomSheet
import com.bhardwaj.passkey.presentation.screens.settings_screen.components.SettingsText
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.presentation.theme.Poppins
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_PICKER_TYPE
import com.bhardwaj.passkey.utils.UiEvents
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onPopBackStack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val context = LocalContext.current
    val importDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val uri: Uri? = result.data?.data
                if(uri != null) {
                    val fileName = getFileName(context, uri)
                    if(fileName.endsWith(".passkey")) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                            ?: return@rememberLauncherForActivityResult
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val content = reader.readText()
                        viewModel.onEvent(SettingsEvents.OnImportDataClick(content))
                        reader.close()
                    } else {
                        scope.launch {
                            snackBarHostState.showSnackbar(message = "Invalid file selected.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val exportPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(SettingsEvents.OnExportDataClick)
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvents.PopBackStack -> onPopBackStack()
                is UiEvents.ShowSnackBar -> {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.action
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        BottomSheetScaffold(
            modifier = Modifier.padding(paddingValues),
            scaffoldState = scaffoldState,
            sheetPeekHeight = 0.dp,
            sheetShape = RectangleShape,
            sheetSwipeEnabled = true,
            sheetDragHandle = {},
            sheetContent = {
                if (viewModel.isSheetOpen) {
                    val sheetState = rememberModalBottomSheetState()
                    ModalBottomSheet(
                        sheetState = sheetState,
                        onDismissRequest = { viewModel.onEvent(SettingsEvents.OnDismissBottomSheet) },
                        dragHandle = {},
                        shape = RectangleShape,
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
                } else {
                    FaqItem(openedBy = viewModel.bottomSheetOpenedBy)
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_logo),
                            contentDescription = "App Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        ) {
                            SettingsText(text = stringResource(id = R.string.change_language)) {
                                viewModel.onEvent(SettingsEvents.OnLanguageClick)
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            }
                            SettingsText(text = stringResource(id = R.string.rate_app)) {
                                viewModel.onEvent(SettingsEvents.OnRateAppClick)
                            }
                            SettingsText(text = stringResource(id = R.string.import_data)) {
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "text/*"
                                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(FILE_PICKER_TYPE))
                                }
                                importDataLauncher.launch(intent)
                            }
                            SettingsText(text = stringResource(id = R.string.export_data)) {
                                exportDataToStorage(
                                    appContext = context,
                                    viewModel = viewModel,
                                    exportPermissionLauncher = exportPermissionLauncher
                                )
                            }
                            SettingsText(text = stringResource(id = R.string.privacy)) {
                                viewModel.onEvent(SettingsEvents.OnPrivacyClick)
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            }
                            SettingsText(text = stringResource(id = R.string.terms_n_condition)) {
                                viewModel.onEvent(SettingsEvents.OnTermsAndConditionClick)
                                scope.launch { scaffoldState.bottomSheetState.expand() }
                            }
                            SettingsText(text = stringResource(id = R.string.about)) {
                                viewModel.onEvent(SettingsEvents.OnAboutClick)
                                scope.launch { scaffoldState.bottomSheetState.expand() }
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
}

private fun exportDataToStorage(
    appContext: Context,
    viewModel: SettingsViewModel,
    exportPermissionLauncher: ActivityResultLauncher<String>
) {
    if (ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        viewModel.onEvent(SettingsEvents.OnExportDataClick)
    } else {
        exportPermissionLauncher.launch(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}

private fun getFileName(context: Context, uri: Uri):String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = it.getString(nameIndex)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result ?: ""
}
