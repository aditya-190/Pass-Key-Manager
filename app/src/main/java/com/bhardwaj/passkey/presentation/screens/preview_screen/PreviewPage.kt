package com.bhardwaj.passkey.presentation.screens.preview_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.domain.viewModels.PreviewViewModel
import com.bhardwaj.passkey.domain.events.PreviewEvents
import com.bhardwaj.passkey.presentation.screens.common.PassKeyButton
import com.bhardwaj.passkey.presentation.screens.common.PasskeySearchBar
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.MainBottomNavigation
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.PreviewBottomSheet
import com.bhardwaj.passkey.presentation.screens.preview_screen.components.PreviewItem
import com.bhardwaj.passkey.presentation.theme.BebasNeue
import com.bhardwaj.passkey.utils.ButtonType
import com.bhardwaj.passkey.utils.Categories
import com.bhardwaj.passkey.utils.UiEvents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    onNavigate: (UiEvents.Navigate) -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    val categoryName by viewModel.categoryName.collectAsState()
    val categoryNameMap = mapOf(
        Categories.BANKS.name to stringResource(id = R.string.banks),
        Categories.APPS.name to stringResource(id = R.string.apps),
        Categories.MAILS.name to stringResource(id = R.string.mails),
        Categories.OTHERS.name to stringResource(id = R.string.others),
    )

    val previewHeading by viewModel.previewHeading.collectAsState()
    val bottomSheetHeading by viewModel.bottomSheetHeading.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val previews by viewModel.previews.collectAsState()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiEvents.Navigate -> onNavigate(event)
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
        bottomBar = {
            MainBottomNavigation(
                selectedIndex = selectedIndex,
                onItemClick = { newIndex, newTitle ->
                    viewModel.onEvent(PreviewEvents.OnBottomNavigationClick(newTitle))
                    selectedIndex = newIndex
                },
                onFabClick = {
                    viewModel.onEvent(PreviewEvents.OnAddPreviewClick)
                }
            )
        },
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
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { viewModel.onEvent(PreviewEvents.OnSettingsClick) }
                            )
                    )
                }
                categoryNameMap[categoryName]?.let {
                    Text(
                        text = it,
                        fontFamily = BebasNeue,
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (searchText.isNotBlank() || previews.isNotEmpty()) {
                    PasskeySearchBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        query = searchText,
                        onQueryChange = { viewModel.onEvent(PreviewEvents.OnSearchTextUpdate(newText = it)) },
                        onTrailingIconClick = {
                            viewModel.onEvent(PreviewEvents.OnSearchTextUpdate(newText = ""))
                        }
                    )
                }

                if (previews.isEmpty()) {
                    Image(
                        modifier = Modifier
                            .weight(1F)
                            .fillMaxWidth()
                            .padding(16.dp),
                        painter = painterResource(id = R.drawable.icon_empty_list),
                        contentDescription = "No Previews Found"
                    )
                } else {
                    LazyColumn {
                        itemsIndexed(
                            items = previews,
                            key = { _, item ->
                                item.hashCode()
                            }
                        ) { _, preview ->
                            if (searchText.isNotBlank()) {
                                PreviewItem(preview = preview, onEvent = viewModel::onEvent)
                            } else {
                                val state = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) {
                                            viewModel.onEvent(PreviewEvents.OnSwipedLeft(preview))
                                        }
                                        true
                                    },
                                    positionalThreshold = { density ->
                                        0.6F * density
                                    }
                                )
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
                                                contentDescription = "Icon Delete",
                                                modifier = Modifier.align(Alignment.CenterEnd),
                                                tint = MaterialTheme.colorScheme.background
                                            )
                                        }
                                    },
                                    content = {
                                        PreviewItem(preview = preview, onEvent = viewModel::onEvent)
                                    },
                                    enableDismissFromEndToStart = true,
                                )
                            }
                        }
                    }
                }
            }
            if (viewModel.isSheetOpen) {
                PreviewBottomSheet(
                    bottomSheetHeading = bottomSheetHeading,
                    previewHeading = previewHeading,
                    onDismiss = {
                        viewModel.onEvent(PreviewEvents.OnDismissBottomSheet)
                    },
                    onHeadingChange = {
                        viewModel.onEvent(PreviewEvents.OnHeadingChange(it))
                    },
                    onSave = {
                        viewModel.onEvent(PreviewEvents.OnSaveClick)
                    }
                )
            }
        }
        if (viewModel.isAlertOpen) {
            AlertDialog(
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    viewModel.onEvent(PreviewEvents.OnDismissAlertDialog)
                },
                title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
                text = { Text(text = stringResource(id = R.string.confirm_delete_description_heading)) },
                confirmButton = {
                    PassKeyButton(
                        onClick = {
                            viewModel.onEvent(PreviewEvents.OnDeleteClick)
                        },
                        text = stringResource(id = R.string.delete),
                        buttonType = ButtonType.DEFAULT
                    )
                },
                dismissButton = {
                    PassKeyButton(
                        onClick = {
                            viewModel.onEvent(PreviewEvents.OnCancelClick)
                        },
                        text = stringResource(id = R.string.cancel),
                        buttonType = ButtonType.OUTLINED
                    )
                }
            )
        }
    }
}