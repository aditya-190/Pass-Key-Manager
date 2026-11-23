package com.bhardwaj.passkey.presentation.screens.preview_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.domain.model.Category
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.PasswordHistoryEntry
import com.bhardwaj.passkey.domain.model.PendingShortcut
import com.bhardwaj.passkey.domain.model.Preview
import com.bhardwaj.passkey.domain.model.ShortcutAction
import com.bhardwaj.passkey.domain.model.TotpEntry
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.presentation.navigation.NavRoute
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_CATEGORY_NAME
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    private val savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsTracker,
    pendingShortcut: PendingShortcut
) : ViewModel() {
    // BUFFERED: with lifecycle-aware collection a backgrounded screen has no collector, and a rendezvous
    // channel would suspend whoever sent the effect.
    private val _effects = Channel<PreviewEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Survives process death; the rest is screen-local. fromNameOrOther, never valueOf, on a saved string. */
    private val category = savedStateHandle.getStateFlow(PREVIEW_CATEGORY_NAME, Category.BANKS.name)
    private val query = MutableStateFlow("")
    private val transient = MutableStateFlow(Transient())

    private data class Transient(val editor: PreviewState.Editor? = null, val pendingDelete: Preview? = null)

    /** Held for the Undo snackbar only. */
    /** Everything a delete removes, held for the Undo snackbar only. */
    private data class Deleted(
        val preview: Preview,
        val details: List<Detail>,
        val authenticators: List<TotpEntry>,
        val history: Map<Long, List<PasswordHistoryEntry>>
    )

    private var lastDeleted: Deleted? = null

    /**
     * Derived from independent sources rather than accumulated: feeding the public state back into itself
     * would be a loop that only ends because StateFlow drops equal values.
     */
    /**
     * Field names per entry, for search (6.0.0). Only the names: values are dropped here, so a search can never
     * match (or reveal the presence of) a password.
     */
    private val fieldNames = repository.getDetails().map { details ->
        details.groupBy({ it.previewId }, { it.question })
    }

    val state: StateFlow<PreviewState> = combine(
        category,
        query,
        transient,
        category.flatMapLatest { repository.getPreviewsByCategory(Category.fromNameOrOther(it)) },
        fieldNames
    ) { categoryName, currentQuery, ui, rows, names ->
        PreviewState(
            category = Category.fromNameOrOther(categoryName),
            query = currentQuery,
            items = rows
                .searchedBy(currentQuery.trim(), names)
                .filterNot { it.id == ui.pendingDelete?.id },
            isLoading = false,
            editor = ui.editor,
            pendingDelete = ui.pendingDelete
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PreviewState())

    init {
        // An icon shortcut (6.0.0) runs here, after the vault is open, and only once, including one that arrives
        // while Home is already showing.
        viewModelScope.launch {
            pendingShortcut.requests.filterNotNull().collect {
                when (pendingShortcut.take()) {
                    ShortcutAction.ADD_ENTRY -> openEditor(PreviewState.Editor())
                    ShortcutAction.SEARCH -> send(PreviewEffect.FocusSearch)
                    null -> Unit
                }
            }
        }
    }

    /** Name matches first, then entries that only have a matching field name; each group keeps its order. */
    private fun List<Preview>.searchedBy(query: String, names: Map<Long, List<String>>): List<Preview> {
        if (query.isEmpty()) return this
        val (byName, rest) = partition { it.heading.contains(query, ignoreCase = true) }
        return byName +
            rest.filter { preview -> names[preview.id].orEmpty().any { it.contains(query, ignoreCase = true) } }
    }

    fun onIntent(intent: PreviewIntent) {
        when (intent) {
            is PreviewIntent.CategorySelected -> if (category.value != intent.category.name) {
                savedStateHandle[PREVIEW_CATEGORY_NAME] = intent.category.name
                query.value = ""
            }

            is PreviewIntent.QueryChanged -> query.value = intent.query

            PreviewIntent.AddClicked -> openEditor(PreviewState.Editor())
            is PreviewIntent.EditClicked ->
                openEditor(PreviewState.Editor(heading = intent.preview.heading, editingId = intent.preview.id))

            is PreviewIntent.HeadingChanged ->
                transient.update { it.copy(editor = it.editor?.copy(heading = intent.heading, error = null)) }

            PreviewIntent.EditorDismissed -> transient.update { it.copy(editor = null) }
            PreviewIntent.SaveClicked -> save()

            // Nothing is written until the user confirms. Deleting on swipe and re-inserting on Cancel lost the
            // entry if the app died with the dialog open.
            is PreviewIntent.SwipedToDelete -> transient.update { it.copy(pendingDelete = intent.preview) }
            PreviewIntent.DeleteCancelled -> transient.update { it.copy(pendingDelete = null) }
            PreviewIntent.DeleteConfirmed -> deletePending()
            PreviewIntent.UndoDelete -> undoDelete()

            is PreviewIntent.Moved -> move(intent.fromIndex, intent.toIndex)

            is PreviewIntent.ItemClicked -> {
                query.value = ""
                send(PreviewEffect.Navigate(NavRoute.Details(intent.previewId)))
            }

            is PreviewIntent.ItemLongPressed -> {
                // A heading is not a secret: not marked sensitive, not auto-cleared.
                send(PreviewEffect.CopyToClipboard(intent.heading, isSensitive = false))
                analytics.log(AppEvents.valueCopied(sensitive = false))
            }

            PreviewIntent.SettingsClicked -> {
                query.value = ""
                send(PreviewEffect.Navigate(NavRoute.Settings()))
            }
        }
    }

    private fun openEditor(editor: PreviewState.Editor) {
        query.value = ""
        transient.update { it.copy(editor = editor) }
    }

    private fun rejectSave(message: Int) = transient.update {
        it.copy(editor = it.editor?.copy(error = UiText.StringResource(message), isSaving = false))
    }

    private fun save() {
        val editor = transient.value.editor ?: return
        if (editor.isSaving) return
        val heading = editor.heading.trim()
        if (heading.isEmpty()) return rejectSave(R.string.enter_valid_heading)
        transient.update { it.copy(editor = it.editor?.copy(isSaving = true)) }
        viewModelScope.launch {
            val currentCategory = Category.fromNameOrOther(category.value)
            val existing = repository.getPreviewByHeading(heading, currentCategory)
            // While editing, the lookup finds the row being edited; that is not a clash.
            if (existing != null && existing.id != editor.editingId) return@launch rejectSave(R.string.heading_exists)

            // An entry deleted while its sheet was open is saved as new rather than dropping what was typed.
            val editing = editor.editingId?.let { repository.getPreviewById(it) }
            if (editing != null) {
                repository.updatePreview(editing.copy(heading = heading, category = currentCategory))
            } else {
                repository.createPreview(heading = heading, category = currentCategory)
                analytics.log(AppEvents.entryCreated(currentCategory))
            }
            transient.update { it.copy(editor = null) }
        }
    }

    private fun deletePending() {
        val target = transient.value.pendingDelete ?: return
        // Cleared before the write: the dialog closes at once, and a second tap finds nothing to delete. A
        // second delete used to replace the Undo copy with one that had no fields left.
        transient.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val details = repository.getDetailsByPreviewId(target.id).first()
            // Snapshot what the database cascades away (authenticators with the entry, history with each field),
            // so Undo can put all of it back: a lost authenticator can lock the user out of the account.
            val snapshot = Deleted(
                preview = target,
                details = details,
                authenticators = repository.getTotpByPreviewId(target.id).first(),
                history = details.associate { it.id to repository.getHistoryForDetail(it.id).first() }
            )
            // No foreign-key cascade for fields: they go in the same transaction as their entry.
            repository.runInTransaction {
                repository.deleteDetailsByPreviewId(target.id)
                repository.deletePreview(target)
            }
            lastDeleted = snapshot
            send(PreviewEffect.ShowUndo(UiText.StringResource(R.string.entry_deleted)))
        }
    }

    private fun undoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            // Same ids as before the delete, so fields, codes and history point at the restored entry.
            repository.runInTransaction {
                repository.updatePreview(deleted.preview)
                deleted.details.forEach { repository.updateDetail(it) }
                repository.restoreCascaded(deleted.authenticators, deleted.history)
            }
        }
    }

    private fun move(fromIndex: Int, toIndex: Int) {
        val current = state.value.items
        // Only a full, unfiltered list may be renumbered; the UI offers drag only then.
        if (state.value.query.isNotBlank() || state.value.pendingDelete != null) return
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val reordered = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch {
            // One transaction: an interrupted reorder used to leave the list partly renumbered.
            repository.runInTransaction {
                reordered.forEachIndexed { index, preview ->
                    repository.updatePreviewSequence(preview.id, index.toLong())
                }
            }
        }
    }

    private fun send(effect: PreviewEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
