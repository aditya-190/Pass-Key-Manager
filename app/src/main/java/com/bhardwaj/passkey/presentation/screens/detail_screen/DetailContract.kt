package com.bhardwaj.passkey.presentation.screens.detail_screen

import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.PasswordCharacterClass
import com.bhardwaj.passkey.domain.model.PasswordHistoryEntry
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.domain.model.TotpEntry
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
    val isPolicySheetOpen: Boolean = false,
    val authenticators: List<TotpEntry> = emptyList(),
    /** Non-null while the add-authenticator dialog is open. */
    val totpEditor: TotpEditor? = null,
    /** Non-null while removing an authenticator waits for confirmation: losing it can lock the user out. */
    val pendingTotpDelete: TotpEntry? = null,
    /** Non-null while the history sheet shows this secret's past values. */
    val history: History? = null
) {
    data class TotpEditor(val input: String = "", val isInvalid: Boolean = false)

    data class History(val detail: Detail, val entries: List<PasswordHistoryEntry>)

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
    data class PassphraseToggled(val enabled: Boolean) : DetailIntent
    data class WordsChanged(val words: Int) : DetailIntent

    /** Typed, unlike the old event that dispatched on "Upper", "Lower", "Number" and "Special". */
    data class CharacterClassToggled(val characterClass: PasswordCharacterClass, val enabled: Boolean) : DetailIntent
    data object GenerateClicked : DetailIntent

    data object AddAuthenticatorClicked : DetailIntent
    data class AuthenticatorInputChanged(val input: String) : DetailIntent
    data object AuthenticatorSaveClicked : DetailIntent
    data object AuthenticatorEditorDismissed : DetailIntent
    data class AuthenticatorDeleteClicked(val entry: TotpEntry) : DetailIntent
    data object AuthenticatorDeleteConfirmed : DetailIntent
    data object AuthenticatorDeleteCancelled : DetailIntent

    /** The code is generated in the card, where the per-second clock lives. */
    data class AuthenticatorCodeCopied(val code: String) : DetailIntent

    data class HistoryClicked(val detail: Detail) : DetailIntent
    data object HistoryDismissed : DetailIntent
}

sealed interface DetailEffect {
    data class ShowUndo(val message: UiText) : DetailEffect
    data class CopyToClipboard(val value: String, val isSensitive: Boolean) : DetailEffect
}
