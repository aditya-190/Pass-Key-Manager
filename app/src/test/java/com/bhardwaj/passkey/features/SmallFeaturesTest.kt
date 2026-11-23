package com.bhardwaj.passkey.features

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bhardwaj.passkey.data.datastore.PreferencesRepositoryImpl
import com.bhardwaj.passkey.domain.model.PasswordPolicy
import com.bhardwaj.passkey.domain.model.PendingShortcut
import com.bhardwaj.passkey.domain.model.ShortcutAction
import com.bhardwaj.passkey.domain.model.ThemeMode
import com.bhardwaj.passkey.testing.testDataStore
import com.bhardwaj.passkey.utils.PassphraseGenerator
import java.util.Random
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 6.0.0c: theme choice, passphrases and icon shortcuts (the domain parts; ViewModels have their own tests). */
class SmallFeaturesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `the theme choice is saved, and an unknown stored value falls back to the system setting`() = runTest {
        val store = testDataStore(tmp.root)
        val prefs = PreferencesRepositoryImpl(store)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.first())
        store.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA_FROM_A_NEWER_VERSION" }
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode.first())
    }

    @Test
    fun `the word list is large, unique and plain`() {
        val words = PassphraseGenerator.words
        assertTrue("at least 1,296 words (10.3 bits each), was ${words.size}", words.size >= 1_296)
        assertEquals("no duplicates", words.size, words.toSet().size)
        assertTrue(words.all { it.matches(Regex("[a-z]{3,9}")) })
    }

    @Test
    fun `a passphrase has the chosen number of words, joined by hyphens`() {
        val plain = PassphraseGenerator.generate(words = 5, capitalize = false, withDigit = false, random = Random(1))
        val parts = plain.split("-")
        assertEquals(5, parts.size)
        assertTrue(parts.all { it in PassphraseGenerator.words })

        val strong = PassphraseGenerator.generate(words = 4, capitalize = true, withDigit = true, random = Random(2))
        val strongParts = strong.split("-")
        assertEquals(4, strongParts.size)
        assertTrue("every word capitalised", strongParts.all { it.first().isUpperCase() })
        assertEquals("exactly one digit", 1, strong.count { it.isDigit() })
    }

    @Test
    fun `the word count stays within range`() {
        assertEquals(PasswordPolicy.MAX_WORDS, PasswordPolicy().withWords(99).words)
        assertEquals(PasswordPolicy.MIN_WORDS, PasswordPolicy().withWords(0).words)
        assertEquals(6, PasswordPolicy().withWords(6).words)
    }

    @Test
    fun `only our own shortcut actions are recognised`() {
        assertEquals(ShortcutAction.ADD_ENTRY, ShortcutAction.fromIntentAction("com.bhardwaj.passkey.action.ADD_ENTRY"))
        assertEquals(ShortcutAction.SEARCH, ShortcutAction.fromIntentAction("com.bhardwaj.passkey.action.SEARCH"))
        assertNull(ShortcutAction.fromIntentAction("android.intent.action.MAIN"))
        assertNull(ShortcutAction.fromIntentAction(null))
    }

    @Test
    fun `a shortcut runs once, and a newer one replaces an older one`() {
        val pending = PendingShortcut()
        pending.offer(ShortcutAction.ADD_ENTRY)
        pending.offer(ShortcutAction.SEARCH)
        assertEquals(ShortcutAction.SEARCH, pending.take())
        assertNull(pending.take())
    }
}
