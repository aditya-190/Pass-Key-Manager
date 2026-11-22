package com.bhardwaj.passkey.legal

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bhardwaj.passkey.analytics.AnalyticsTracker
import com.bhardwaj.passkey.analytics.BaselineEvents
import com.bhardwaj.passkey.config.AppConfig
import com.bhardwaj.passkey.config.ConfigKeys
import com.bhardwaj.passkey.config.ConfigProvider
import com.bhardwaj.passkey.core.SafeUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class LegalUiState(
    val document: LegalDocument,
    @StringRes val title: Int,
    val lastUpdated: LocalDate,
    val sections: List<LegalSection>,
    /** Hosted full version (https only); null hides the "Read online" button. */
    val hostedUrl: String?
)

sealed interface LegalIntent {
    data object OpenOnline : LegalIntent
}

sealed interface LegalEffect {
    data class OpenUrl(val url: String) : LegalEffect
}

/**
 * Terms or Privacy. [hostedUrl] comes from Remote Config (`terms_url` / `privacy_url`), resolved by the caller
 * with [hostedUrlFor]. The primary constructor is annotation-free (tests, manual DI:
 * `viewModel { LegalViewModel(texts.of(doc), LegalViewModel.hostedUrlFor(doc, config.current), analytics) }`).
 *
 * HILT: add `@HiltViewModel` on the class and this secondary constructor (imports: dagger.hilt.android.lifecycle.
 * HiltViewModel, javax.inject.Inject, androidx.lifecycle.SavedStateHandle), and `@Provides fun legalTexts() =
 * LegalTexts(...)` in the module. The nav argument [ARG_DOCUMENT] carries `LegalDocument.name`.
 * ```
 * @Inject constructor(
 *     handle: SavedStateHandle,
 *     texts: LegalTexts,
 *     config: ConfigProvider,
 *     analytics: AnalyticsTracker
 * ) : this(
 *     texts.of(documentOf(handle.get<String>(ARG_DOCUMENT))),
 *     hostedUrlFor(documentOf(handle.get<String>(ARG_DOCUMENT)), config.current),
 *     analytics
 * )
 * ```
 */
@HiltViewModel
class LegalViewModel(content: LegalContent, hostedUrl: String, private val analytics: AnalyticsTracker) : ViewModel() {
    @Inject
    constructor(
        handle: SavedStateHandle,
        texts: LegalTexts,
        config: ConfigProvider,
        analytics: AnalyticsTracker
    ) : this(
        texts.of(documentOf(handle.get<String>(ARG_DOCUMENT))),
        hostedUrlFor(documentOf(handle.get<String>(ARG_DOCUMENT)), config.current),
        analytics
    )

    private val _state =
        MutableStateFlow(
            LegalUiState(
                document = content.document,
                title = content.title,
                lastUpdated = content.lastUpdated,
                sections = content.sections,
                hostedUrl = hostedUrl.trim().takeIf { SafeUrl.isHttps(it) }
            )
        )
    val state: StateFlow<LegalUiState> = _state.asStateFlow()

    private val _effects = Channel<LegalEffect>(Channel.BUFFERED)
    val effects: Flow<LegalEffect> = _effects.receiveAsFlow()

    init {
        analytics.log(BaselineEvents.legalViewed(content.document.analyticsName))
    }

    fun onIntent(intent: LegalIntent) {
        when (intent) {
            LegalIntent.OpenOnline -> {
                val url = _state.value.hostedUrl ?: return
                analytics.log(BaselineEvents.legalOpenedOnline(_state.value.document.analyticsName))
                _effects.trySend(LegalEffect.OpenUrl(url))
            }
        }
    }

    companion object {
        /** Navigation / SavedStateHandle argument holding [LegalDocument.name]. */
        const val ARG_DOCUMENT = "document"

        /** Nav argument -> document (name or analytics name, any case); unknown or missing -> Privacy. */
        fun documentOf(arg: String?): LegalDocument = LegalDocument.entries.firstOrNull {
            it.name.equals(arg, ignoreCase = true) || it.analyticsName.equals(arg, ignoreCase = true)
        } ?: LegalDocument.PRIVACY

        /** Hosted URL from Remote Config for [document] (validated as https by the ViewModel). */
        fun hostedUrlFor(document: LegalDocument, config: AppConfig): String = config[
            when (document) {
                LegalDocument.TERMS -> ConfigKeys.TERMS_URL
                LegalDocument.PRIVACY -> ConfigKeys.PRIVACY_URL
            }
        ]
    }
}
