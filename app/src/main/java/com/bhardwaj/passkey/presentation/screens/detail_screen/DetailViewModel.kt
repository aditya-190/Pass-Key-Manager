package com.bhardwaj.passkey.presentation.screens.detail_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.AppEvents
import com.bhardwaj.passkey.domain.model.Detail
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.domain.model.TotpEntry
import com.bhardwaj.passkey.domain.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.totp.Base32
import com.bhardwaj.passkey.domain.totp.OtpAuthUri
import com.bhardwaj.passkey.domain.totp.TotpConfig
import com.bhardwaj.passkey.utils.PasswordAnalyzer
import com.bhardwaj.passkey.utils.PasswordGenerator
import com.bhardwaj.passkey.utils.SecretKeywords
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    savedStateHandle: SavedStateHandle,
    private val analytics: AnalyticsTracker,
    private val secretKeywords: SecretKeywords
) : ViewModel() {
    private val _effects = Channel<DetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** From the typed NavRoute.Details argument, which always carries it. */
    private val previewId: Long =
        checkNotNull(savedStateHandle.get<Long>("previewId")) { "Details opened without an entry" }

    private val query = MutableStateFlow("")

    /**
     * Deliberately NOT in SavedStateHandle: that bundle is written to disk outside the encrypted database for
     * task persistence, so an in-progress secret (including a freshly generated password) would be stored in
     * cleartext. Losing a half-typed field to process death is the right trade.
     */
    private val local = MutableStateFlow(Local())

    private data class Local(
        val editor: DetailState.Editor? = null,
        val pendingDelete: Detail? = null,
        val policy: PasswordPolicy = PasswordPolicy(),
        val isPolicySheetOpen: Boolean = false,
        val totpEditor: DetailState.TotpEditor? = null,
        val pendingTotpDelete: TotpEntry? = null,
        val history: DetailState.History? = null
    )

    private var lastDeleted: Detail? = null

    val state: StateFlow<DetailState> = combine(
        repository.getDetailsByPreviewId(previewId),
        repository.getTotpByPreviewId(previewId),
        query,
        local
    ) { rows, authenticators, currentQuery, ui ->
        DetailState(
            query = currentQuery,
            items = rows
                .filter {
                    currentQuery.isBlank() ||
                        it.question.contains(currentQuery, ignoreCase = true) ||
                        it.answer.contains(currentQuery, ignoreCase = true)
                }
                .filterNot { it.id == ui.pendingDelete?.id },
            isLoading = false,
            editor = ui.editor,
            pendingDelete = ui.pendingDelete,
            policy = ui.policy,
            isPolicySheetOpen = ui.isPolicySheetOpen,
            authenticators = authenticators.filterNot { it.id == ui.pendingTotpDelete?.id },
            totpEditor = ui.totpEditor,
            pendingTotpDelete = ui.pendingTotpDelete,
            history = ui.history
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DetailState())

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            is DetailIntent.QueryChanged -> query.value = intent.query

            DetailIntent.AddClicked -> openEditor(DetailState.Editor())
            is DetailIntent.EditClicked -> openEditor(
                DetailState.Editor(
                    question = intent.detail.question,
                    answer = intent.detail.answer,
                    editingId = intent.detail.id,
                    // A stored choice is the user's; only legacy rows fall back to the title.
                    isSecret = intent.detail.isSecret || looksSecret(intent.detail.question),
                    secretSetByUser = true
                )
            )

            is DetailIntent.QuestionChanged -> editEditor {
                it.copy(
                    question = intent.question,
                    isSecret = if (it.secretSetByUser) it.isSecret else looksSecret(intent.question),
                    error = null
                )
            }

            is DetailIntent.AnswerChanged -> editEditor { it.copy(answer = intent.answer, error = null) }
            is DetailIntent.SecretToggled -> editEditor { it.copy(isSecret = intent.isSecret, secretSetByUser = true) }
            DetailIntent.EditorDismissed -> local.update { it.copy(editor = null) }
            DetailIntent.SaveClicked -> save()

            // Nothing is written until the user confirms; a delete can be undone from the snackbar.
            is DetailIntent.SwipedToDelete -> local.update { it.copy(pendingDelete = intent.detail) }
            DetailIntent.DeleteCancelled -> local.update { it.copy(pendingDelete = null) }
            DetailIntent.DeleteConfirmed -> deletePending()
            DetailIntent.UndoDelete -> lastDeleted?.let { detail ->
                lastDeleted = null
                viewModelScope.launch { repository.updateDetail(detail) }
            }

            is DetailIntent.Moved -> move(intent.fromIndex, intent.toIndex)

            is DetailIntent.LongPressed -> {
                send(DetailEffect.CopyToClipboard(intent.value, isSensitive = true))
                analytics.log(AppEvents.valueCopied(sensitive = true))
            }

            DetailIntent.PolicyClicked -> local.update { it.copy(isPolicySheetOpen = true) }
            DetailIntent.PolicyDismissed -> local.update { it.copy(isPolicySheetOpen = false) }
            is DetailIntent.LengthChanged -> local.update { it.copy(policy = it.policy.withLength(intent.length)) }
            is DetailIntent.CharacterClassToggled ->
                local.update { it.copy(policy = it.policy.toggle(intent.characterClass, intent.enabled)) }

            DetailIntent.GenerateClicked -> generate()
            else -> onAuthenticatorOrHistoryIntent(intent)
        }
    }

    private fun onAuthenticatorOrHistoryIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.AddAuthenticatorClicked -> local.update { it.copy(totpEditor = DetailState.TotpEditor()) }
            is DetailIntent.AuthenticatorInputChanged ->
                local.update { it.copy(totpEditor = it.totpEditor?.copy(input = intent.input, isInvalid = false)) }
            DetailIntent.AuthenticatorEditorDismissed -> local.update { it.copy(totpEditor = null) }
            DetailIntent.AuthenticatorSaveClicked -> addAuthenticator()
            is DetailIntent.AuthenticatorDeleteClicked -> local.update { it.copy(pendingTotpDelete = intent.entry) }
            DetailIntent.AuthenticatorDeleteCancelled -> local.update { it.copy(pendingTotpDelete = null) }
            DetailIntent.AuthenticatorDeleteConfirmed -> {
                val target = local.value.pendingTotpDelete ?: return
                local.update { it.copy(pendingTotpDelete = null) }
                viewModelScope.launch { repository.deleteTotp(target) }
            }

            is DetailIntent.AuthenticatorCodeCopied -> send(
                DetailEffect.CopyToClipboard(intent.code, isSensitive = true)
            )
            is DetailIntent.HistoryClicked -> viewModelScope.launch {
                val entries = repository.getHistoryForDetail(intent.detail.id).first()
                local.update { it.copy(history = DetailState.History(intent.detail, entries)) }
            }

            DetailIntent.HistoryDismissed -> local.update { it.copy(history = null) }
            else -> Unit
        }
    }

    /** Accepts the otpauth:// link behind a QR code or the Base32 secret printed beside it. */
    private fun addAuthenticator() {
        val input = local.value.totpEditor?.input?.trim().orEmpty()
        val config = OtpAuthUri.parse(input) ?: Base32.decode(input)?.let { TotpConfig(secret = it) }
        if (config == null) {
            local.update { it.copy(totpEditor = it.totpEditor?.copy(isInvalid = true)) }
            return
        }
        local.update { it.copy(totpEditor = null) }
        viewModelScope.launch {
            val label = config.issuer?.takeIf { it.isNotBlank() }
                ?: config.account?.takeIf { it.isNotBlank() }
                ?: DEFAULT_AUTHENTICATOR_LABEL
            repository.createTotp(previewId = previewId, label = label, config = config)
        }
    }

    private fun looksSecret(title: String) = PasswordAnalyzer.looksSecret(title, secretKeywords.all())

    private fun openEditor(editor: DetailState.Editor) {
        query.value = ""
        local.update { it.copy(editor = editor) }
    }

    private inline fun editEditor(crossinline transform: (DetailState.Editor) -> DetailState.Editor) =
        local.update { it.copy(editor = it.editor?.let(transform)) }

    private fun save() {
        val editor = local.value.editor ?: return
        // Trimmed on both paths: a trailing space in a stored password fails silently wherever it is pasted.
        val question = editor.question.trim()
        val answer = editor.answer.trim()
        if (editor.isSaving) return
        if (question.isEmpty() || answer.isEmpty()) {
            return editEditor { it.copy(error = UiText.StringResource(R.string.enter_valid_title_n_response)) }
        }
        editEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            val existing = editor.editingId?.let { repository.getDetailById(it) }
            if (existing != null) {
                repository.updateDetail(existing.copy(question = question, answer = answer, isSecret = editor.isSecret))
            } else {
                repository.createDetail(previewId, question, answer, isSecret = editor.isSecret)
            }
            analytics.log(AppEvents.fieldSaved(isEdit = existing != null))
            local.update { it.copy(editor = null) }
        }
    }

    private fun deletePending() {
        val target = local.value.pendingDelete ?: return
        // Cleared before the write, so the dialog closes at once and a second tap does nothing.
        local.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            repository.deleteDetail(target)
            lastDeleted = target
            send(DetailEffect.ShowUndo(UiText.StringResource(R.string.field_deleted)))
        }
    }

    private fun move(fromIndex: Int, toIndex: Int) {
        val current = state.value
        if (current.query.isNotBlank() || current.pendingDelete != null) return
        if (fromIndex !in current.items.indices || toIndex !in current.items.indices) return
        val reordered = current.items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch {
            repository.runInTransaction {
                reordered.forEachIndexed { index, detail -> repository.updateDetailSequence(detail.id, index.toLong()) }
            }
        }
    }

    private fun generate() {
        val policy = local.value.policy
        val password = PasswordGenerator.generate(
            length = policy.length,
            includeUpper = policy.includeUppercase,
            includeLower = policy.includeLowercase,
            includeNumbers = policy.includeDigits,
            includeSpecial = policy.includeSymbols
        )
        // A generated value is unambiguously a secret.
        editEditor { it.copy(answer = password, isSecret = true, secretSetByUser = true, error = null) }
        analytics.log(AppEvents.passwordGenerated(length = password.length))
    }

    private fun send(effect: DetailEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /** Only reached by a hand-typed secret, which carries no issuer or account. */
        const val DEFAULT_AUTHENTICATOR_LABEL = "Authenticator"
    }
}
