package com.bhardwaj.passkey.domain.events

sealed interface SecurityEvents {
    data object OnAuthenticationSucceeded : SecurityEvents
    data object OnAuthenticationError : SecurityEvents
    data object AddPasswordToTheDeviceFirst : SecurityEvents

    /** The system prompt (or PIN screen) was just requested. */
    data object OnPromptStarted : SecurityEvents

    /** The lock screen's lifecycle reached ON_STOP. */
    data object OnStopped : SecurityEvents
}
