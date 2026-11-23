package com.bhardwaj.passkey.presentation.screens.detail_screen

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.presentation.screens.common.ObserveAsEvents
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PasskeySearchBar
import com.bhardwaj.passkey.presentation.screens.detail_screen.DetailViewModel
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.AuthenticatorCard
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.AuthenticatorDialog
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.DetailsBottomSheet
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.DetailsItem
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.PasswordHistorySheet
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.PasswordSettingsSheet
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.utils.ButtonType
import com.bhardwaj.passkey.utils.SecureClipboard
import com.bhardwaj.passkey.utils.UiText
import com.bhardwaj.passkey.utils.asString
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(onPopBackStack: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    // Stable, so rows do not recompose on every search keystroke.
    val onIntent = remember(viewModel) { viewModel::onIntent }
    // Resolved here rather than in the ViewModel so it follows the app locale.
    val bottomSheetHeading = stringResource(if (uiState.editor?.isEdit == true) R.string.edit else R.string.add)
    val searchText = uiState.query
    val details = uiState.items

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onIntent(DetailIntent.Moved(from.index, to.index))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        }
    }
    val context = LocalContext.current
    val undoLabel = stringResource(R.string.undo)

    ObserveAsEvents(viewModel.effects) { event ->
        when (event) {
            is DetailEffect.ShowUndo -> {
                scope.launch {
                    val result = snackBarHostState.showSnackbar(
                        message = event.message.asString(context),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) onIntent(DetailIntent.UndoDelete)
                }
            }

            is DetailEffect.CopyToClipboard -> {
                SecureClipboard.copy(
                    context = context,
                    text = event.value,
                    isSensitive = event.isSensitive
                )
                // Android 13+ shows its own copy confirmation, so a second one is noise.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    scope.launch {
                        snackBarHostState.showSnackbar(
                            message = UiText.StringResource(R.string.copied).asString(context)
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = { onIntent(DetailIntent.AddAuthenticatorClicked) },
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = stringResource(R.string.add_authenticator)
                    )
                }
                // Content-slot overload on purpose: the text/icon overload clears the label's semantics, which left
                // TalkBack announcing an unlabeled button (found on device).
                ExtendedFloatingActionButton(
                    onClick = { onIntent(DetailIntent.AddClicked) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.cd_add_detail))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // A visible way back: this screen had only the system Back gesture.
                IconButton(onClick = onPopBackStack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(id = R.string.details),
                    fontFamily = BebasNeue,
                    fontSize = 64.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Codes sit above the fields; each card runs its own one-second clock, so the list does not
                // recompose every second.
                uiState.authenticators.forEach { entry ->
                    AuthenticatorCard(
                        entry = entry,
                        onCopy = { onIntent(DetailIntent.AuthenticatorCodeCopied(it)) },
                        onDelete = { onIntent(DetailIntent.AuthenticatorDeleteClicked(entry)) }
                    )
                }

                if (searchText.isNotBlank() || details.isNotEmpty()) {
                    PasskeySearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        query = searchText,
                        onQueryChange = { onIntent(DetailIntent.QueryChanged(it)) },
                        onTrailingIconClick = {
                            onIntent(DetailIntent.QueryChanged(""))
                        }
                    )
                }

                if (uiState.isLoading) {
                    // Blank while the first rows load, instead of flashing the empty illustration.
                    Spacer(modifier = Modifier.weight(1F))
                } else if (details.isEmpty()) {
                    Image(
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxWidth()
                            .padding(16.dp),
                        painter = painterResource(id = R.drawable.icon_empty_list),
                        contentDescription = stringResource(id = R.string.no_details_found)
                    )
                } else {
                    LazyColumn(
                        state = lazyListState
                    ) {
                        items(
                            items = details,
                            key = { item -> item.id }
                        ) { detail ->
                            if (searchText.isNotBlank()) {
                                DetailsItem(details = detail, onIntent = onIntent)
                            } else {
                                val state = rememberSwipeToDismissBoxState(
                                    initialValue = SwipeToDismissBoxValue.Settled,
                                    positionalThreshold = { totalDistance -> totalDistance * 0.6f }
                                )

                                LaunchedEffect(state.currentValue) {
                                    if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                        onIntent(DetailIntent.SwipedToDelete(detail))
                                        state.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                }
                                ReorderableItem(
                                    reorderableLazyColumnState,
                                    "${detail.id}"
                                ) {
                                    SwipeToDismissBox(
                                        state = state,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(vertical = 6.dp)
                                                    .padding(bottom = 24.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = stringResource(R.string.cd_delete),
                                                    modifier = Modifier.align(Alignment.CenterEnd),
                                                    tint = MaterialTheme.colorScheme.background
                                                )
                                            }
                                        },
                                        content = {
                                            DetailsItem(
                                                scope = this@ReorderableItem,
                                                details = detail,
                                                onIntent = onIntent
                                            )
                                        },
                                        enableDismissFromEndToStart = true,
                                        enableDismissFromStartToEnd = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
            uiState.editor?.let { editor ->
                DetailsBottomSheet(
                    modifier = Modifier,
                    bottomSheetHeading = bottomSheetHeading,
                    detailTitle = editor.question,
                    detailResponse = editor.answer,
                    isSecret = editor.isSecret,
                    error = editor.error?.asString(context),
                    onSecretToggle = { onIntent(DetailIntent.SecretToggled(it)) },
                    onTitleChange = { onIntent(DetailIntent.QuestionChanged(it)) },
                    onDescriptionChange = { onIntent(DetailIntent.AnswerChanged(it)) },
                    onDismiss = { onIntent(DetailIntent.EditorDismissed) },
                    onSave = { onIntent(DetailIntent.SaveClicked) },
                    onGeneratePasswordClicked = { onIntent(DetailIntent.GenerateClicked) },
                    onPasswordSettingsClicked = { onIntent(DetailIntent.PolicyClicked) }
                )
            }
        }
        if (uiState.isPolicySheetOpen) {
            PasswordSettingsSheet(
                policy = uiState.policy,
                onPasswordLengthChange = { onIntent(DetailIntent.LengthChanged(it)) },
                onDismissPasswordSettings = { onIntent(DetailIntent.PolicyDismissed) },
                onToggle = { characterClass, checked ->
                    onIntent(DetailIntent.CharacterClassToggled(characterClass, checked))
                }
            )
        }
        uiState.totpEditor?.let { editor ->
            AuthenticatorDialog(
                editor = editor,
                onInputChange = { onIntent(DetailIntent.AuthenticatorInputChanged(it)) },
                onDismiss = { onIntent(DetailIntent.AuthenticatorEditorDismissed) },
                onSave = { onIntent(DetailIntent.AuthenticatorSaveClicked) }
            )
        }
        uiState.history?.let { history ->
            PasswordHistorySheet(
                history = history,
                onCopy = { onIntent(DetailIntent.LongPressed(it)) },
                onDismiss = { onIntent(DetailIntent.HistoryDismissed) }
            )
        }
        if (uiState.pendingTotpDelete != null) {
            AlertDialog(
                properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { onIntent(DetailIntent.AuthenticatorDeleteCancelled) },
                title = { Text(text = stringResource(id = R.string.cd_remove_authenticator)) },
                text = { Text(text = stringResource(id = R.string.remove_authenticator_confirm)) },
                confirmButton = {
                    PassKeyButton(
                        onClick = { onIntent(DetailIntent.AuthenticatorDeleteConfirmed) },
                        text = stringResource(id = R.string.delete),
                        buttonType = ButtonType.DEFAULT
                    )
                },
                dismissButton = {
                    PassKeyButton(
                        onClick = { onIntent(DetailIntent.AuthenticatorDeleteCancelled) },
                        text = stringResource(id = R.string.cancel),
                        buttonType = ButtonType.OUTLINED
                    )
                }
            )
        }
        if (uiState.pendingDelete != null) {
            AlertDialog(
                properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { onIntent(DetailIntent.DeleteCancelled) },
                title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
                text = { Text(text = stringResource(id = R.string.confirm_delete_description_question)) },
                confirmButton = {
                    PassKeyButton(
                        onClick = { onIntent(DetailIntent.DeleteConfirmed) },
                        text = stringResource(id = R.string.delete),
                        buttonType = ButtonType.DEFAULT
                    )
                },
                dismissButton = {
                    PassKeyButton(
                        onClick = { onIntent(DetailIntent.DeleteCancelled) },
                        text = stringResource(id = R.string.cancel),
                        buttonType = ButtonType.OUTLINED
                    )
                }
            )
        }
    }
}
