package com.bhardwaj.passkey.banner

import app.cash.turbine.test
import com.bhardwaj.passkey.analytics.BaselineEventNames
import com.bhardwaj.passkey.testing.FakeClock
import com.bhardwaj.passkey.testing.FakeConfig
import com.bhardwaj.passkey.testing.MainDispatcherRule
import com.bhardwaj.passkey.testing.RecordingAnalyticsTracker
import com.bhardwaj.passkey.testing.testDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AnnouncementRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @get:Rule val main = MainDispatcherRule()

    private val json =
        """[{"id":"one","title":"First","cta":"Open","url":"https://example.com"},""" +
            """{"id":"two","title":"Second","route":"store","cta":"Go"}]"""
    private val config = FakeConfig(mapOf("announcement_json" to json))
    private val clock = FakeClock()

    private fun repository() = AnnouncementRepository(config.flow, testDataStore(tmp.root), clock, versionCode = 10)

    @Test
    fun `shows one at a time and dismissals persist`() = runTest {
        val store = testDataStore(tmp.root)
        val repo = AnnouncementRepository(config.flow, store, clock, versionCode = 10)
        assertEquals("one", repo.current.first()?.id)
        repo.dismiss("one")
        assertEquals("two", repo.current.first()?.id)
        // A new repository over the same store (next app launch) remembers the dismissal.
        assertEquals("two", AnnouncementRepository(config.flow, store, clock, 10).current.first()?.id)
        repo.dismiss("two")
        assertNull(repo.current.first())
    }

    @Test
    fun `config changes are picked up`() = runTest {
        val repo = repository()
        config.set(emptyMap())
        assertNull(repo.current.first())
        config.set(mapOf("announcement_json" to """{"id":"new","title":"New"}"""))
        assertEquals("new", repo.current.first()?.id)
    }

    @Test
    fun `view model logs shown once, routes CTAs and dismisses`() = runTest {
        val analytics = RecordingAnalyticsTracker()
        val vm = AnnouncementViewModel(repository(), analytics)
        vm.announcement.test {
            // StateFlow starts at null, then the first eligible item arrives.
            var item = awaitItem()
            if (item == null) item = awaitItem()
            assertEquals("one", item?.id)
            vm.effects.test {
                vm.onIntent(AnnouncementIntent.CtaClicked(item!!))
                assertEquals(AnnouncementEffect.OpenUrl("https://example.com"), awaitItem())
            }
            vm.onIntent(AnnouncementIntent.Dismissed(item!!))
            val next = awaitItem()
            assertEquals("two", next?.id)
            vm.effects.test {
                vm.onIntent(AnnouncementIntent.CtaClicked(next!!))
                assertEquals(AnnouncementEffect.Navigate("store"), awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
        val names = analytics.names
        assertEquals(2, names.count { it == BaselineEventNames.ANNOUNCEMENT_SHOWN })
        assertTrue(BaselineEventNames.ANNOUNCEMENT_CLICKED in names)
        assertTrue(BaselineEventNames.ANNOUNCEMENT_DISMISSED in names)
    }
}
