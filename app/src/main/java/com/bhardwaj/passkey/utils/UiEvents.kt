package com.bhardwaj.passkey.utils

sealed interface UiEvents {
    data object PopBackStack : UiEvents
    data class Navigate(val route: String) : UiEvents
    data class ShowSnackBar(
        val message: String,
        val action: String? = null
    ) : UiEvents
}