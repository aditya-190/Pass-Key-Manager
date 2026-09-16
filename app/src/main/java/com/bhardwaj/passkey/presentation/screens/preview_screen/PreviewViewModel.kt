package com.bhardwaj.passkey.presentation.screens.preview_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.presentation.navigation.Routes
import com.bhardwaj.passkey.utils.Constants.Companion.BOTTOM_SHEET_HEADING
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_CATEGORY_NAME
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_HEADING
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    private val savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsTracker
) : ViewModel() {
    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    val categoryName = savedStateHandle.getStateFlow(PREVIEW_CATEGORY_NAME, Category.BANKS.name)
    val previewHeading = savedStateHandle.getStateFlow(PREVIEW_HEADING, "")

    /** true = editing an existing row, false = adding. Resolved to text by the UI. */
    val isEditingSheet = savedStateHandle.getStateFlow(BOTTOM_SHEET_HEADING, false)

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    // Filtered by category in SQL, so a reorder renumbers one category's rows rather than a slice of a
    // globally ordered list. fromNameOrOther, never valueOf: the name comes from saved state.
    val previews: StateFlow<List<Preview>> = categoryName
        .flatMapLatest { name -> repository.getPreviewsByCategory(Category.fromNameOrOther(name)) }
        .combine(searchText) { previews, text ->
            if (text.isBlank()) {
                previews
            } else {
                previews.filter { it.heading.contains(text, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var isSheetOpen by mutableStateOf(false)
        private set

    var isAlertOpen by mutableStateOf(false)
        private set

    var preview by mutableStateOf<Preview?>(null)
        private set

    private var pendingDelete: Preview? = null
    private var lastDeleted: Pair<Preview, List<Detail>>? = null

    /** Shown inside the add/edit sheet; a failed save keeps the sheet and what was typed. */
    var sheetError by mutableStateOf<UiText?>(null)
        private set

    fun onEvent(event: PreviewEvents) {
        when (event) {
            is PreviewEvents.OnBottomNavigationClick -> {
                if (categoryName.value != event.newTitle) {
                    savedStateHandle[PREVIEW_CATEGORY_NAME] = event.newTitle
                    _searchText.value = ""
                }
            }

            PreviewEvents.OnAddPreviewClick -> {
                sheetError = null
                savedStateHandle[BOTTOM_SHEET_HEADING] = false
                isSheetOpen = true
                _searchText.value = ""
            }

            PreviewEvents.OnDismissBottomSheet -> {
                sheetError = null
                savedStateHandle[PREVIEW_HEADING] = ""
                isSheetOpen = false
            }

            is PreviewEvents.OnChangeClick -> {
                viewModelScope.launch {
                    _searchText.value = ""
                    repository.getPreviewById(event.preview.id)?.let { preview ->
                        isSheetOpen = true
                        savedStateHandle[BOTTOM_SHEET_HEADING] = true
                        savedStateHandle[PREVIEW_HEADING] = event.preview.heading
                        this@PreviewViewModel.preview = preview
                    }
                }
            }

            is PreviewEvents.OnHeadingChange -> {
                sheetError = null
                savedStateHandle[PREVIEW_HEADING] = event.newHeading
            }

            is PreviewEvents.OnSaveClick -> {
                viewModelScope.launch {
                    if (previewHeading.value.isBlank()) {
                        sheetError = UiText.StringResource(R.string.enter_valid_heading)
                        return@launch
                    }

                    val heading = previewHeading.value.trim()
                    val category = Category.fromNameOrOther(categoryName.value)

                    val existingPreview = repository.getPreviewByHeading(heading, category)
                    // When editing, the lookup finds the very row being edited. Treating that as
                    // a clash meant saving an edit without renaming it reported "heading exists"
                    // and silently discarded the edit.
                    val isClashWithAnotherRow =
                        existingPreview != null && existingPreview.id != preview?.id
                    if (isClashWithAnotherRow) {
                        sheetError = UiText.StringResource(R.string.heading_exists)
                        return@launch
                    }

                    preview?.let {
                        repository.updatePreview(it.copy(heading = heading, category = category))
                    } ?: run {
                        repository.createPreview(heading = heading, category = category)
                        analytics.log(AppEvents.entryCreated(category))
                    }

                    preview = null
                    savedStateHandle[PREVIEW_HEADING] = ""
                    isSheetOpen = false
                }
            }

            is PreviewEvents.OnPreviewClick -> {
                _searchText.value = ""
                sendUiEvents(UiEvents.Navigate(Routes.DETAILS_PAGE + "?previewId=${event.previewId}"))
            }

            is PreviewEvents.OnLongPress -> {
                sendUiEvents(UiEvents.CopyToClipboard(value = event.previewHeading, isSensitive = false))
                analytics.log(AppEvents.valueCopied(sensitive = false))
            }

            PreviewEvents.OnSettingsClick -> {
                _searchText.value = ""
                sendUiEvents(UiEvents.Navigate(Routes.SETTINGS_PAGE))
            }

            // Nothing is written until the user confirms. Deleting on swipe and re-inserting on Cancel lost the
            // entry if the app died with the dialog open, and left its fields orphaned on Delete.
            is PreviewEvents.OnSwipedLeft -> {
                pendingDelete = event.preview
                isAlertOpen = true
            }

            PreviewEvents.OnDismissAlertDialog, PreviewEvents.OnCancelClick -> {
                pendingDelete = null
                isAlertOpen = false
            }

            is PreviewEvents.OnDeleteClick -> {
                val preview = pendingDelete ?: return
                pendingDelete = null
                isAlertOpen = false
                viewModelScope.launch {
                    val details = repository.getDetailsByPreviewId(preview.id).first()
                    repository.runInTransaction {
                        repository.deleteDetailsByPreviewId(preview.id)
                        repository.deletePreview(preview)
                    }
                    lastDeleted = preview to details
                    sendUiEvents(UiEvents.ShowUndo(UiText.StringResource(R.string.entry_deleted)))
                }
            }

            PreviewEvents.OnUndoDelete -> {
                val (preview, details) = lastDeleted ?: return
                lastDeleted = null
                viewModelScope.launch {
                    repository.runInTransaction {
                        // Same ids as before the delete, so the details point at the restored row.
                        repository.updatePreview(preview)
                        details.forEach { repository.updateDetail(it) }
                    }
                }
            }

            is PreviewEvents.OnSearchTextUpdate -> {
                _searchText.value = event.newText
            }

            is PreviewEvents.OnReorderPreview -> {
                viewModelScope.launch {
                    val newList = previews.value.toMutableList().apply {
                        add(event.to.index, removeAt(event.from.index))
                    }

                    val updatedList = newList.mapIndexed { index, preview ->
                        preview.copy(sequence = index.toLong())
                    }

                    // One transaction, not N separate writes: a reorder interrupted midway
                    // used to leave the list in a partially renumbered state.
                    repository.runInTransaction {
                        updatedList.forEach { preview ->
                            repository.updatePreviewSequence(
                                previewId = preview.id,
                                sequence = preview.sequence
                            )
                        }
                    }
                }
            }
        }
    }

    private fun sendUiEvents(events: UiEvents) {
        viewModelScope.launch {
            _uiEvents.send(events)
        }
    }
}
