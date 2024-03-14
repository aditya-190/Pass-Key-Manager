package com.bhardwaj.passkey.domain.events

sealed interface SplashEvents {
    data object OnLoadingComplete : SplashEvents
}