package com.bhardwaj.passkey.utils

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 6.0.0b attack-pass #3: a backup's date follows the app's chosen language, not the phone's. */
class FormattedDateTest {
    private val at = 1_800_000_000_000L

    @Test
    fun `dates are formatted in the locale they are shown in`() {
        assertEquals(
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.GERMAN).format(Date(at)),
            formatDate(at, Locale.GERMAN)
        )
        assertNotEquals(formatDate(at, Locale.ENGLISH), formatDate(at, Locale.forLanguageTag("hi")))
    }
}
