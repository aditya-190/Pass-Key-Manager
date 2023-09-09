package com.bhardwaj.passkey.presentation.views.events

sealed interface SecurityEvents {
    data object OnAuthenticationSucceeded : SecurityEvents
    data object OnAuthenticationError : SecurityEvents
    data object AddPasswordToTheDeviceFirst : SecurityEvents
}