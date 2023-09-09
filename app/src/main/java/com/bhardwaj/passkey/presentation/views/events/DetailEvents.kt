package com.bhardwaj.passkey.presentation.views.events

import com.bhardwaj.passkey.data.local.entity.Details

sealed interface DetailEvents {
    // Dialogs.
    data class OnTitleChange(val newTitle: String) : DetailEvents
    data class OnDescriptionChange(val newDescription: String) : DetailEvents
    data object OnSaveClick : DetailEvents
    data object OnCancelClick : DetailEvents
    data object OnDeleteClick : DetailEvents
    data object OnDismissBottomSheet : DetailEvents
    data object OnDismissAlertDialog : DetailEvents

    // Single Item.
    data class OnLongPress(val detailsDescription: String) : DetailEvents
    data class OnReorderDetails(val details: Details) : DetailEvents
    data class OnChangeClick(val details: Details) : DetailEvents
    data class OnSwipedLeft(val details: Details) : DetailEvents

    // Floating Action Button
    data object OnAddDetailClick : DetailEvents

    // Search
    data class OnSearchTextUpdate(val newText: String) : DetailEvents
}