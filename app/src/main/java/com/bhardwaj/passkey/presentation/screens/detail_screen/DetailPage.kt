package com.bhardwaj.passkey.presentation.screens.detail_screen

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.events.DetailEvents
import com.bhardwaj.passkey.domain.viewModels.DetailViewModel
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PasskeySearchBar
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.DetailsBottomSheet
import com.bhardwaj.passkey.presentation.screens.detail_screen.components.DetailsItem
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.utils.ButtonType
import com.bhardwaj.passkey.utils.UiEvents
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailScreen(
    onPopBackStack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val detailTitle by viewModel.detailTitle.collectAsState()
    val detailResponse by viewModel.detailResponse.collectAsState()
    val bottomSheetHeading by viewModel.bottomSheetHeading.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val details by viewModel.details.collectAsState(initial = emptyList())

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val lazyListState = rememberLazyListState()
    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.onEvent(DetailEvents.OnReorderDetails(from, to))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
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
        snackbarHost = { SnackbarHost(snackBarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(bottom = 42.dp),
                onClick = {
                    viewModel.onEvent(DetailEvents.OnAddDetailClick)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                content = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Detail",
                    )
                },
            )
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
                Icon(
                    painter = painterResource(id = R.drawable.icon_logo),
                    contentDescription = "App Icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(id = R.string.details),
                    fontFamily = BebasNeue,
                    fontSize = 64.sp,
                    color = MaterialTheme.colorScheme.secondary
                )

                if (searchText.isNotBlank() || details.isNotEmpty()) {
                    PasskeySearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        query = searchText,
                        onQueryChange = { viewModel.onEvent(DetailEvents.OnSearchTextUpdate(newText = it)) },
                        onTrailingIconClick = {
                            viewModel.onEvent(DetailEvents.OnSearchTextUpdate(newText = ""))
                        }
                    )
                }

                if (details.isEmpty()) {
                    Image(
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxWidth()
                            .padding(16.dp),
                        painter = painterResource(id = R.drawable.icon_empty_list),
                        contentDescription = "No Previews Found"
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                    ) {
                        items(
                            items = details,
                            key = { item -> "${item.detailsId}" }
                        ) { detail ->
                            if (searchText.isNotBlank()) {
                                DetailsItem(details = detail, onEvent = viewModel::onEvent)
                            } else {
                                val state = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.onEvent(DetailEvents.OnSwipedLeft(detail))
                                        }
                                        true
                                    },
                                    positionalThreshold = { density ->
                                        0.6F * density
                                    }
                                )
                                ReorderableItem(
                                    reorderableLazyColumnState,
                                    "${detail.detailsId}"
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
                                                    contentDescription = "Icon Delete",
                                                    modifier = Modifier.align(Alignment.CenterEnd),
                                                    tint = MaterialTheme.colorScheme.background
                                                )
                                            }
                                        },
                                        content = {
                                            DetailsItem(
                                                scope = this@ReorderableItem,
                                                details = detail,
                                                onEvent = viewModel::onEvent
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
            if (viewModel.isSheetOpen) {
                DetailsBottomSheet(
                    modifier = Modifier,
                    bottomSheetHeading = bottomSheetHeading,
                    detailTitle = detailTitle,
                    detailResponse = detailResponse,
                    onTitleChange = {
                        viewModel.onEvent(DetailEvents.OnTitleChange(it))
                    },
                    onDescriptionChange = {
                        viewModel.onEvent(DetailEvents.OnDescriptionChange(it))
                    },
                    onDismiss = {
                        viewModel.onEvent(DetailEvents.OnDismissBottomSheet)
                    },
                    onSave = {
                        viewModel.onEvent(DetailEvents.OnSaveClick)
                    }
                )
            }
        }
        if (viewModel.isAlertOpen) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.onEvent(DetailEvents.OnDismissAlertDialog)
                },
                title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
                text = { Text(text = stringResource(id = R.string.confirm_delete_description_question)) },
                confirmButton = {
                    PassKeyButton(
                        onClick = {
                            viewModel.onEvent(DetailEvents.OnDeleteClick)
                        },
                        text = stringResource(id = R.string.delete),
                        buttonType = ButtonType.DEFAULT
                    )
                },
                dismissButton = {
                    PassKeyButton(
                        onClick = {
                            viewModel.onEvent(DetailEvents.OnCancelClick)
                        },
                        text = stringResource(id = R.string.cancel),
                        buttonType = ButtonType.OUTLINED
                    )
                },
            )
        }
    }
}