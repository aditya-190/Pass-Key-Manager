package com.bhardwaj.passkey.analytics

import com.bhardwaj.passkey.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Analytics must never carry vault content. Every param is an enum name, a boolean or a coarse bucket,
 * so these tests pin the bucket edges and the screen-name sanitising.
 */
class AppEventsTest {
    @Test
    fun `password length is bucketed, never sent raw`() {
        assertEquals("1-7", AppEvents.lengthBucket(4))
        assertEquals("1-7", AppEvents.lengthBucket(7))
        assertEquals("8-11", AppEvents.lengthBucket(8))
        assertEquals("12-15", AppEvents.lengthBucket(12))
        assertEquals("16-23", AppEvents.lengthBucket(16))
        assertEquals("24+", AppEvents.lengthBucket(24))
        assertEquals("24+", AppEvents.lengthBucket(10_000))
        assertEquals("0", AppEvents.lengthBucket(0))
        assertEquals("0", AppEvents.lengthBucket(-5))
    }

    @Test
    fun `entry counts are bucketed`() {
        assertEquals("0", AppEvents.countBucket(0))
        assertEquals("1-9", AppEvents.countBucket(1))
        assertEquals("10-49", AppEvents.countBucket(10))
        assertEquals("50-199", AppEvents.countBucket(199))
        assertEquals("200+", AppEvents.countBucket(200))
        assertEquals("0", AppEvents.countBucket(-1))
    }

    @Test
    fun `safety score is bucketed and clamped`() {
        assertEquals("0-49", AppEvents.scoreBucket(-10))
        assertEquals("0-49", AppEvents.scoreBucket(49))
        assertEquals("50-79", AppEvents.scoreBucket(50))
        assertEquals("80-100", AppEvents.scoreBucket(80))
        assertEquals("80-100", AppEvents.scoreBucket(250))
    }

    @Test
    fun `screen view drops query arguments so ids never leave the device`() {
        val event = AppEvents.screenView("details_page?previewId=42")
        assertEquals("details_page", event.params[AppParams.SCREEN_NAME])
        assertFalse(event.params.values.any { it.toString().contains("42") })
    }

    @Test
    fun `entry created carries only the category enum`() {
        assertEquals(
            mapOf(AppParams.CATEGORY to "mails"),
            AppEvents.entryCreated(Category.MAILS).params
        )
    }

    @Test
    fun `language tag is truncated to a sane length`() {
        val tag = AppEvents.languageChanged("x".repeat(500)).params[AppParams.LANGUAGE] as String
        assertEquals(EventValidator.MAX_VALUE_LENGTH, tag.length)
    }
}
