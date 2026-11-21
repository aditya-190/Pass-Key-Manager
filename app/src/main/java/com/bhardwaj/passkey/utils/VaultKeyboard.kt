package com.bhardwaj.passkey.utils

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Keyboard options for vault fields. For secrets, [KeyboardType.Password] tells the keyboard not to
 * learn, suggest or sync what is typed, so a password cannot reappear later as a word suggestion.
 */
fun vaultKeyboardOptions(isSecret: Boolean, imeAction: ImeAction): KeyboardOptions = if (isSecret) {
    KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction)
} else {
    KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = imeAction)
}
