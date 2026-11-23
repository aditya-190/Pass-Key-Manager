package com.bhardwaj.passkey.presentation.screens.preview_screen

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
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
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.presentation.navigation.HomeAnnouncement
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.presentation.screens.common.ObserveAsEvents
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PasskeySearchBar
import com.bhardwaj.passkey.presentation.screens.common.labelRes
import com.bhardwaj.passkey.presentation.screens.preview_screen.PreviewViewModel
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.MainBottomNavigation
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.PreviewBottomSheet
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.PreviewItem
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.bottomNavigationList
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
fun PreviewScreen(onNavigate: (NavRoute) -> Unit, viewModel: PreviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A stable reference: `viewModel::onIntent` is a new object on every recomposition, so every row
    // recomposed on each keystroke in the search bar.
    val onIntent = remember(viewModel) { viewModel::onIntent }
    // Localized display names come from Category.labelRes, which the bottom navigation
    // also uses, so the two can no longer drift apart.
    val categoryNameMap = Category.entries.associate { it.name to stringResource(it.labelRes()) }

    // Resolved here rather than in the ViewModel so it follows the app locale.
    val bottomSheetHeading = stringResource(if (state.editor?.isEdit == true) R.string.edit else R.string.add)
    val searchText = state.query
    val previews = state.items
    val selectedIndex = bottomNavigationList.indexOfFirst { it.category == state.category }

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onIntent(PreviewIntent.Moved(from.index, to.index))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        }
    }

    val context = LocalContext.current
    val undoLabel = stringResource(R.string.undo)

    ObserveAsEvents(viewModel.effects) { event ->
        when (event) {
            is PreviewEffect.Navigate -> onNavigate(event.route)
            is PreviewEffect.ShowUndo -> {
                scope.launch {
                    val result = snackBarHostState.showSnackbar(
                        message = event.message.asString(context),
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) onIntent(PreviewIntent.UndoDelete)
                }
            }

            is PreviewEffect.CopyToClipboard -> {
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
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                MainBottomNavigation(
                    selectedIndex = selectedIndex,
                    onItemClick = { _, newTitle ->
                        onIntent(PreviewIntent.CategorySelected(Category.fromNameOrOther(newTitle)))
                    }
                )
            }
        },
        // A labelled button: reviewers could not find the old icon-only button docked in the tab bar.
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            // Content-slot overload on purpose: the text/icon overload clears the label's semantics, which left
            // TalkBack announcing an unlabeled button (found on device).
            ExtendedFloatingActionButton(
                onClick = { onIntent(PreviewIntent.AddClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.cd_add_entry))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { onIntent(PreviewIntent.SettingsClicked) }
                            )
                    )
                }
                categoryNameMap[state.category.name]?.let {
                    Text(
                        text = it,
                        fontFamily = BebasNeue,
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                HomeAnnouncement(onNavigate = onNavigate)

                if (searchText.isNotBlank() || previews.isNotEmpty()) {
                    PasskeySearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        query = searchText,
                        onQueryChange = { onIntent(PreviewIntent.QueryChanged(it)) },
                        onTrailingIconClick = {
                            onIntent(PreviewIntent.QueryChanged(""))
                        }
                    )
                }

                if (state.isLoading) {
                    // Blank while the first rows load, instead of flashing the empty illustration.
                    Spacer(modifier = Modifier.weight(1F))
                } else if (previews.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            modifier = Modifier
                                .weight(1F, fill = false)
                                .fillMaxWidth()
                                .padding(16.dp),
                            painter = painterResource(id = R.drawable.icon_empty_list),
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(id = R.string.no_previews_found),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState
                    ) {
                        items(
                            items = previews,
                            key = { item -> item.id }
                        ) { preview ->
                            if (searchText.isNotBlank()) {
                                PreviewItem(
                                    preview = preview,
                                    onIntent = onIntent
                                )
                            } else {
                                val state = rememberSwipeToDismissBoxState(
                                    initialValue = SwipeToDismissBoxValue.Settled,
                                    positionalThreshold = { totalDistance -> totalDistance * 0.6f }
                                )

                                LaunchedEffect(state.currentValue) {
                                    if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                        onIntent(PreviewIntent.SwipedToDelete(preview))
                                        state.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                }
                                ReorderableItem(
                                    reorderableLazyColumnState,
                                    "${preview.id}"
                                ) {
                                    SwipeToDismissBox(
                                        state = state,
                                        backgroundContent = {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
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
                                            PreviewItem(
                                                scope = this@ReorderableItem,
                                                preview = preview,
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
            state.editor?.let { editor ->
                PreviewBottomSheet(
                    bottomSheetHeading = bottomSheetHeading,
                    previewHeading = editor.heading,
                    error = editor.error?.asString(context),
                    onDismiss = { onIntent(PreviewIntent.EditorDismissed) },
                    onHeadingChange = { onIntent(PreviewIntent.HeadingChanged(it)) },
                    onSave = { onIntent(PreviewIntent.SaveClicked) }
                )
            }
        }
        if (state.pendingDelete != null) {
            AlertDialog(
                properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = { onIntent(PreviewIntent.DeleteCancelled) },
                title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
                text = { Text(text = stringResource(id = R.string.confirm_delete_description_heading)) },
                confirmButton = {
                    PassKeyButton(
                        onClick = { onIntent(PreviewIntent.DeleteConfirmed) },
                        text = stringResource(id = R.string.delete),
                        buttonType = ButtonType.DEFAULT
                    )
                },
                dismissButton = {
                    PassKeyButton(
                        onClick = { onIntent(PreviewIntent.DeleteCancelled) },
                        text = stringResource(id = R.string.cancel),
                        buttonType = ButtonType.OUTLINED
                    )
                }
            )
        }
    }
}
