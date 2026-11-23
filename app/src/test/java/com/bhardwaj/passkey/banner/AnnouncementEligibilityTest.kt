package com.bhardwaj.passkey.banner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnouncementEligibilityTest {
    private val now = 1_800_000_000_000L
    private val base = Announcement(id = "a1", title = "Hello")

    private fun eligible(a: Announcement, version: Long = 10, dismissed: Set<String> = emptySet()) =
        AnnouncementEligibility.isEligible(a, now, version, dismissed)

    @Test
    fun `plain announcement is eligible`() = assertTrue(eligible(base))

    @Test
    fun `time window is start-inclusive and end-exclusive`() {
        assertFalse(eligible(base.copy(startAtMs = now + 1)))
        assertTrue(eligible(base.copy(startAtMs = now)))
        assertTrue(eligible(base.copy(endAtMs = now + 1)))
        assertFalse(eligible(base.copy(endAtMs = now)))
    }

    @Test
    fun `version range is inclusive`() {
        assertFalse(eligible(base.copy(minVersionCode = 11)))
        assertTrue(eligible(base.copy(minVersionCode = 10, maxVersionCode = 10)))
        assertFalse(eligible(base.copy(maxVersionCode = 9)))
    }

    @Test
    fun `dismissed ids are hidden unless the announcement is not dismissible`() {
        assertFalse(eligible(base, dismissed = setOf("a1")))
        assertTrue(eligible(base.copy(dismissible = false), dismissed = setOf("a1")))
    }

    @Test
    fun `pick returns the first eligible one`() {
        val list = listOf(base.copy(id = "old", endAtMs = now - 1), base.copy(id = "b"), base.copy(id = "c"))
        assertEquals("b", AnnouncementEligibility.pick(list, now, 10, emptySet())?.id)
        assertNull(AnnouncementEligibility.pick(list, now, 10, setOf("b", "c")))
    }

    // ---- JSON parsing ----

    @Test
    fun `parses a full object`() {
        val json =
            """
            {"id":"spring","title":"New levels","body":"Fresh puzzles","cta":"Play","route":"daily",
             "url":"https://example.com/x","style":"promo","start":"2026-11-01T00:00:00Z","end":"2026-11-15",
             "min_version":3,"max_version":9,"dismissible":false,"future_field":1}
            """.trimIndent()
        val a = AnnouncementParser.parse(json).single()
        assertEquals("spring", a.id)
        assertEquals("Play", a.ctaLabel)
        assertEquals("daily", a.route)
        assertEquals("https://example.com/x", a.url)
        assertEquals(AnnouncementStyle.PROMO, a.style)
        assertEquals(1_793_491_200_000L, a.startAtMs)
        assertEquals(1_794_700_800_000L, a.endAtMs)
        assertEquals(3L, a.minVersionCode)
        assertEquals(9L, a.maxVersionCode)
        assertFalse(a.dismissible)
        assertTrue(a.hasCta)
    }

    @Test
    fun `parses an array and skips invalid entries`() {
        val json = """[{"id":"","title":"x"},{"id":"ok","title":"Hi"},{"title":"no id"},42,{"id":"b","title":"B"}]"""
        assertEquals(listOf("ok", "b"), AnnouncementParser.parse(json).map { it.id })
    }

    @Test
    fun `blank, malformed and non-object input yields nothing`() {
        assertTrue(AnnouncementParser.parse("").isEmpty())
        assertTrue(AnnouncementParser.parse("   ").isEmpty())
        assertTrue(AnnouncementParser.parse("{not json").isEmpty())
        assertTrue(AnnouncementParser.parse("\"text\"").isEmpty())
    }

    @Test
    fun `unknown style falls back to info and unsafe urls are dropped`() {
        val a = AnnouncementParser.parse(
            """{"id":"a","title":"T","style":"neon","cta":"Go","url":"javascript:alert(1)"}"""
        ).single()
        assertEquals(AnnouncementStyle.INFO, a.style)
        assertNull(a.url)
        assertFalse(a.hasCta)
        assertNull(AnnouncementParser.parse("""{"id":"a","title":"T","url":"http://plain.test"}""").single().url)
    }

    @Test
    fun `bad or inverted dates reject the announcement`() {
        assertTrue(AnnouncementParser.parse("""{"id":"a","title":"T","start":"someday"}""").isEmpty())
        assertTrue(
            AnnouncementParser.parse("""{"id":"a","title":"T","start":"2026-11-02","end":"2026-11-01"}""").isEmpty()
        )
    }

    @Test
    fun `time formats`() {
        assertEquals(1_000L, AnnouncementParser.parseTime("1000"))
        assertEquals(1_793_491_200_000L, AnnouncementParser.parseTime("2026-11-01T05:30:00+05:30"))
        assertEquals(1_793_491_200_000L, AnnouncementParser.parseTime("2026-11-01"))
        assertNull(AnnouncementParser.parseTime("01/11/2026"))
    }

    @Test
    fun `long texts are clamped`() {
        val a = AnnouncementParser.parse(
            """{"id":"a","title":"${"t".repeat(200)}","body":"${"b".repeat(500)}"}"""
        ).single()
        assertEquals(AnnouncementParser.MAX_TITLE, a.title.length)
        assertEquals(AnnouncementParser.MAX_BODY, a.body.length)
    }
}
