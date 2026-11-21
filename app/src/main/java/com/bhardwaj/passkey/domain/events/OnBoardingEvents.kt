package com.bhardwaj.passkey.domain.events

sealed interface OnBoardingEvents {
    /** [page] is the 0-based pager index. */
    data class OnPageViewed(val page: Int) : OnBoardingEvents

    /** [page] is the 0-based pager index the user skipped from. */
    data class OnSkip(val page: Int) : OnBoardingEvents

    data object OnBoardingComplete : OnBoardingEvents
}
