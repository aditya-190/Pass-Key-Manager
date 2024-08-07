package com.bhardwaj.passkey.domain.events

sealed interface SecurityEvents {
    data object OnAuthenticationSucceeded : SecurityEvents
    data object OnAuthenticationError : SecurityEvents
    data object AddPasswordToTheDeviceFirst : SecurityEvents
}