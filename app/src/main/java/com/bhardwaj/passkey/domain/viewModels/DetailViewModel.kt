package com.bhardwaj.passkey.domain.viewModels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.events.DetailEvents
import com.bhardwaj.passkey.utils.Constants.Companion.BOTTOM_SHEET_HEADING
import com.bhardwaj.passkey.utils.Constants.Companion.DETAIL_RESPONSE
import com.bhardwaj.passkey.utils.Constants.Companion.DETAIL_TITLE
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    private val appContext: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    val detailTitle = savedStateHandle.getStateFlow(DETAIL_TITLE, "")
    val detailResponse = savedStateHandle.getStateFlow(DETAIL_RESPONSE, "")
    val bottomSheetHeading = savedStateHandle.getStateFlow(BOTTOM_SHEET_HEADING, "")
    val previewId = savedStateHandle.get<Long>("previewId") ?: -1

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    val details: StateFlow<List<Details>> = repository.getDetailsByPreviewId(previewId = previewId)
        .combine(searchText) { details, text ->
            if (text.isBlank()) {
                details
            } else {
                details.filter {
                    it.question.contains(
                        text,
                        ignoreCase = true
                    ) or it.answer.contains(text, ignoreCase = true)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var isSheetOpen by mutableStateOf(false)
        private set

    var isAlertOpen by mutableStateOf(false)
        private set

    var detail by mutableStateOf<Details?>(null)
        private set

    private var deletedDetail: Details? = null

    fun onEvent(event: DetailEvents) {
        when (event) {
            DetailEvents.OnAddDetailClick -> {
                savedStateHandle[BOTTOM_SHEET_HEADING] =
                    UiText.StringResource(R.string.add).asString(context = appContext)
                isSheetOpen = true
                _searchText.value = ""
            }

            DetailEvents.OnDismissBottomSheet -> {
                savedStateHandle[DETAIL_TITLE] = ""
                savedStateHandle[DETAIL_RESPONSE] = ""
                isSheetOpen = false
            }

            is DetailEvents.OnChangeClick -> {
                viewModelScope.launch {
                    _searchText.value = ""
                    repository.getDetailById(event.details.detailsId!!)?.let { detail ->
                        isSheetOpen = true
                        savedStateHandle[BOTTOM_SHEET_HEADING] =
                            UiText.StringResource(R.string.edit).asString(context = appContext)
                        savedStateHandle[DETAIL_TITLE] = event.details.question
                        savedStateHandle[DETAIL_RESPONSE] = event.details.answer
                        this@DetailViewModel.detail = detail
                    }
                }
            }

            is DetailEvents.OnTitleChange -> {
                savedStateHandle[DETAIL_TITLE] = event.newTitle
            }

            is DetailEvents.OnDescriptionChange -> {
                savedStateHandle[DETAIL_RESPONSE] = event.newDescription
            }

            DetailEvents.OnSaveClick -> {
                viewModelScope.launch {
                    if (detailTitle.value.isBlank() or detailResponse.value.isBlank()) {
                        isSheetOpen = false
                        savedStateHandle[DETAIL_TITLE] = ""
                        savedStateHandle[DETAIL_RESPONSE] = ""
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(R.string.enter_valid_title_n_response)
                                    .asString(context = appContext)
                            )
                        )
                        return@launch
                    }
                    if (previewId.toInt() == -1) {
                        isSheetOpen = false
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(R.string.something_went_wrong)
                                    .asString(context = appContext)
                            )
                        )
                        return@launch
                    }
                    val newDetail = Details(
                        previewId = previewId,
                        question = detailTitle.value.trim(),
                        answer = detailResponse.value.trim(),
                    )

                    detail?.let {
                        repository.upsertDetails(
                            it.copy(
                                previewId = previewId,
                                question = detailTitle.value,
                                answer = detailResponse.value,
                            )
                        )
                    } ?: repository.upsertDetails(newDetail)

                    detail = null
                    savedStateHandle[DETAIL_TITLE] = ""
                    savedStateHandle[DETAIL_RESPONSE] = ""
                    isSheetOpen = false
                }
            }

            is DetailEvents.OnLongPress -> {
                val clipboardManager =
                    appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData =
                    ClipData.newPlainText("Text Copied Successfully.", event.detailsDescription)
                clipboardManager.setPrimaryClip(clipData)
                sendUiEvents(
                    UiEvents.ShowSnackBar(
                        message = UiText.StringResource(R.string.copied)
                            .asString(context = appContext)
                    )
                )
            }

            is DetailEvents.OnSwipedLeft -> {
                viewModelScope.launch {
                    isAlertOpen = true
                    repository.deleteDetail(event.details)
                    deletedDetail = event.details
                }
            }

            DetailEvents.OnDismissAlertDialog -> {
                viewModelScope.launch {
                    isAlertOpen = false
                    deletedDetail?.let { repository.upsertDetails(it) }
                    deletedDetail = null
                }
            }

            DetailEvents.OnCancelClick -> {
                viewModelScope.launch {
                    isAlertOpen = false
                    deletedDetail?.let { repository.upsertDetails(it) }
                    deletedDetail = null
                }
            }

            is DetailEvents.OnDeleteClick -> {
                isAlertOpen = false
                deletedDetail = null
            }

            is DetailEvents.OnSearchTextUpdate -> {
                _searchText.value = event.newText
            }

            is DetailEvents.OnReorderDetails -> {
                viewModelScope.launch {
                    val newList = details.value.toMutableList().apply {
                        add(event.to.index, removeAt(event.from.index))
                    }

                    val updatedList = newList.mapIndexed { index, detail ->
                        detail.copy(sequence = index.toLong())
                    }

                    updatedList.forEach { detail ->
                        repository.updateDetailSequence(
                            detailId = detail.detailsId!!,
                            sequence = detail.sequence
                        )
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