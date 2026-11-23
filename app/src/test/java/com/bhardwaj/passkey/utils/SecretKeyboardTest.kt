package com.bhardwaj.passkey.utils

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import org.junit.Assert.assertEquals
import org.junit.Test

class SecretKeyboardTest {
    @Test
    fun `secret fields ask the keyboard not to learn or suggest`() {
        val options = vaultKeyboardOptions(isSecret = true, imeAction = ImeAction.Done)
        assertEquals(KeyboardType.Password, options.keyboardType)
        assertEquals(false, options.autoCorrectEnabled)
        assertEquals(ImeAction.Done, options.imeAction)
    }

    @Test
    fun `ordinary fields keep a normal keyboard`() {
        val options = vaultKeyboardOptions(isSecret = false, imeAction = ImeAction.Next)
        assertEquals(KeyboardType.Text, options.keyboardType)
        assertEquals(ImeAction.Next, options.imeAction)
    }
}
