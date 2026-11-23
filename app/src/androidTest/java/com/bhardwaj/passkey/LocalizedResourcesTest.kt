package com.bhardwaj.passkey

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bhardwaj.passkey.presentation.screens.onboarding_screens.components.parseHighlightMarkup
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Resolves resources on a real device in every shipped locale: onboarding headings must parse to exactly
 * one highlight with no leftover markers (the old indexOf approach crashed on translations), and the
 * legal and consent texts must actually be translated rather than falling back to English.
 */
@RunWith(AndroidJUnit4::class)
class LocalizedResourcesTest {
    private val app: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val locales = listOf(
        "en", "hi", "de", "mr", "bn", "te", "ta", "gu", "zh", "es", "pt", "ru", "ja", "ko", "fr", "it", "ar"
    )

    private fun contextFor(tag: String): Context = app.createConfigurationContext(
        Configuration(app.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(tag))
        }
    )

    @Test
    fun onboardingHeadingsParseToOneHighlightInEveryLocale() {
        val headings = listOf(R.string.first_heading, R.string.second_heading, R.string.third_heading)
        locales.forEach { tag ->
            headings.forEach { id ->
                val parsed = parseHighlightMarkup(contextFor(tag).getString(id))
                assertWithMessage("$tag ${app.resources.getResourceEntryName(id)}").that(parsed.spans).hasSize(1)
                assertWithMessage(tag).that(parsed.text).doesNotContain("[[")
                assertWithMessage(tag).that(parsed.text).doesNotContain("]]")
            }
        }
    }

    @Test
    fun legalAndConsentTextIsTranslatedEverywhere() {
        val translated = listOf(
            R.string.about_studio_blurb,
            R.string.legal_privacy_analytics_body,
            R.string.legal_privacy_backup_body,
            R.string.legal_terms_safety_body,
            R.string.consent_title,
            R.string.consent_body,
            R.string.privacy_options
        )
        val english = contextFor("en")
        locales.filter { it != "en" }.forEach { tag ->
            translated.forEach { id ->
                assertWithMessage("$tag ${app.resources.getResourceEntryName(id)} fell back to English")
                    .that(contextFor(tag).getString(id))
                    .isNotEqualTo(english.getString(id))
            }
        }
    }

    @Test
    fun noShippedTextStillPromisesAutofill() {
        // Autofill arrives in 5.8.0. Until then no onboarding copy may advertise it.
        assertThat(contextFor("en").getString(R.string.third_heading)).doesNotContain("Autofill")
        assertThat(contextFor("en").getString(R.string.third_description)).doesNotContain("autofill")
    }
}
