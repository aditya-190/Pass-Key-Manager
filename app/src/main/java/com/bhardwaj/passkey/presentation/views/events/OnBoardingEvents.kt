package com.bhardwaj.passkey.presentation.views.events

sealed interface OnBoardingEvents {
    data object OnBoardingComplete : OnBoardingEvents
}