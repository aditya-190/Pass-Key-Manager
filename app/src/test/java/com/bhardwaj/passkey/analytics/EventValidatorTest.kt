package com.bhardwaj.passkey.analytics

import com.bhardwaj.passkey.utils.Categories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventValidatorTest {
    @Test
    fun `every app event factory produces a valid event`() {
        val events = listOf(
            AppEvents.screenView("preview_page"),
            AppEvents.consentUpdated(analytics = true, crash = false),
            AppEvents.onboardingPageView(1),
            AppEvents.onboardingSkipped(2),
            AppEvents.onboardingComplete(),
            AppEvents.entryCreated(Categories.BANKS),
            AppEvents.fieldSaved(isEdit = true),
            AppEvents.passwordGenerated(length = 16),
            AppEvents.valueCopied(sensitive = true),
            AppEvents.vaultUnlocked(),
            AppEvents.unlockFailed(),
            AppEvents.backupExported(entryCount = 42),
            AppEvents.backupImported(success = false, entryCount = 0),
            AppEvents.safetyCheckViewed(score = 73),
            AppEvents.languageChanged("pt-BR")
        )
        events.forEach {
            assertTrue("${it.name}: ${EventValidator.validate(it)}", EventValidator.validate(it).isEmpty())
        }
    }

    @Test
    fun `every declared event and param name is within Firebase limits`() {
        val names = (AppEventNames::class.java.declaredFields + AppParams::class.java.declaredFields)
            .filter { it.type == String::class.java }
            .map { it.get(null) as String }
        assertTrue(names.isNotEmpty())
        names.forEach { assertTrue(it, EventValidator.validate(AnalyticsEvent(it)).isEmpty()) }
    }

    @Test
    fun `rejects long names, bad case and reserved prefixes`() {
        assertTrue(EventValidator.validate(AnalyticsEvent("a".repeat(41))).isNotEmpty())
        assertTrue(EventValidator.validate(AnalyticsEvent("ScreenView")).isNotEmpty())
        assertTrue(EventValidator.validate(AnalyticsEvent("firebase_custom")).isNotEmpty())
        assertTrue(EventValidator.validate(AnalyticsEvent("ok", mapOf("Bad-Key" to "x"))).isNotEmpty())
    }

    @Test
    fun `rejects too many params, long values and unsupported types`() {
        val many = (1..26).associate { "p$it" to it }
        assertTrue(EventValidator.validate(AnalyticsEvent("ok", many)).isNotEmpty())
        assertTrue(EventValidator.validate(AnalyticsEvent("ok", mapOf("v" to "x".repeat(101)))).isNotEmpty())
        assertTrue(EventValidator.validate(AnalyticsEvent("ok", mapOf("v" to listOf(1)))).isNotEmpty())
    }

    @Test
    fun `accepts boundary values`() {
        val params = (1..25).associate { "p$it" to "x".repeat(100) }
        assertEquals(emptyList<String>(), EventValidator.validate(AnalyticsEvent("a".repeat(40), params)))
    }
}
