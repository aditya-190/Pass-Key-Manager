package com.bhardwaj.passkey.presentation.views.events

sealed interface SplashEvents {
    data object OnLoadingComplete : SplashEvents
}