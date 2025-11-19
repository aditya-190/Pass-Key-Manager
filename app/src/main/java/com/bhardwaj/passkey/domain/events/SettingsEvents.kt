package com.bhardwaj.passkey.domain.events

import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.domain.models.Language

sealed interface SettingsEvents {
    data class OnLanguageChange(val newLanguage: Language) : SettingsEvents
    data class OnImportDataClick(val content: String) : SettingsEvents

    data class OnAnalysisItemClick(val detail: Details) : SettingsEvents
    data object OnLanguageClick : SettingsEvents
    data object OnDismissBottomSheet : SettingsEvents
    data object OnRateAppClick : SettingsEvents
    data object OnExportDataClick : SettingsEvents
    data object OnPrivacyClick : SettingsEvents
    data object OnTermsAndConditionClick : SettingsEvents
    data object OnAboutClick : SettingsEvents
    data object OnAnalyzePasswordsClick : SettingsEvents
    data object OnDismissAnalysisSheet : SettingsEvents
}