package com.bhardwaj.passkey.domain.events

sealed interface OnBoardingEvents {
    data object OnBoardingComplete : OnBoardingEvents
}