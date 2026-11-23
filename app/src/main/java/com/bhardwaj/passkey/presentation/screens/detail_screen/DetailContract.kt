package com.bhardwaj.passkey.presentation.screens.detail_screen

import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.utils.UiText

/** Everything an entry's field list shows, in one immutable value. */
data class DetailState(
    val query: String = "",
    val items: List<Detail> = emptyList(),
    val isLoading: Boolean = true,
    /** Non-null while the add/edit sheet is open. Never saved to SavedStateHandle: it can hold a secret. */
    val editor: Editor? = null,
    /** Non-null while the delete confirmation is up; the row is hidden but not deleted yet. */
    val pendingDelete: Detail? = null,
    val policy: PasswordPolicy = PasswordPolicy(),
    val isPolicySheetOpen: Boolean = false
) {
    data class Editor(
        val question: String = "",
        val answer: String = "",
        val editingId: Long? = null,
        /** Drives the keyboard (a password keyboard never learns what is typed) and is stored with the field. */
        val isSecret: Boolean = false,
        /** Until the user sets the switch, it follows the title's keywords. */
        val secretSetByUser: Boolean = false,
        /** Shown inside the sheet: a rejected save keeps the sheet and what was typed. */
        val error: UiText? = null,
        /** A save is being written; further Save taps are ignored so a double tap cannot create a duplicate. */
        val isSaving: Boolean = false
    ) {
        val isEdit: Boolean get() = editingId != null
    }

    val showEmptyState: Boolean get() = !isLoading && items.isEmpty() && query.isBlank()
}

sealed interface DetailIntent {
    data class QueryChanged(val query: String) : DetailIntent

    data object AddClicked : DetailIntent
    data class EditClicked(val detail: Detail) : DetailIntent
    data class QuestionChanged(val question: String) : DetailIntent
    data class AnswerChanged(val answer: String) : DetailIntent
    data class SecretToggled(val isSecret: Boolean) : DetailIntent
    data object SaveClicked : DetailIntent
    data object EditorDismissed : DetailIntent

    data class SwipedToDelete(val detail: Detail) : DetailIntent
    data object DeleteConfirmed : DetailIntent
    data object DeleteCancelled : DetailIntent
    data object UndoDelete : DetailIntent

    /** Plain indices into [DetailState.items]. */
    data class Moved(val fromIndex: Int, val toIndex: Int) : DetailIntent
    data class LongPressed(val value: String) : DetailIntent

    data object PolicyClicked : DetailIntent
    data object PolicyDismissed : DetailIntent
    data class LengthChanged(val length: Int) : DetailIntent

    /** Typed, unlike the old event that dispatched on "Upper", "Lower", "Number" and "Special". */
    data class CharacterClassToggled(val characterClass: PasswordCharacterClass, val enabled: Boolean) : DetailIntent
    data object GenerateClicked : DetailIntent
}

sealed interface DetailEffect {
    data class ShowUndo(val message: UiText) : DetailEffect
    data class CopyToClipboard(val value: String, val isSensitive: Boolean) : DetailEffect
}
