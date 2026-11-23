package com.bhardwaj.passkey.presentation.screens.preview_screen

import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.utils.UiText

/** Everything the vault home shows, in one immutable value. */
data class PreviewState(
    val category: Category = Category.BANKS,
    val query: String = "",
    val items: List<Preview> = emptyList(),
    /** `stateIn` emits its initial value first; without this the empty illustration flashed on every visit. */
    val isLoading: Boolean = true,
    /** Non-null while the add/edit sheet is open. */
    val editor: Editor? = null,
    /** Non-null while the delete confirmation is up. The row is hidden from [items] but not deleted yet. */
    val pendingDelete: Preview? = null
) {
    data class Editor(
        val heading: String = "",
        val editingId: Long? = null,
        /** Shown inside the sheet: a rejected save keeps the sheet and what was typed. */
        val error: UiText? = null,
        /** A save is being written; further Save taps are ignored so a double tap cannot create a duplicate. */
        val isSaving: Boolean = false
    ) {
        val isEdit: Boolean get() = editingId != null
    }

    /** A search with no hits is not an empty vault. */
    val showEmptyState: Boolean get() = !isLoading && items.isEmpty() && query.isBlank()
}

sealed interface PreviewIntent {
    data class CategorySelected(val category: Category) : PreviewIntent
    data class QueryChanged(val query: String) : PreviewIntent

    data object AddClicked : PreviewIntent
    data class EditClicked(val preview: Preview) : PreviewIntent
    data class HeadingChanged(val heading: String) : PreviewIntent
    data object SaveClicked : PreviewIntent
    data object EditorDismissed : PreviewIntent

    data class SwipedToDelete(val preview: Preview) : PreviewIntent
    data object DeleteConfirmed : PreviewIntent
    data object DeleteCancelled : PreviewIntent
    data object UndoDelete : PreviewIntent

    /** Plain indices into [PreviewState.items]; the old event carried a Compose LazyListItemInfo. */
    data class Moved(val fromIndex: Int, val toIndex: Int) : PreviewIntent

    data class ItemClicked(val previewId: Long) : PreviewIntent
    data class ItemLongPressed(val heading: String) : PreviewIntent
    data object SettingsClicked : PreviewIntent
}

sealed interface PreviewEffect {
    /** The Search shortcut: put the cursor in the search field. */
    data object FocusSearch : PreviewEffect

    data class Navigate(val route: NavRoute) : PreviewEffect
    data class ShowUndo(val message: UiText) : PreviewEffect
    data class CopyToClipboard(val value: String, val isSensitive: Boolean) : PreviewEffect
}
